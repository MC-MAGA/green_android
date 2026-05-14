package com.blockstream.data.lwk

import com.blockstream.data.gdk.data.Address
import kotlinx.serialization.Serializable

@Serializable
data class LwkAddress(
    override val address: String,
    override val index: Long = 0,
) : Address
