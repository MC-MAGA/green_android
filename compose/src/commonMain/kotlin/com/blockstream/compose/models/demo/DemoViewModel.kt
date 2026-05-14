package com.blockstream.compose.models.demo

import androidx.lifecycle.viewModelScope
import com.blockstream.data.Urls
import com.blockstream.data.data.DataState
import com.blockstream.data.gdk.GdkSession
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DemoViewModel : GreenViewModel() {

    val data = MutableStateFlow<DataState<Int>>(DataState.Loading)

    class LocalEvents {
        object EventOpenBrowser : Event
    }

    override suspend fun handleEvent(event: Event) {
        when (event) {
            is LocalEvents.EventOpenBrowser -> {
                postSideEffect(SideEffects.OpenBrowser(Urls.BLOCKSTREAM_GREEN_WEBSITE))
            }
        }
    }

    val gdkSession: GdkSession

    init {
        viewModelScope.launch {
            while (true) {
                delay(3000L)
                data.value = DataState.Success(1337)
                delay(3000L)
                data.value = DataState.Loading
                delay(3000L)
                data.value = DataState.Empty
                delay(3000L)
                data.value = DataState.Error(Exception("OMG an error"))
            }
        }

        this.gdkSession = sessionManager.getOnBoardingSession()
        bootstrap()
    }

    val accounts get() = gdkSession.accounts
}
