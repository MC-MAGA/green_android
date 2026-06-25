package com.blockstream.data.data

import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountlyAsset constructor(
    @SerialName("id")
    val assetId: String,
    @SerialName("amp2")
    val isAmp: Boolean = false,
    @SerialName("amp")
    val isAmpLegacy: Boolean = false,
    @SerialName("weight")
    val weight: Int = 0,
) : GreenJson<CountlyAsset>() {
    override fun kSerializer() = serializer()

    val isAmp2OrLegacy
        get() = isAmp || isAmpLegacy

    companion object {

        fun create(assetId: String, isAmp: Boolean = false, weight: Int = 0) =
            CountlyAsset(assetId = assetId, isAmp = isAmp, weight = weight)

    }
}