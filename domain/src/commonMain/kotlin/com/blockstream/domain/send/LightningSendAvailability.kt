package com.blockstream.domain.send

import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.domain.swap.IsSwapDirectionAvailableUseCase

enum class LightningDestination { Bolt11WithAmount, AmountlessBolt11, Bolt12, LnUrl, Unknown }

fun PaymentInstruction?.toLightningDestination(): LightningDestination = when (this) {
    is PaymentInstruction.Bolt11 ->
        if (amountSats != null) LightningDestination.Bolt11WithAmount else LightningDestination.AmountlessBolt11

    is PaymentInstruction.Bolt12 -> LightningDestination.Bolt12
    is PaymentInstruction.LnUrl -> LightningDestination.LnUrl
    null -> LightningDestination.Unknown
}

object LightningSendAvailability {
    const val ERROR_AMOUNTLESS_BOLT11 = "id_no_amount_less_invoices_supported"
    const val ERROR_INVALID_ADDRESS = "id_invalid_address"
    const val ERROR_BOLT12_ONLY_VIA_LBTC = "id_bolt12_payment_is_only_available_via_lbtc"
    const val ERROR_INSUFFICIENT_FUNDS = "id_insufficient_funds"

    private val SwappableDestinations = setOf(
        LightningDestination.Bolt11WithAmount,
        LightningDestination.Bolt12,
        LightningDestination.LnUrl,
    )

    fun onlyRouteWasLiquidSwap(
        isLightningDestination: Boolean,
        destination: LightningDestination,
        hasNativeLightning: Boolean,
        liquidCouldHavePaid: Boolean,
        isDirectionAvailable: Boolean,
    ): Boolean = isLightningDestination &&
            !hasNativeLightning &&
            liquidCouldHavePaid &&
            !isDirectionAvailable &&
            destination in SwappableDestinations

    fun noFundableAccountErrorId(
        destination: LightningDestination,
        isDirectionAvailable: Boolean,
        hasEligibleAccount: Boolean,
        liquidCouldHavePaid: Boolean,
        liquidRailExists: Boolean,
    ): String? = when {
        destination == LightningDestination.AmountlessBolt11 && !hasEligibleAccount -> ERROR_AMOUNTLESS_BOLT11
        destination == LightningDestination.Unknown && !hasEligibleAccount -> ERROR_INVALID_ADDRESS
        !isDirectionAvailable && liquidCouldHavePaid -> IsSwapDirectionAvailableUseCase.ERROR_PAY_LIGHTNING_WITH_LIQUID
        destination == LightningDestination.Bolt12 && !hasEligibleAccount && !liquidRailExists ->
            ERROR_BOLT12_ONLY_VIA_LBTC
        else -> null
    }

    fun bolt12OverLightningSource(
        destination: LightningDestination,
        assetIsLightning: Boolean,
        accountIsLightning: Boolean,
    ): Boolean = destination == LightningDestination.Bolt12 && (assetIsLightning || accountIsLightning)

    fun explicitLiquidSourceBlocked(
        isLightningDestination: Boolean,
        accountIsLiquid: Boolean,
        isDirectionAvailable: Boolean,
    ): Boolean = isLightningDestination && accountIsLiquid && !isDirectionAvailable
}
