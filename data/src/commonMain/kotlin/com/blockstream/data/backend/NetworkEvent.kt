package com.blockstream.data.backend

sealed class NetworkEvent {
    data class Block(val height: Long) : NetworkEvent()
    data class Transaction(val accountPointers: List<Long>) : NetworkEvent()
    data class Synced(val accountPointer: Long) : NetworkEvent()
}
