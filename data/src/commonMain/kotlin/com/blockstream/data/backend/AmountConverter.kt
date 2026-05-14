package com.blockstream.data.backend

import kotlinx.serialization.json.JsonElement

interface AmountConverter {
    suspend fun isPolicyAsset(assetId: String?): Boolean
    suspend fun convertAmount(convert: JsonElement): JsonElement
}
