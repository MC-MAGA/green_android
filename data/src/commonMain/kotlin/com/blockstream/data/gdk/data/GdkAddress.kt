package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GdkAddress constructor(
    @SerialName("address")
    override val address: String,
    @SerialName("pointer")
    val pointer: Long = 0,
    @SerialName("address_type")
    val addressType: String? = null,
    @SerialName("branch")
    val branch: Long = 0,
    @SerialName("tx_count")
    override val txCount: Long? = null,
    @SerialName("script")
    val script: String? = null,
    @SerialName("subaccount")
    val subaccount: Int? = null,
    @SerialName("subtype")
    val subType: Long? = null,
    @SerialName("user_path")
    val userPath: List<Long>? = null,

    // Used only as AddressParams Sweep
    @SerialName("satoshi")
    var satoshi: Long = 0,
    @SerialName("is_greedy")
    var isGreedy: Boolean = true,
) : GreenJson<GdkAddress>(), Address {
    override fun encodeDefaultsValues() = true

    override fun kSerializer() = serializer()

    override val index: Long
        get() = pointer
}
