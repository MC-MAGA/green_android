package com.blockstream.domain.send

import com.blockstream.data.lwk.Bolt12AmountMode
import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.domain.swap.IsSwapDirectionAvailableUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightningSendAvailabilityTest {
    private val bolt11WithAmount = PaymentInstruction.Bolt11(invoice = "lnbc1", amountSats = 10_000L)
    private val bolt11Amountless = PaymentInstruction.Bolt11(invoice = "lnbc1", amountSats = null)
    private val bolt12 = PaymentInstruction.Bolt12(offer = "lno1", amountMode = Bolt12AmountMode.AMOUNTLESS)
    private val lnUrl = PaymentInstruction.LnUrl(
        raw = "lnurl1",
        minSats = 1L,
        maxSats = 100L,
        description = null,
        metadata = ""
    )

    @Test
    fun bolt11WithAmount_mapsToBolt11WithAmount() =
        assertEquals(LightningDestination.Bolt11WithAmount, bolt11WithAmount.toLightningDestination())

    @Test
    fun bolt11WithoutAmount_mapsToAmountlessBolt11() =
        assertEquals(LightningDestination.AmountlessBolt11, bolt11Amountless.toLightningDestination())

    @Test
    fun bolt12_mapsToBolt12() =
        assertEquals(LightningDestination.Bolt12, bolt12.toLightningDestination())

    @Test
    fun lnUrl_mapsToLnUrl() =
        assertEquals(LightningDestination.LnUrl, lnUrl.toLightningDestination())

    @Test
    fun nullInstruction_mapsToUnknown() =
        assertEquals(LightningDestination.Unknown, (null as PaymentInstruction?).toLightningDestination())

    private fun onlyRoute(
        destination: LightningDestination = LightningDestination.Bolt11WithAmount,
        isLightningDestination: Boolean = true,
        hasNativeLightning: Boolean = false,
        liquidCouldHavePaid: Boolean = true,
        isDirectionAvailable: Boolean = false,
    ) = LightningSendAvailability.onlyRouteWasLiquidSwap(
        isLightningDestination = isLightningDestination,
        destination = destination,
        hasNativeLightning = hasNativeLightning,
        liquidCouldHavePaid = liquidCouldHavePaid,
        isDirectionAvailable = isDirectionAvailable,
    )

    @Test
    fun noNativeLightning_withFundedLiquid_swappableDestination_blocks() {
        assertTrue(onlyRoute(destination = LightningDestination.Bolt11WithAmount))
        assertTrue(onlyRoute(destination = LightningDestination.Bolt12))
        assertTrue(onlyRoute(destination = LightningDestination.LnUrl))
    }

    @Test
    fun noNativeLightning_withUnfundedLiquid_doesNotBlock() =
        assertFalse(onlyRoute(liquidCouldHavePaid = false))

    @Test
    fun noNativeLightning_amountlessBolt11_doesNotBlock() =
        assertFalse(onlyRoute(destination = LightningDestination.AmountlessBolt11))

    @Test
    fun noNativeLightning_unknownDestination_doesNotBlock() =
        assertFalse(onlyRoute(destination = LightningDestination.Unknown))

    @Test
    fun withNativeLightning_doesNotBlock() =
        assertFalse(onlyRoute(hasNativeLightning = true))

    @Test
    fun directionAvailable_doesNotBlock() =
        assertFalse(onlyRoute(isDirectionAvailable = true))

    @Test
    fun nonLightningDestination_doesNotBlockOnlyRoute() =
        assertFalse(onlyRoute(isLightningDestination = false))

    private fun errorId(
        destination: LightningDestination,
        isDirectionAvailable: Boolean = false,
        hasEligibleAccount: Boolean = false,
        liquidCouldHavePaid: Boolean = false,
        liquidRailExists: Boolean = liquidCouldHavePaid,
    ) = LightningSendAvailability.noFundableAccountErrorId(
        destination = destination,
        isDirectionAvailable = isDirectionAvailable,
        hasEligibleAccount = hasEligibleAccount,
        liquidCouldHavePaid = liquidCouldHavePaid,
        liquidRailExists = liquidRailExists,
    )

    @Test
    fun amountlessBolt11_beatsOutage() = assertEquals(
        LightningSendAvailability.ERROR_AMOUNTLESS_BOLT11,
        errorId(LightningDestination.AmountlessBolt11, liquidCouldHavePaid = true)
    )

    @Test
    fun unknownDestination_beatsOutage() = assertEquals(
        LightningSendAvailability.ERROR_INVALID_ADDRESS,
        errorId(LightningDestination.Unknown, liquidCouldHavePaid = true)
    )

    @Test
    fun outage_beatsInsufficientFunds_whenLiquidCouldHavePaid() = assertEquals(
        IsSwapDirectionAvailableUseCase.ERROR_PAY_LIGHTNING_WITH_LIQUID,
        errorId(LightningDestination.Bolt11WithAmount, hasEligibleAccount = true, liquidCouldHavePaid = true)
    )

    @Test
    fun outage_beatsBolt12OnlyViaLbtc_whenLiquidCouldHavePaid() = assertEquals(
        IsSwapDirectionAvailableUseCase.ERROR_PAY_LIGHTNING_WITH_LIQUID,
        errorId(LightningDestination.Bolt12, liquidCouldHavePaid = true)
    )

    @Test
    fun bolt12OnlyViaLbtc_whenNoLiquidCouldHavePaid() = assertEquals(
        LightningSendAvailability.ERROR_BOLT12_ONLY_VIA_LBTC,
        errorId(LightningDestination.Bolt12, liquidCouldHavePaid = false)
    )

    @Test
    fun bolt12WithEligibleAccount_returnsNull() =
        assertNull(errorId(LightningDestination.Bolt12, hasEligibleAccount = true))

    @Test
    fun insufficientFunds_whenLiquidAlsoShort_returnsNull() =
        assertNull(errorId(LightningDestination.Bolt11WithAmount, hasEligibleAccount = true))

    @Test
    fun directionAvailable_neverProducesOutageMessage() {
        LightningDestination.entries.forEach { destination ->
            listOf(true, false).forEach { hasEligible ->
                listOf(true, false).forEach { liquidCould ->
                    val id = errorId(
                        destination = destination,
                        isDirectionAvailable = true,
                        hasEligibleAccount = hasEligible,
                        liquidCouldHavePaid = liquidCould,
                    )
                    assertTrue(
                        id != IsSwapDirectionAvailableUseCase.ERROR_PAY_LIGHTNING_WITH_LIQUID,
                        "$destination/$hasEligible/$liquidCould leaked the outage message when re-enabled"
                    )
                }
            }
        }
    }

    @Test
    fun directionAvailable_preservesPreExistingMessages() {
        assertEquals(
            LightningSendAvailability.ERROR_AMOUNTLESS_BOLT11,
            errorId(LightningDestination.AmountlessBolt11, isDirectionAvailable = true)
        )
        assertEquals(
            LightningSendAvailability.ERROR_INVALID_ADDRESS,
            errorId(LightningDestination.Unknown, isDirectionAvailable = true)
        )
        assertEquals(
            LightningSendAvailability.ERROR_BOLT12_ONLY_VIA_LBTC,
            errorId(LightningDestination.Bolt12, isDirectionAvailable = true)
        )
    }

    @Test
    fun explicitLiquidSource_blocked_regardlessOfNativeLightning() = assertTrue(
        LightningSendAvailability.explicitLiquidSourceBlocked(
            isLightningDestination = true,
            accountIsLiquid = true,
            isDirectionAvailable = false,
        )
    )

    @Test
    fun explicitLightningSource_notBlocked() = assertFalse(
        LightningSendAvailability.explicitLiquidSourceBlocked(
            isLightningDestination = true,
            accountIsLiquid = false,
            isDirectionAvailable = false,
        )
    )

    @Test
    fun nonLightningAddress_neverBlocked() = assertFalse(
        LightningSendAvailability.explicitLiquidSourceBlocked(
            isLightningDestination = false,
            accountIsLiquid = true,
            isDirectionAvailable = false,
        )
    )

    @Test
    fun directionAvailable_notBlocked() = assertFalse(
        LightningSendAvailability.explicitLiquidSourceBlocked(
            isLightningDestination = true,
            accountIsLiquid = true,
            isDirectionAvailable = true,
        )
    )

    @Test
    fun bolt12_withShortLiquidRail_doesNotClaimOnlyViaLbtc() = assertNull(
        errorId(
            LightningDestination.Bolt12,
            liquidCouldHavePaid = false,
            liquidRailExists = true,
        )
    )

    @Test
    fun bolt12_withNoLiquidRailAtAll_keepsOnlyViaLbtc() = assertEquals(
        LightningSendAvailability.ERROR_BOLT12_ONLY_VIA_LBTC,
        errorId(
            LightningDestination.Bolt12,
            liquidCouldHavePaid = false,
            liquidRailExists = false,
        )
    )

    @Test
    fun bolt12_overExplicitLightningAccount_isBlocked() = assertTrue(
        LightningSendAvailability.bolt12OverLightningSource(
            destination = LightningDestination.Bolt12,
            assetIsLightning = false,
            accountIsLightning = true,
        )
    )

    @Test
    fun bolt12_overExplicitLightningAsset_isBlocked() = assertTrue(
        LightningSendAvailability.bolt12OverLightningSource(
            destination = LightningDestination.Bolt12,
            assetIsLightning = true,
            accountIsLightning = false,
        )
    )

    @Test
    fun bolt12_overExplicitLiquidAccount_isNotBlockedHere() = assertFalse(
        LightningSendAvailability.bolt12OverLightningSource(
            destination = LightningDestination.Bolt12,
            assetIsLightning = false,
            accountIsLightning = false,
        )
    )

    @Test
    fun nonBolt12_overLightningAccount_isNotBlocked() {
        listOf(
            LightningDestination.Bolt11WithAmount,
            LightningDestination.AmountlessBolt11,
            LightningDestination.LnUrl,
            LightningDestination.Unknown,
        ).forEach { destination ->
            assertFalse(
                LightningSendAvailability.bolt12OverLightningSource(
                    destination = destination,
                    assetIsLightning = true,
                    accountIsLightning = true,
                ),
                "$destination must not be blocked as BOLT12"
            )
        }
    }
}
