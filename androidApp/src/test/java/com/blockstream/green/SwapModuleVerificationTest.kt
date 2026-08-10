package com.blockstream.green

import com.blockstream.domain.swap.IsSwapDirectionAvailableUseCase
import com.blockstream.domain.swap.SwapAvailability
import com.blockstream.domain.swap.SwapDirection
import com.blockstream.domain.swap.swapModule
import org.junit.After
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.assertFalse
import kotlin.test.assertSame

class SwapModuleVerificationTest : KoinTest {
    @After
    fun tearDown() = stopKoin()

    @Test
    fun swapAvailabilityGate_resolvesFromKoin() {
        startKoin { modules(swapModule) }

        val gate = get<IsSwapDirectionAvailableUseCase>()
        val availability = get<SwapAvailability>()

        assertSame(SwapAvailability.Current, availability)

        assertFalse(gate.hasAnyEnabledDirection())
        SwapDirection.All.forEach { direction ->
            assertFalse(gate.isEnabled(direction), "expected $direction to be blocked")
        }
    }
}
