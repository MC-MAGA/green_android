package com.blockstream.domain.swap

import com.blockstream.data.gdk.data.Network
import com.blockstream.data.swap.SwapAsset
import com.blockstream.domain.swap.IsSwapDirectionAvailableUseCase.Companion.ERROR_PAY_LIGHTNING_WITH_LIQUID
import com.blockstream.domain.swap.IsSwapDirectionAvailableUseCase.Companion.ERROR_RECEIVE_LIGHTNING_AS_LIQUID
import com.blockstream.domain.swap.IsSwapDirectionAvailableUseCase.Companion.ERROR_SWAPS_UNAVAILABLE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwapDirectionAvailabilityTest {
    private val btcNetwork = Network(
        network = Network.ElectrumMainnet,
        name = "Bitcoin",
        isMainnet = true,
        isLiquid = false,
        isDevelopment = false,
        policyAsset = "btc"
    )

    private val liquidNetwork = Network(
        network = Network.ElectrumLiquid,
        name = "Liquid",
        isMainnet = true,
        isLiquid = true,
        isDevelopment = false,
        policyAsset = "6f0279e9ed041c3d710a9f57d0c02928416460c4b722ae3457a11eec381c526d"
    )

    private val lightningNetwork = Network(
        network = Network.LightningMainnet,
        name = "Lightning",
        isMainnet = true,
        isLiquid = false,
        isDevelopment = false,
        isLightning = true
    )

    private val shipped = IsSwapDirectionAvailableUseCase(SwapAvailability.Current)
    private val unrestricted = IsSwapDirectionAvailableUseCase(SwapAvailability.Unrestricted)

    @Test
    fun networkToSwapAsset_isTotal() {
        assertEquals(SwapAsset.Bitcoin, btcNetwork.toSwapAsset())
        assertEquals(SwapAsset.Liquid, liquidNetwork.toSwapAsset())
        assertEquals(SwapAsset.Lightning, lightningNetwork.toSwapAsset())
    }

    @Test
    fun shippedAvailability_isEmpty() {
        assertTrue(SwapAvailability.Current.enabledDirections.isEmpty())
        assertFalse(SwapAvailability.Current.hasEnabledCreationDirection)
        assertFalse(shipped.hasAnyEnabledDirection())
    }

    @Test
    fun shippedAvailability_blocksEveryDirection() {
        SwapDirection.All.forEach { direction ->
            assertFalse(shipped.isEnabled(direction), "expected $direction to be blocked")
            assertFalse(shipped(direction).isAvailable, "expected $direction to be unavailable")
        }
    }

    @Test
    fun liquidToLightning_reportsPayLightningError() {
        val result = shipped(liquidNetwork, lightningNetwork)
        assertEquals(
            SwapDirectionAvailability.Unavailable(ERROR_PAY_LIGHTNING_WITH_LIQUID),
            result
        )
        val error = assertFailsWith<Exception> {
            shipped.requireAvailable(SwapDirection.LiquidToLightning)
        }
        assertEquals(ERROR_PAY_LIGHTNING_WITH_LIQUID, error.message)
    }

    @Test
    fun lightningToLiquid_reportsReceiveLightningError() {
        val result = shipped(lightningNetwork, liquidNetwork)
        assertEquals(
            SwapDirectionAvailability.Unavailable(ERROR_RECEIVE_LIGHTNING_AS_LIQUID),
            result
        )
    }

    @Test
    fun chainDirections_reportGenericSwapsUnavailableError() {
        listOf(
            SwapDirection.BitcoinToLiquid,
            SwapDirection.LiquidToBitcoin,
            SwapDirection.BitcoinToLightning,
            SwapDirection.LightningToBitcoin
        ).forEach { direction ->
            assertEquals(
                SwapDirectionAvailability.Unavailable(ERROR_SWAPS_UNAVAILABLE),
                shipped(direction),
                "unexpected error id for $direction"
            )
        }
    }

    @Test
    fun directionsAreReEnabledIndependently() {
        val onlyLiquidToLightning = IsSwapDirectionAvailableUseCase(
            SwapAvailability(setOf(SwapDirection.LiquidToLightning))
        )

        assertTrue(onlyLiquidToLightning.isEnabled(SwapDirection.LiquidToLightning))
        assertTrue(onlyLiquidToLightning(liquidNetwork, lightningNetwork).isAvailable)
        assertTrue(onlyLiquidToLightning.hasAnyEnabledDirection())

        assertFalse(onlyLiquidToLightning.isEnabled(SwapDirection.LightningToLiquid))
        assertFalse(onlyLiquidToLightning.isEnabled(SwapDirection.BitcoinToLiquid))
    }

    @Test
    fun unrestrictedAvailability_allowsEveryDirection() {
        SwapDirection.All.forEach { direction ->
            assertTrue(unrestricted.isEnabled(direction), "expected $direction to be enabled")
            assertTrue(unrestricted(direction).isAvailable)
        }
        unrestricted.requireAvailable(SwapDirection.LiquidToLightning)
    }

    @Test
    fun directionIsOrdered() {
        assertTrue(SwapDirection.LiquidToLightning != SwapDirection.LightningToLiquid)

        val oneWay = IsSwapDirectionAvailableUseCase(
            SwapAvailability(setOf(SwapDirection.BitcoinToLiquid))
        )
        assertTrue(oneWay.isEnabled(SwapDirection.BitcoinToLiquid))
        assertFalse(oneWay.isEnabled(SwapDirection.LiquidToBitcoin))
    }

    @Test
    fun errorIds_areResolvableStringIds() {
        listOf(
            ERROR_PAY_LIGHTNING_WITH_LIQUID,
            ERROR_RECEIVE_LIGHTNING_AS_LIQUID,
            ERROR_SWAPS_UNAVAILABLE
        ).forEach { id ->
            assertTrue(id.startsWith("id_"), "$id must start with id_")
            assertFalse(id.contains("|"), "$id must not contain the format-arg separator")
        }
    }
}
