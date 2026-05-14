package com.blockstream.data.data

import com.blockstream.data.gdk.data.Transaction
import kotlinx.serialization.Serializable

@Serializable
data class TransactionList(
    val transactions: List<Transaction> = listOf(),
    val hasMore: Boolean = false
)