package com.blockstream.domain.swap

import com.blockstream.data.database.Database

/**
 * Marks all stored swaps of a wallet as pending so LWK picks them up again for
 * background processing.
 *
 * Used to recover stuck swaps: flagging the swaps as pending causes them to be
 * retried the next time LWK connects and processes Boltz events.
 */
class ResetWalletSwapsUseCase(
    private val database: Database
) {

    /**
     * Flags all swaps of the wallet identified by [xPubHashId] as pending.
     */
    suspend operator fun invoke(xPubHashId: String) {
        database.setWalletSwapsPending(xPubHashId = xPubHashId)
    }
}