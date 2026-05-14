package com.blockstream.domain.transaction

import com.blockstream.data.backend.NetworkEvent
import com.blockstream.data.comparators.ComparatorTransactions
import com.blockstream.data.data.DataState
import com.blockstream.data.data.TransactionList
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GdkSession.Companion.DEFAULT_TRANSACTIONS_LIMIT
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.domain.base.DataStateObservableUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Aggregates every account's transactions into a single wallet-level, paginated list.
 *
 * Each account is queried for up to [currentTransactionLimit] transactions; the results are
 * merged, sorted with [ComparatorTransactions] (pending first, then newest first) and truncated
 * back to the limit, so the page size applies to the whole wallet rather than per account.
 * A reset returns to the first page; "load more" grows the limit by [DEFAULT_TRANSACTIONS_LIMIT].
 *
 * While [observe] is collected the list is refreshed on every network event (a new block, or a
 * transaction touching the wallet) and whenever the set of accounts changes. The stream is shared,
 * so concurrent collectors drive a single refresh rather than one each.
 */
class GetWalletTransactionsUseCase(private val session: GdkSession) :
    DataStateObservableUseCase<GetWalletTransactionsUseCase.Params, TransactionList>() {

    var currentTransactionLimit = DEFAULT_TRANSACTIONS_LIMIT

    override suspend fun doAsyncWork(params: Params) {
        if (params.isReset) {
            set(DataState.Loading)
            currentTransactionLimit = DEFAULT_TRANSACTIONS_LIMIT
        } else if (params.isLoadMore) {
            currentTransactionLimit += DEFAULT_TRANSACTIONS_LIMIT
        }

        // Pull a full page from each account, then merge into one wallet-wide page: flatten,
        // order pending-first/newest-first, and truncate back to the limit. Truncating the merged
        // list (rather than trusting per-account counts) keeps the page size stable regardless of
        // how the transactions are distributed across accounts.
        val accountTransactions = session.accounts.value.associateWith { account ->
            session.accountBackend(account).getTransactions(TransactionParams(limit = currentTransactionLimit))
        }

        val walletTransactions = accountTransactions.values.flatMap {
            it.transactions
        }.sortedWith(ComparatorTransactions)

        val coercedWalletTransactions = walletTransactions.subList(0, walletTransactions.size.coerceAtMost(currentTransactionLimit))

        val hasMore = accountTransactions.any {
            !it.key.isLightning && it.value.transactions.size == currentTransactionLimit
        }

        val dataState = DataState.Success(
            data = TransactionList(
                transactions = coercedWalletTransactions,
                hasMore = hasMore
            )
        )

        set(dataState)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun createObservable(params: Params): Flow<DataState<TransactionList>> {
        // Refresh on a network event (a new block, or a transaction touching the wallet).
        // Merge every backend's live events. The event flow does not replay, so a (re)subscribe
        // alone does not refresh and the first live event after subscribing is not dropped.
        val networkEventTriggers = session.networkBackendsStateFlow
            .flatMapLatest { backends ->
                backends.values.map { it.networkEventsFlow }.merge()
            }
            .filter { event ->
                when (event) {
                    is NetworkEvent.Block -> true
                    is NetworkEvent.Transaction -> true
                    is NetworkEvent.Synced -> true
                }
            }
            .map { }

        // Refresh when the set of accounts changes (created, archived, hidden/unhidden) — a
        // lifecycle change rather than a network event, which would otherwise be missed.
        val accountTriggers = session.accounts
            .drop(1)
            .map { }

        // Emits nothing itself; the refresh writes to the cache that get() exposes, and lives only
        // while observe() is collected. Debounced once over the merged triggers so a burst from
        // either source — or both arriving together — collapses into a single doWork.
        val refresh = flow<DataState<TransactionList>> {
            merge(networkEventTriggers, accountTriggers)
                .debounce(750)
                .collect {
                    doWork(params.copy(isReset = false, isLoadMore = false))
                }
        }

        return merge(get(), refresh)
    }

    data class Params(
        val isReset: Boolean = false,
        val isLoadMore: Boolean = false,
    )
}
