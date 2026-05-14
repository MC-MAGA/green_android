package com.blockstream.data.gdk.data

interface Address {
    val address: String
    val index: Long
    val txCount: Long?
        get() = null
}
