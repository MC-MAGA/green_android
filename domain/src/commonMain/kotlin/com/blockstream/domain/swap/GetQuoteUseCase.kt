package com.blockstream.domain.swap

import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapAsset
import com.blockstream.jade.Loggable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetQuoteUseCase {
    operator fun invoke(
        session: GdkSession, from: Flow<SwapAsset>, to: Flow<SwapAsset>, satoshi: Flow<Long>, quoteMode: Flow<QuoteMode>
    ): Flow<Quote?> {
        val swapInfo = combine(from.distinctUntilChanged(), to.distinctUntilChanged()) { _, _ ->
            tryCatch {
                session.lwkOrNull?.refreshSwapInfo()
            }
        }

        return combine(satoshi, quoteMode, swapInfo, from, to) { satoshi, quoteMode, _, from, to ->
            if (satoshi == 0L) {
                null
            } else {
                tryCatch { session.lwkOrNull?.quote(satoshi, quoteMode, from, to) }?.also {
                    logger.d { "Quote: $it" }
                }
            }
        }
    }

    companion object : Loggable()
}

fun AccountAsset.toSwapAsset() = when {
    account.isLightning -> SwapAsset.Lightning
    account.isLiquid -> SwapAsset.Liquid
    account.isBitcoin -> SwapAsset.Bitcoin
    else -> throw Exception("Invalid account type)")
}

// Swappable source/destination assets: policy assets (BTC, L-BTC), and Lightning only when it is
// actually enabled and connected (not the onboarding shortcut, whose node/SDK isn't available). Note:
// lnbtc is the Lightning network's policy asset, so the Lightning case must be gated on hasLightning
// here rather than falling through to the isPolicyAsset() check.
fun AccountAsset.isSwappableAsset(session: GdkSession): Boolean =
    if (account.isLightning) session.hasLightning && session.lightningSdkOrNull != null else asset.isPolicyAsset(session)

// True when the given asset has more than one swappable account, i.e. the account is a real choice.
// Used to decide whether to show the account (swap screen + review); a single account stays hidden.
fun hasMultipleSwapAccounts(session: GdkSession?, accountAsset: AccountAsset): Boolean {
    session ?: return false
    return session.accountAsset.value.count {
        it.isSwappableAsset(session) && it.account.network.isSameNetwork(accountAsset.account.network)
    } > 1
}

/**
 * Supported swap pairs: Bitcoin <-> Liquid Bitcoin and Bitcoin <-> Lightning Bitcoin.
 * Liquid Bitcoin <-> Lightning Bitcoin and same-asset swaps are not supported.
 */
fun isSwapPairSupported(from: AccountAsset, to: AccountAsset): Boolean =
    isSwapPairSupported(from.account.network, to.account.network)

fun isSwapPairSupported(from: Network, to: Network): Boolean {
    if (from.isSameNetwork(to)) return false
    val liquidLightning = (from.isLiquid && to.isLightning) || (from.isLightning && to.isLiquid)
    return !liquidLightning
}