package com.blockstream.data.comparators

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.LN_BTC_POLICY_ASSET
import com.blockstream.data.backend.NetworkBackend
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.data.managers.NetworkAssetManager
import kotlinx.coroutines.runBlocking

class ComparatorAssets(
    private val networkBackends: List<NetworkBackend>,
    private val networkAssetManager: NetworkAssetManager,
    private val assetsProvider: AssetsProvider
) : Comparator<String> {

    constructor(session: GdkSession) : this(session.networkBackends.values.toList(), session.networkAssetManager, session)

    constructor(networkBackend: NetworkBackend, networkAssetManager: NetworkAssetManager, assetsProvider: AssetsProvider) : this(
        listOf(
            networkBackend
        ), networkAssetManager, assetsProvider
    )

    override fun compare(a: String, b: String): Int {
        return when {
            a == b -> 0
            a == BTC_POLICY_ASSET -> -1
            b == BTC_POLICY_ASSET -> 1
            a == LN_BTC_POLICY_ASSET -> -1
            b == LN_BTC_POLICY_ASSET -> 1
            a.isPolicyAsset(networkBackends) -> -1 // Liquid
            b.isPolicyAsset(networkBackends) -> 1 // Liquid
            else -> {
                runBlocking {
                    val asset1 = networkAssetManager.getAsset(a, assetsProvider)
                    val icon1 = networkAssetManager.getAssetIcon(a, assetsProvider)

                    val asset2 = networkAssetManager.getAsset(b, assetsProvider)
                    val icon2 = networkAssetManager.getAssetIcon(b, assetsProvider)

                    if ((icon1 == null) xor (icon2 == null)) {
                        if (icon1 != null) -1 else 1
                    } else if ((asset1 == null) xor (asset2 == null)) {
                        if (asset1 != null) -1 else 1
                    } else if (asset1 != null && asset2 != null) {
                        val weight1 = networkAssetManager.getCountlyAsset(a)?.weight ?: 0
                        val weight2 = networkAssetManager.getCountlyAsset(b)?.weight ?: 0

                        if (weight1 == weight2) {
                            asset1.name.compareTo(asset2.name)
                        } else {
                            weight2.compareTo(weight1)
                        }
                    } else {
                        a.compareTo(b)
                    }
                }
            }
        }
    }
}