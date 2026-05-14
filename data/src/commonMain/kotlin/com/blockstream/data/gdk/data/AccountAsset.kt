package com.blockstream.data.gdk.data

import com.blockstream.data.comparators.ComparatorAssets
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class AccountAssetList constructor(val list: List<AccountAsset>)

@Serializable
data class AccountAsset constructor(
    val account: Account,
    val asset: EnrichedAsset
) : GreenJson<AccountAsset>() {
    override fun kSerializer() = serializer()

    val assetId
        get() = asset.assetId

    val accountAssetBalance
        get() = AccountAssetBalance.create(this)

    val assetBalance
        get() = AssetBalance.create(this.asset)

    val network
        get() = account.network

    fun balance(session: GdkSession) = session.accountAssets(account).value.balance(assetId)

    companion object {
        suspend fun fromAccountAsset(account: Account, assetId: String, session: GdkSession): AccountAsset {
            return AccountAsset(account, EnrichedAsset.create(session, assetId))
        }
    }
}

fun List<AccountAsset>.sort(session: GdkSession): List<AccountAsset> {
    return this.sortedWith(comparator = Comparator { a1, a2 ->
        when {
            a1.account.isBitcoin && a2.account.isLiquid -> -1
            a1.account.isLiquid && a2.account.isBitcoin -> 1
            a1.asset.assetId == a2.asset.assetId -> a1.account.compareTo(a2.account)
            else -> {
                ComparatorAssets(session).compare(a1.asset.assetId, a2.asset.assetId)
            }
        }
    })
}

