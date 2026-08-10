package com.blockstream.domain.swap

import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession

sealed interface ResetSwapsResult {
    data object NoSwaps : ResetSwapsResult

    data object Reprocessing : ResetSwapsResult

    data object Queued : ResetSwapsResult
}

internal fun resetSwapsResultOf(hadSwaps: Boolean, isLwkConnected: Boolean): ResetSwapsResult = when {
    !hadSwaps -> ResetSwapsResult.NoSwaps
    isLwkConnected -> ResetSwapsResult.Reprocessing
    else -> ResetSwapsResult.Queued
}

/**
 */
class ResetWalletSwapsUseCase(
    private val database: Database
) {
    suspend operator fun invoke(session: GdkSession, xPubHashId: String): ResetSwapsResult {
        val hadSwaps = database.hasSwaps(xPubHashId = xPubHashId)

        database.setWalletSwapsPending(xPubHashId = xPubHashId)

        return resetSwapsResultOf(
            hadSwaps = hadSwaps,
            isLwkConnected = session.lwkOrNull?.isConnected == true
        )
    }
}
