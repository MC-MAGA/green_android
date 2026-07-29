@file:OptIn(ExperimentalCoroutinesApi::class)

package com.blockstream.domain.swap

import com.blockstream.data.CountlyBase
import com.blockstream.data.data.Denomination
import com.blockstream.data.extensions.isBlank
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lightning.maxSendableSatoshi
import com.blockstream.data.lightning.maxReceivableSatoshi
import com.blockstream.data.lightning.totalInboundLiquiditySatoshi
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.QuoteValidity
import com.blockstream.data.swap.SwapAmount
import com.blockstream.data.swap.SwapErrorSide
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.toAmountLook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Provides a reactive flow of [SwapAmount] based on user input and selected swap pair.
 *
 * This use case combines source/destination balances, the user-entered amount, and current swap limits
 * to validate the transaction. It calculates the expected receive amounts (in crypto and fiat) and
 * provides descriptive error messages if the input violates balance or provider limits.
 */
class GetSwapAmountUseCase(
    private val getQuoteUseCase: GetQuoteUseCase,
    private val countly: CountlyBase
) {

    /**
     * Returns a [Flow] that emits updated [SwapAmount] whenever any input changes.
     *
     * The validation logic includes:
     * 1. Parsing user input according to the selected denomination.
     * 2. Checking against the source account balance (Insufficient Funds).
     * 3. Fetching and validating against minimum and maximum swap limits for the pair.
     * 4. Calculating the estimated amount to be received after provider fees.
     *
     * @param session the current [GdkSession]
     * @param from [Flow] of the source account balance
     * @param to [Flow] of the destination account balance
     * @param amount [Flow] of the user-entered amount string
     * @param denomination [Flow] of the selected [Denomination]
     * @return a [Flow] containing the calculated [SwapAmount] with optional error messages
     */
    operator fun invoke(
        session: GdkSession,
        from: Flow<AccountAsset>,
        to: Flow<AccountAsset>,
        amountFrom: Flow<String>,
        amountTo: Flow<String>,
        quoteMode: Flow<QuoteMode>,
        denomination: Flow<Denomination>
    ): Flow<SwapAmount> {

        val account = quoteMode.flatMapLatest {
            if (it.isSend) from else to
        }

        val amount = quoteMode.flatMapLatest {
            if (it.isSend) amountFrom else amountTo
        }

        val balance = combine(
            account.distinctUntilChanged(),
            amount.distinctUntilChanged(),
            denomination.distinctUntilChanged()
        ) { account, amount, denomination ->
            amount.takeIf { it.isNotBlank() }?.let {
                UserInput.parseUserInputSafe(
                    session = session, input = it, assetId = account.asset.assetId, denomination = denomination
                ).getBalance()
            }
        }

        val quote = getQuoteUseCase(
            session = session,
            from = from.map { it.toSwapAsset() }.distinctUntilChanged(),
            to = to.map { it.toSwapAsset() }.distinctUntilChanged(),
            satoshi = balance.map {
                it?.satoshi ?: 0
            }.distinctUntilChanged(),
            quoteMode = quoteMode.distinctUntilChanged()
        )

        return combine(
            from.distinctUntilChanged(),
            to.distinctUntilChanged(),
            amountFrom,
            quote,
            denomination
        ) { from, to, amountFrom, quote, denomination ->

            val balance = UserInput.parseUserInputSafe(
                session = session, input = amountFrom, assetId = from.asset.assetId, denomination = denomination
            ).getBalance()

            val satoshi = balance?.satoshi ?: 0

            val receiveSatoshi = quote?.receiveAmount

            val swapAmount = SwapAmount(
                quote = quote,
                amountFrom = quote?.sendAmount.toAmountLook(
                    session = session, assetId = from.assetId, denomination = denomination, withGrouping = false, withUnit = false
                ) ?: "",
                amountFromExchange = quote?.sendAmount.toAmountLook(
                    session = session,
                    assetId = from.assetId,
                    denomination = Denomination.exchange(session, denomination),
                )?.let { if (denomination.isFiat) it else "≈ $it" },
                amountTo = receiveSatoshi.toAmountLook(
                    session = session, assetId = to.assetId, denomination = denomination, withGrouping = false, withUnit = false
                ) ?: "",
                amountToExchange = receiveSatoshi.toAmountLook(
                    session = session,
                    assetId = to.assetId,
                    denomination = Denomination.exchange(session, denomination),
                )?.let { if (denomination.isFiat) it else "≈ $it" }
            )

            // Boltz enforces submarine limits on the Lightning invoice (the receive side), so for
            // Lightning destinations validate the quoted receive amount; other directions validate
            // the entered send amount.
            val limitAmount = if (to.account.isLightning) quote?.receiveAmount ?: satoshi else satoshi
            val isValid = quote?.isValid(limitAmount) ?: if (satoshi > 0) QuoteValidity.MAX else QuoteValidity.MIN

            val nodeInfo = session.lightningSdkOrNull?.nodeInfoStateFlow?.value

            // Lightning-specific limits, enforced in addition to the Boltz min/max.
            val lightningError: String? = when {
                amountFrom.isBlank() -> null
                from.account.isLightning -> {
                    // Lightning -> Bitcoin (like Lightning Send): cannot send more than the fee-adjusted
                    // spendable amount (reserves the routing-fee budget the node demands on top).
                    val maxSendable = nodeInfo?.maxSendableSatoshi() ?: 0L
                    when {
                        maxSendable == 0L -> "id_lightning_balance_too_low_to_send"
                        satoshi > maxSendable -> maxSendable.toAmountLook(
                            session = session, assetId = from.account.network.policyAsset,
                            denomination = denomination.notFiat(), withUnit = true
                        )?.let { "id_amount_is_above_the_maximum_payment_limit_of_s|$it" }
                        else -> null
                    }
                }
                to.account.isLightning -> {
                    // Bitcoin -> Lightning (like Lightning Receive): the received amount (the invoice,
                    // created for the quoted receive amount) must fit the Countly hard limits and the
                    // node's inbound liquidity / channel limits.
                    val receiveSats = quote?.receiveAmount ?: 0
                    val lnMin = countly.getLnMinSatoshis()
                    val lnMax = countly.getLnMaxSatoshis()
                    val maxReceivable = nodeInfo?.maxReceivableSatoshi() ?: 0L
                    val hasChannels = (nodeInfo?.totalInboundLiquiditySatoshi() ?: 0L) > 0L
                    val inbound = nodeInfo?.totalInboundLiquiditySatoshi()?.takeIf { it > 0 } ?: Long.MAX_VALUE
                    when {
                        receiveSats <= 0 -> null
                        // Countly hard maximum, always enforced.
                        receiveSats > lnMax -> lnMax.toAmountLook(
                            session = session, assetId = to.account.network.policyAsset,
                            denomination = denomination.notFiat(), withUnit = true
                        )?.let { "id_amount_too_high_s|$it" }
                        // No channel yet: require the Countly minimum to open one.
                        !hasChannels && receiveSats < lnMin -> lnMin.toAmountLook(
                            session = session, assetId = to.account.network.policyAsset,
                            denomination = denomination.notFiat(), withUnit = true
                        )?.let { "id_amount_must_be_at_least_s|$it" }
                        receiveSats > inbound -> "id_the_amount_is_above_your_inbound|${
                            inbound.toAmountLook(session = session, withUnit = true, denomination = denomination.notFiat()) ?: ""
                        }|${
                            inbound.toAmountLook(session = session, withUnit = true, denomination = Denomination.fiat(session)) ?: ""
                        }"
                        maxReceivable > 0 && receiveSats > maxReceivable -> maxReceivable.toAmountLook(
                            session = session, assetId = to.account.network.policyAsset,
                            denomination = denomination.notFiat(), withUnit = true
                        )?.let { "id_amount_too_high_s|$it" }
                        else -> null
                    }
                }
                else -> null
            }

            val error = when {
                !isSwapPairSupported(from, to) -> "id_swap_pair_is_not_supported_yet"
                satoshi > from.balance(session) -> "id_insufficient_funds"
                isValid == QuoteValidity.MIN -> {
                    if (to.account.isLightning && quote != null) {
                        // The floor applies to the received amount; the red To highlight marks the
                        // value that must be raised.
                        val minimum = quote.minimal.toAmountLook(
                            session = session, assetId = to.account.network.policyAsset,
                            denomination = denomination.notFiat(), withUnit = true
                        )
                        val minimumFiat = quote.minimal.toAmountLook(
                            session = session, assetId = to.account.network.policyAsset,
                            denomination = Denomination.fiat(session), withUnit = true
                        )
                        if (minimum != null && minimumFiat != null) {
                            "id_minimum_receive_is_s|$minimum|$minimumFiat"
                        } else null
                    } else {
                        quote?.minimal.toAmountLook(
                            session = session,
                            assetId = from.account.network.policyAsset,
                            denomination = denomination.notFiat(),
                            withUnit = true
                        )?.let {
                            "id_amount_too_low_s|$it"
                        }
                    }
                }

                isValid == QuoteValidity.MAX -> {
                    quote?.maximal.toAmountLook(
                        session = session,
                        assetId = from.account.network.policyAsset,
                        denomination = denomination.notFiat(),
                        withUnit = true
                    )?.let {
                        "id_amount_too_high_s|$it"
                    }
                }

                lightningError != null -> lightningError

                else -> null
            }

            // Which amount the error refers to, so the UI can highlight the offending value: pair
            // errors concern neither side; funds concern the source; Boltz/Lightning limits bind the
            // invoice, which is the To side for Lightning destinations and the From side otherwise.
            val errorSide = when {
                error == null || error == "id_swap_pair_is_not_supported_yet" -> SwapErrorSide.NONE
                error == "id_insufficient_funds" -> SwapErrorSide.FROM
                to.account.isLightning -> SwapErrorSide.TO
                else -> SwapErrorSide.FROM
            }

            swapAmount.copy(error = error, errorSide = errorSide, isValid = quote != null && isValid == QuoteValidity.VALID && error == null)
        }
    }
}
