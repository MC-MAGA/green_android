package com.blockstream.data.managers

import com.blockstream.data.CountlyBase
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.GetAssetsParams
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

interface AssetsProvider {
    suspend fun refreshAssets(params: AssetsParams)
    suspend fun getAssets(params: GetAssetsParams): LiquidAssets?
}

/*
 * AssetManager is responsible of updating Assets and handle different caches
 * App Cache: cached data from apk
 * GDK Cache: cached data from a previous successful fetch
 */
class AssetManager(private val countly: CountlyBase) {
    private val liquidAssetManager by lazy {
        NetworkAssetManager(true, countly)
    }
    private val liquidTestnetAssetManager by lazy {
        NetworkAssetManager(false, countly)
    }

    fun getNetworkAssetManager(isMainnet: Boolean): NetworkAssetManager {
        return if (isMainnet) {
            liquidAssetManager
        } else {
            liquidTestnetAssetManager
        }
    }
}