package com.blockstream.domain.swap

import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.swap.SwapAsset

data class SwapDirection(val from: SwapAsset, val to: SwapAsset) {
    companion object {
        val BitcoinToLiquid = SwapDirection(SwapAsset.Bitcoin, SwapAsset.Liquid)
        val LiquidToBitcoin = SwapDirection(SwapAsset.Liquid, SwapAsset.Bitcoin)
        val BitcoinToLightning = SwapDirection(SwapAsset.Bitcoin, SwapAsset.Lightning)
        val LightningToBitcoin = SwapDirection(SwapAsset.Lightning, SwapAsset.Bitcoin)

        val LiquidToLightning = SwapDirection(SwapAsset.Liquid, SwapAsset.Lightning)

        val LightningToLiquid = SwapDirection(SwapAsset.Lightning, SwapAsset.Liquid)

        val All: Set<SwapDirection> = setOf(
            BitcoinToLiquid,
            LiquidToBitcoin,
            BitcoinToLightning,
            LightningToBitcoin,
            LiquidToLightning,
            LightningToLiquid
        )
    }
}

fun Network.toSwapAsset(): SwapAsset = when {
    isLightning -> SwapAsset.Lightning
    isLiquid -> SwapAsset.Liquid
    else -> SwapAsset.Bitcoin
}

fun swapDirectionOf(from: Network, to: Network): SwapDirection =
    SwapDirection(from.toSwapAsset(), to.toSwapAsset())

fun swapDirectionOf(from: AccountAsset, to: AccountAsset): SwapDirection =
    swapDirectionOf(from.account.network, to.account.network)

data class SwapAvailability(val enabledDirections: Set<SwapDirection>) {
    fun isCreationEnabled(direction: SwapDirection): Boolean = direction in enabledDirections

    val hasEnabledCreationDirection: Boolean get() = enabledDirections.isNotEmpty()

    companion object {
        val Current = SwapAvailability(enabledDirections = emptySet())

        val Unrestricted = SwapAvailability(enabledDirections = SwapDirection.All)
    }
}

sealed interface SwapDirectionAvailability {
    data object Available : SwapDirectionAvailability

    data class Unavailable(val errorId: String) : SwapDirectionAvailability

    val isAvailable: Boolean get() = this is Available
}
