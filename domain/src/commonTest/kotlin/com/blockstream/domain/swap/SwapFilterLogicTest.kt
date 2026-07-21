package com.blockstream.domain.swap

import com.blockstream.data.gdk.data.Network
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwapFilterLogicTest {

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

    @Test
    fun btcToLiquid_allowed() {
        assertTrue(isSwapPairSupported(btcNetwork, liquidNetwork))
    }

    @Test
    fun liquidToBtc_allowed() {
        assertTrue(isSwapPairSupported(liquidNetwork, btcNetwork))
    }

    @Test
    fun sameNetwork_blocked() {
        assertFalse(isSwapPairSupported(btcNetwork, btcNetwork))
        assertFalse(isSwapPairSupported(liquidNetwork, liquidNetwork))
        assertFalse(isSwapPairSupported(lightningNetwork, lightningNetwork))
    }

    @Test
    fun lightningToBtc_allowed() {
        assertTrue(isSwapPairSupported(lightningNetwork, btcNetwork))
    }

    @Test
    fun btcToLightning_allowed() {
        assertTrue(isSwapPairSupported(btcNetwork, lightningNetwork))
    }

    @Test
    fun lightningToLiquid_blocked() {
        assertFalse(isSwapPairSupported(lightningNetwork, liquidNetwork))
    }

    @Test
    fun liquidToLightning_blocked() {
        assertFalse(isSwapPairSupported(liquidNetwork, lightningNetwork))
    }
}
