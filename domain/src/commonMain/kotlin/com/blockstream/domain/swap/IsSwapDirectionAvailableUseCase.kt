package com.blockstream.domain.swap

import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.swap.SwapAsset

class IsSwapDirectionAvailableUseCase(private val availability: SwapAvailability) {
    operator fun invoke(direction: SwapDirection): SwapDirectionAvailability =
        if (availability.isCreationEnabled(direction)) {
            SwapDirectionAvailability.Available
        } else {
            SwapDirectionAvailability.Unavailable(unavailableErrorId(direction))
        }

    operator fun invoke(from: SwapAsset, to: SwapAsset): SwapDirectionAvailability =
        invoke(SwapDirection(from, to))

    operator fun invoke(from: Network, to: Network): SwapDirectionAvailability =
        invoke(swapDirectionOf(from, to))

    operator fun invoke(from: AccountAsset, to: AccountAsset): SwapDirectionAvailability =
        invoke(swapDirectionOf(from, to))

    fun isEnabled(direction: SwapDirection): Boolean = availability.isCreationEnabled(direction)

    fun hasAnyEnabledDirection(): Boolean = availability.hasEnabledCreationDirection

    fun requireAvailable(direction: SwapDirection) {
        val result = invoke(direction)
        if (result is SwapDirectionAvailability.Unavailable) {
            throw Exception(result.errorId)
        }
    }

    fun requireAvailable(from: Network, to: Network) = requireAvailable(swapDirectionOf(from, to))

    fun requireAvailable(from: AccountAsset, to: AccountAsset) =
        requireAvailable(swapDirectionOf(from, to))

    private fun unavailableErrorId(direction: SwapDirection): String = when (direction) {
        SwapDirection.LiquidToLightning -> ERROR_PAY_LIGHTNING_WITH_LIQUID
        SwapDirection.LightningToLiquid -> ERROR_RECEIVE_LIGHTNING_AS_LIQUID
        else -> ERROR_SWAPS_UNAVAILABLE
    }

    companion object {
        const val ERROR_PAY_LIGHTNING_WITH_LIQUID = "id_paying_lightning_invoices_with_liquid"
        const val ERROR_RECEIVE_LIGHTNING_AS_LIQUID = "id_receiving_lightning_payments_as_liquid_bitcoin"
        const val ERROR_SWAPS_UNAVAILABLE = "id_swaps_are_temporarily_unavailable"
    }
}
