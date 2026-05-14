package com.blockstream.data.backend

import com.blockstream.data.data.DataState
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Address
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.Transactions
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.TransactionParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AccountBackend {
    val assets: StateFlow<Assets>

    suspend fun getReceiveAddress(): Address
    suspend fun getBalance(confirmations: Int = 0): Map<String, Long>
    suspend fun getTransactions(params: TransactionParams = TransactionParams()): Transactions

    suspend fun getTransaction(id: String): Transaction?

    suspend fun getOutputDescriptors(): String?

    suspend fun getUnspentOutputs(
        isBump: Boolean,
        isExpired: Boolean,
        expiredAt: Long?
    ): UnspentOutputs

    suspend fun createTransaction(params: CreateTransactionParams): CreateTransaction

    suspend fun signTransaction(createTransaction: CreateTransaction): CreateTransaction
    fun setAssets(assets: Assets)
}

/**
 * Provides the per-account [assets], [transactions] and [hasMoreTransactions] flows that every
 * concrete backend exposes. Concrete backends extend this and only implement the network-specific
 * fetching methods.
 */
abstract class AbstractAccountBackend : AccountBackend {
    final override val assets: StateFlow<Assets>
        field = MutableStateFlow(Assets())

    override fun setAssets(assets: Assets) {
        this.assets.value = assets
    }

    // Backends whose getTransactions returns the full history in one call can look up by hash
    // directly. Backends that page (e.g. GDK) override this.
    override suspend fun getTransaction(id: String): Transaction? =
        getTransactions().transactions.firstOrNull { it.txHash == id }
}