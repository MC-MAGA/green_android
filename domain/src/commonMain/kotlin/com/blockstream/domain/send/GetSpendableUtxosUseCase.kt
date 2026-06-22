package com.blockstream.domain.send

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.Utxo
import kotlinx.serialization.json.JsonElement

data class SpendableUtxo(
    val assetId: String,
    val utxo: Utxo,
    // GDK expects the original UTXO JSON in CreateTransactionParams.
    val rawUtxo: JsonElement
) {
    val id: String = "${utxo.txHash}:${utxo.index}"
}
class GetSpendableUtxosUseCase {
    suspend operator fun invoke(
        session: GdkSession,
        accountAsset: AccountAsset
    ): List<SpendableUtxo> {
        val unspentOutputs = session.getUnspentOutputs(accountAsset.account)

        return unspentOutputs.unspentOutputs[accountAsset.assetId].orEmpty().mapIndexedNotNull { index, rawUtxo ->
                val utxo = unspentOutputs.unspentOutputsAsUtxo[accountAsset.assetId]?.getOrNull(index)
                    ?: return@mapIndexedNotNull null

                SpendableUtxo(
                    assetId = accountAsset.assetId,
                    utxo = utxo,
                    rawUtxo = rawUtxo
                )
            }
            .filterNot { it.utxo.userStatus == LOCKED_USER_STATUS }
            .filter { (it.utxo.blockHeight ?: 0L) > 0L }
            .sortedBy { it.utxo.blockHeight ?: 0L }
    }

    companion object {
        private const val LOCKED_USER_STATUS = 1
    }
}

