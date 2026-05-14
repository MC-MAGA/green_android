package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GdkSession

data class Assets constructor(val assetsOrNull: Map<String, Long>? = null) {
    val assets
        get() = assetsOrNull ?: emptyMap()

    val isLoading
        get() = assetsOrNull == null

    // By default policy asset is first
    val policyAssetOrNull
        get() = assets.entries.firstOrNull()?.value

    val policyAsset
        get() = policyAssetOrNull ?: 0

    val hasFunds: Boolean
        get() = assets.values.sum() > 0

    val size
        get() = assets.size

    fun isEmpty() = assets.isEmpty()

    fun isNotEmpty() = !isEmpty()

    fun balanceOrNull(assetId: String?) = assets[assetId]

    fun balance(assetId: String) = balanceOrNull(assetId) ?: 0

    operator fun plus(other: Assets): Assets {
        val combined = LinkedHashMap(assets)
        other.assets.forEach { (assetId, value) ->
            combined[assetId] = (combined[assetId] ?: 0) + value
        }
        return Assets(combined)
    }

    suspend fun toAccountAsset(account: Account, session: GdkSession): List<AccountAsset> {
        return assets.keys.map {
            AccountAsset.fromAccountAsset(account, it, session)
        }
    }
}