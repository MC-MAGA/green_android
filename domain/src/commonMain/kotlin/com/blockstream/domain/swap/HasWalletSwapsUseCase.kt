package com.blockstream.domain.swap

import com.blockstream.data.database.Database

class HasWalletSwapsUseCase(private val database: Database) {
    suspend operator fun invoke(xPubHashId: String): Boolean =
        xPubHashId.isNotBlank() && database.hasSwaps(xPubHashId = xPubHashId)
}
