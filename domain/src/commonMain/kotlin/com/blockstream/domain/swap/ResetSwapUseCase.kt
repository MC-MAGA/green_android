package com.blockstream.domain.swap

import com.blockstream.data.database.Database

/**
 * Marks a stored swap as pending so LWK picks it up again for background processing.
 *
 * Used to recover stuck swaps: flagging the swap as pending causes it to be retried
 * the next time LWK connects and processes Boltz events.
 */
class ResetSwapUseCase(
    private val database: Database
) {

    /**
     * Flags the swap identified by [swapId] as pending.
     */
    suspend operator fun invoke(swapId: String) {
        database.setSwapPending(id = swapId)
    }
}