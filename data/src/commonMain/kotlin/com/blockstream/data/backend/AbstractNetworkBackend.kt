package com.blockstream.data.backend

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.ConnectionParams
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.utils.Loggable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

abstract class AbstractNetworkBackend(override val network: Network, val networkAssetManager: NetworkAssetManager) : NetworkBackend {
    protected val accountBackends: ConcurrentMutableMap<String, AccountBackend> = ConcurrentMutableMap()

    protected val state = MutableStateFlow(State())

    override val isConnected
        get() = state.value.isConnected

    override val isLoggedIn
        get() = state.value.isLoggedIn

    protected val _accounts = MutableStateFlow<List<Account>>(emptyList())
    final override val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    protected val _blockStateFlow = MutableStateFlow(Block())
    final override val blockStateFlow: StateFlow<Block> = _blockStateFlow.asStateFlow()

    protected val _networkEventsFlow = MutableSharedFlow<NetworkEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    final override val networkEventsFlow: SharedFlow<NetworkEvent> = _networkEventsFlow.asSharedFlow()

    abstract fun createAccountBackend(account: Account): AccountBackend

    override fun accountBackend(account: Account): AccountBackend {
        return accountBackends.computeIfAbsent(account.id) {
            createAccountBackend(account)
        }
    }

    override suspend fun isPolicyAsset(assetId: String?): Boolean = assetId.isPolicyAsset(network)

    override suspend fun disconnect() {
        if (isConnected) {
            state.update {
                it.copy(isConnected = false, isLoggedIn = false)
            }
            _accounts.value = emptyList()
            accountBackends.clear()
        } else {
            logger.d { "Already disconnected ${network.id}" }
        }
    }

    companion object: Loggable()
}

data class State(val isConnected: Boolean = false, val isLoggedIn: Boolean = false)