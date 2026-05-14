package com.blockstream.data.greenlight

import com.blockstream.data.gdk.data.Address
import kotlinx.serialization.Serializable

@Serializable
data class GlAddress constructor(
    override val address: String,
) : Address {
    override val index: Long = 0
}