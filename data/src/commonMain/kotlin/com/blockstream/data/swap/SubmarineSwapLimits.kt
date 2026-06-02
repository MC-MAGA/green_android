package com.blockstream.data.swap

import kotlinx.serialization.Serializable

@Serializable
data class SubmarineSwapLimits(
    val maximal: Long,
    val minimal: Long,
    val minimalBatched: Long? = null,
) {
    val minimumSats: Long
        get() = minimalBatched ?: minimal
}
