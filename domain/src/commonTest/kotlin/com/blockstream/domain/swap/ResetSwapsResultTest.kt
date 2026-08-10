package com.blockstream.domain.swap

import kotlin.test.Test
import kotlin.test.assertEquals

class ResetSwapsResultTest {
    @Test
    fun noSwaps_regardlessOfConnectivity() {
        assertEquals(
            ResetSwapsResult.NoSwaps,
            resetSwapsResultOf(hadSwaps = false, isLwkConnected = true)
        )
        assertEquals(
            ResetSwapsResult.NoSwaps,
            resetSwapsResultOf(hadSwaps = false, isLwkConnected = false)
        )
    }

    @Test
    fun swapsFlaggedWhileConnected_isReprocessing() = assertEquals(
        ResetSwapsResult.Reprocessing,
        resetSwapsResultOf(hadSwaps = true, isLwkConnected = true)
    )

    @Test
    fun swapsFlaggedWhileDisconnected_isQueued() = assertEquals(
        ResetSwapsResult.Queued,
        resetSwapsResultOf(hadSwaps = true, isLwkConnected = false)
    )

    @Test
    fun successIsNeverClaimedWithoutSwaps() {
        listOf(true, false).forEach { connected ->
            val result = resetSwapsResultOf(hadSwaps = false, isLwkConnected = connected)
            assertEquals(ResetSwapsResult.NoSwaps, result, "connected=$connected")
        }
    }
}
