@file:OptIn(FlowPreview::class)

package com.blockstream.domain.transaction

import com.blockstream.data.backend.NetworkEvent
import com.blockstream.data.data.DataState
import com.blockstream.data.data.TransactionList
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.gdk.params.TransactionParams.Companion.TRANSACTIONS_PER_PAGE
import com.blockstream.domain.base.DataStateObservableUseCase
import com.blockstream.utils.Loggable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

/**
 * Provides a paginated, in-memory-cached stream of an [Account]'s transactions.
 *
 * Each call to [invoke] runs in one of three modes, selected by [Params]:
 * - **reset**: load the first page from scratch, surfacing [DataState.Loading] first.
 * - **load-more**: fetch the next page and append it to the already-loaded transactions.
 * - **refresh** (neither flag set): re-load the currently loaded depth in place, without a
 *   loading state — used to reflect new activity while keeping the user's scroll position.
 *
 * While [observe] is collected, the stream additionally re-fetches on relevant
 * [NetworkEvent]s for the account's network (a new block, or a transaction touching this
 * account). [observe] is shared, so any number of collectors share a single fetch pipeline
 * and a single network-event subscription rather than one per collector.
 */
class GetAccountTransactionsUseCase(private val session: GdkSession, accountAsset: AccountAsset) :
    DataStateObservableUseCase<GetAccountTransactionsUseCase.Params, TransactionList>() {

    private val account = accountAsset.account

    override suspend fun doAsyncWork(params: Params) {
        val currentData = getCurrent().data() ?: TransactionList()

        var offset = 0
        val txSize = currentData.transactions.size

        if (params.isReset) {
            set(DataState.Loading)
        } else if (params.isLoadMore) {
            offset = txSize
        }

        // reset/load-more fetch a single page; a plain refresh re-reads everything already
        // loaded (plus one more page) from the start so the visible list updates in place.
        val limit = if (params.isReset || params.isLoadMore) TRANSACTIONS_PER_PAGE else (txSize + TRANSACTIONS_PER_PAGE)

        val fetchedTransactions = session.accountBackend(account).getTransactions(
            params = TransactionParams(offset = offset, limit = limit)
        ).transactions

        // A result that fills the requested page implies more may exist. Lightning has no
        // on-chain paging, so it never reports more.
        val hasMore = if (account.isLightning) false else fetchedTransactions.size == limit

        val dataState = DataState.Success(
            data = TransactionList(
                transactions = if (params.isLoadMore) {
                    currentData.transactions + fetchedTransactions
                } else {
                    fetchedTransactions
                },
                hasMore = hasMore
            )
        )

        set(dataState)
    }

    override fun createObservable(params: Params): Flow<DataState<TransactionList>> {
        // Re-fetch on a new block (network-wide), or on a transaction that touches this
        // account. Emits nothing itself; the refresh writes to the cache that get() exposes.
        // Lives only while observe() is collected, so it stops when no screen is watching.
        val refreshOnNetworkEvent = flow<DataState<TransactionList>> {
            session.networkBackend(account.network).networkEventsFlow
                // The event flow does not replay, so a (re)subscribe alone does not refresh and
                // the first live event after subscribing is not dropped.
                .filter { event ->
                    when (event) {
                        is NetworkEvent.Block -> true
                        // No pointers means the event is not account-specific (network-wide),
                        // otherwise refresh only when this account is among the affected ones.
                        is NetworkEvent.Transaction -> event.accountPointers.isEmpty() || account.pointer in event.accountPointers
                        is NetworkEvent.Synced -> event.accountPointer == account.pointer
                    }
                }
                .debounce(750)
                .collect {
                    doWork(params.copy(isReset = false, isLoadMore = false))
                }
        }

        return merge(get(), refreshOnNetworkEvent)
    }

    /**
     * @param isReset load the first page from scratch, emitting [DataState.Loading] first.
     * @param isLoadMore append the next page to the already-loaded transactions.
     */
    data class Params(
        val isReset: Boolean = false,
        val isLoadMore: Boolean = false,
    )

    companion object : Loggable()
}
