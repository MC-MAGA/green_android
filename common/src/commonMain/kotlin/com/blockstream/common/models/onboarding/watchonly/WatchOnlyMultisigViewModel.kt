package com.blockstream.common.models.onboarding.watchonly

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.ui.events.Event
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow as ObservableMutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

abstract class WatchOnlyMultisigViewModelAbstract(val setupArgs: SetupArgs) : GreenViewModel() {
    override fun screenName(): String = "OnBoardWatchOnlyMultisig"

    override fun segmentation(): HashMap<String, Any>? =
        setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    @NativeCoroutinesState
    abstract val isLoginEnabled: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val username: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val password: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val isRememberMe: MutableStateFlow<Boolean>
}

class WatchOnlyMultisigViewModel(setupArgs: SetupArgs) :
    WatchOnlyMultisigViewModelAbstract(setupArgs = setupArgs) {

    override val username: MutableStateFlow<String> = ObservableMutableStateFlow(viewModelScope, "")
    override val password: MutableStateFlow<String> = ObservableMutableStateFlow(viewModelScope, "")
    override val isRememberMe: MutableStateFlow<Boolean> = ObservableMutableStateFlow(viewModelScope, true)

    override val isLoginEnabled: StateFlow<Boolean> = combine(
        username,
        password,
        onProgress
    ) { username, password, onProgress ->
        !onProgress && username.isNotBlank() && password.isNotBlank()
    }.stateIn(viewModelScope.coroutineScope, SharingStarted.WhileSubscribed(), false)

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is Events.Continue -> {
                createMultisigWatchOnlyWallet()
            }
        }
    }

    private fun createMultisigWatchOnlyWallet() {
        val watchOnlyCredentials = WatchOnlyCredentials(
            username = username.value,
            password = password.value
        )

        // Use the network from setupArgs (should be a Green network for multisig)
        val network = setupArgs.network ?: session.networks.bitcoinGreen

        createNewWatchOnlyWallet(
            network = network,
            persistLoginCredentials = isRememberMe.value,
            watchOnlyCredentials = watchOnlyCredentials,
        )
    }
}