package com.blockstream.data.backend

import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.ConnectionParams
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.managers.AssetsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

interface NetworkBackend : AssetsProvider {

    val network: Network
    val isConnected: Boolean
    val isLoggedIn: Boolean

    val networkEventsFlow: SharedFlow<NetworkEvent>
    val blockStateFlow : StateFlow<Block>

    val accounts: StateFlow<List<Account>>

    fun accountBackend(account: Account): AccountBackend

    suspend fun isPolicyAsset(assetId: String?): Boolean

    suspend fun connect(params: ConnectionParams)

    suspend fun isAddressValid(address: String): Boolean

    suspend fun getAccounts(refresh: Boolean = false): List<Account>

    suspend fun getAccount(account: Account): Account

    suspend fun disconnect()
    suspend fun createAccount(params: SubAccountParams, hardwareWalletResolver: HardwareWalletResolver? = null): Account

    suspend fun broadcastTransaction(broadcastTransaction: BroadcastTransactionParams): ProcessedTransactionDetails
}

inline fun <R> Map<Network, NetworkBackend>.mapLoggedIn(transform: (NetworkBackend) -> R): List<R> {
    return filter {
        it.value.isLoggedIn
    }.values.map(transform = transform)
}

/**
 * Combines a per-backend list flow (selected by [selector]) from every backend of type [R]
 * into a single flat list. Backends not assignable to [R] are skipped, as are those for
 * which [selector] returns `null`.
 *
 * An empty input produces an empty list. This case is handled explicitly because [combine]
 * over an empty collection of flows never emits, which would otherwise leave the result
 * stuck on the previous (non-empty) value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified R : NetworkBackend, T> Flow<Map<Network, NetworkBackend>>.combineBackends(
    crossinline selector: (R) -> Flow<List<T>>?
): Flow<List<T>> =
    flatMapLatest { backends ->
        val flows = backends.values.filterIsInstance<R>().mapNotNull { selector(it) }
        if (flows.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(flows) { perBackend -> perBackend.toList().flatten() }
        }
    }
