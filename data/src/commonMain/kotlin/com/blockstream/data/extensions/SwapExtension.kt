package com.blockstream.data.extensions

import com.blockstream.data.data.SwapType
import com.blockstream.data.database.wallet.BoltzSwaps
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val CHAIN_BITCOIN = "BTC"

val BoltzSwaps.fromChain: String?
    get() = tryCatchNull {
        Json.parseToJsonElement(data_).jsonObject["from_chain"]?.jsonPrimitive?.content
    }

/**
 * A normal submarine swap funded from the Bitcoin chain (BTC -> Lightning).
 *
 * Both BTC -> Lightning and Liquid -> Lightning are stored as [SwapType.NormalSubmarine], so the
 * serialized LWK `from_chain` field ("BTC" or "L-BTC") is what tells the two apart. Swaps created
 * before LWK started serializing the field have no `from_chain` and are treated as Liquid funded.
 */
val BoltzSwaps.isBtcToLightning: Boolean
    get() = swap_type == SwapType.NormalSubmarine && fromChain == CHAIN_BITCOIN
