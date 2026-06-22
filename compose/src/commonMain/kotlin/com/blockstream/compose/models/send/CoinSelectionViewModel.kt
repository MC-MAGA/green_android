package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import kotlinx.serialization.Serializable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.funnel
import blockstream_green.common.generated.resources.id_filters
import blockstream_green.common.generated.resources.id_select_your_coins
import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.shortOutpoint
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.send.SendUseCase
import com.blockstream.domain.send.SpendableUtxo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

@Serializable
enum class CoinFilter {
    ALL,
    CSV,
    P2WSH,
    P2SH,
    DUST,
    NOT_CONFIDENTIAL,
    EXPIRED
}

data class CoinSelectionListItem(
    val id: String,
    val amount: String,
    val satoshi: Long,
    val outpoint: String,
    val addressType: String,
    val blockHeight: Long?,
    val expiryHeight: Long?,
    val isBlinded: Boolean?,
    val isSelected: Boolean = false
)

data class CoinSelectionSummary(
    val count: Int = 0,
    val amount: String? = null,
    val canConfirm: Boolean = false
)

data class CoinSelectionResult(
    val selectedUtxoIds: List<String>,
    val selectedAmount: String?,
    val gdkPayloadUtxos: Map<String, List<JsonElement>>
)

private data class CoinSelectionData(
    val spendableUtxos: List<SpendableUtxo>,
    val items: List<CoinSelectionListItem>
)

abstract class CoinSelectionViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    class LocalEvents {
        data class ToggleCoin(val id: String) : Event
        data class SelectFilter(val filter: CoinFilter) : Event
        object ConfirmSelection : Event
        object OpenFilters : Event
    }

    override fun screenName(): String = "CoinSelection"

    abstract val coins: StateFlow<List<CoinSelectionListItem>>
    abstract val coinsCount: StateFlow<Int>
    abstract val summary: StateFlow<CoinSelectionSummary>
    abstract val selectedFilter: StateFlow<CoinFilter>
    abstract val availableFilters: List<CoinFilter>
}

class CoinSelectionViewModel(
    greenWallet: GreenWallet,
    private val selectedAccountAsset: AccountAsset,
    private val selectedUtxoIds: List<String> = emptyList()
) : CoinSelectionViewModelAbstract(greenWallet = greenWallet, accountAsset = selectedAccountAsset) {
    private val sendUseCase: SendUseCase by inject()

    private var spendableUtxos: List<SpendableUtxo> = emptyList()
    private var allCoins: List<CoinSelectionListItem> = emptyList()

    private val _coins: MutableStateFlow<List<CoinSelectionListItem>> = MutableStateFlow(emptyList())
    override val coins: StateFlow<List<CoinSelectionListItem>> = _coins.asStateFlow()

    private val _coinsCount: MutableStateFlow<Int> = MutableStateFlow(0)
    override val coinsCount: StateFlow<Int> = _coinsCount.asStateFlow()

    private val _summary: MutableStateFlow<CoinSelectionSummary> = MutableStateFlow(CoinSelectionSummary())
    override val summary: StateFlow<CoinSelectionSummary> = _summary.asStateFlow()

    private val _selectedFilter: MutableStateFlow<CoinFilter> = MutableStateFlow(CoinFilter.ALL)
    override val selectedFilter: StateFlow<CoinFilter> = _selectedFilter.asStateFlow()

    override val availableFilters: List<CoinFilter> = buildList {
        add(CoinFilter.ALL)
        add(CoinFilter.CSV)
        add(CoinFilter.P2WSH)

        if (selectedAccountAsset.account.isLiquid) {
            add(CoinFilter.NOT_CONFIDENTIAL)
        } else {
            add(CoinFilter.P2SH)
            add(CoinFilter.DUST)
        }

        if (selectedAccountAsset.account.type != AccountType.TWO_OF_THREE &&
            selectedAccountAsset.account.type != AccountType.AMP_ACCOUNT
        ) {
            add(CoinFilter.EXPIRED)
        }
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_select_your_coins),
                actions = listOf(
                    NavAction(
                        title = getString(Res.string.id_filters),
                        icon = Res.drawable.funnel,
                        isMenuEntry = false,
                        onClick = {
                            postEvent(LocalEvents.OpenFilters)
                        }
                    )
                )
            )
        }

        doAsync({
            val spendableUtxos = sendUseCase.getSpendableUtxosUseCase(
                session = session,
                accountAsset = selectedAccountAsset
            )

            CoinSelectionData(
                spendableUtxos = spendableUtxos,
                items = spendableUtxos.map { coin ->
                    CoinSelectionListItem(
                        id = coin.id,
                        amount = coin.utxo.satoshi.toAmountLook(
                            session = session,
                            assetId = coin.assetId,
                            withUnit = true,
                            withGrouping = true
                        ) ?: "${coin.utxo.satoshi}",
                        satoshi = coin.utxo.satoshi,
                        outpoint = coin.utxo.shortOutpoint(),
                        addressType = coin.utxo.addressType,
                        blockHeight = coin.utxo.blockHeight,
                        expiryHeight = coin.utxo.expiryHeight,
                        isBlinded = coin.utxo.isBlinded,
                        isSelected = coin.id in selectedUtxoIds
                    )
                }
            )
        }, onSuccess = {
            spendableUtxos = it.spendableUtxos
            allCoins = it.items
            _coinsCount.value = allCoins.size
            applyFilter()
            viewModelScope.launch {
                updateSummary(allCoins)
            }
        })

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ToggleCoin -> {
                toggleCoin(event.id)
            }

            is LocalEvents.SelectFilter -> {
                selectFilter(event.filter)
            }

            is LocalEvents.ConfirmSelection -> {
                confirmSelection()
            }

            is LocalEvents.OpenFilters -> {
                openFilters()
            }
        }
    }

    private fun toggleCoin(id: String) {
        allCoins = allCoins.map {
            if (it.id == id) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
        applyFilter()

        viewModelScope.launch {
            updateSummary(allCoins)
        }
    }

    private suspend fun confirmSelection() {
        val selectedIds = allCoins.filter { it.isSelected }.map { it.id }.toSet()
        val selectedUtxos = spendableUtxos.filter { it.id in selectedIds }

        NavigateDestinations.CoinSelection.setResult(
            CoinSelectionResult(
                selectedUtxoIds = selectedIds.toList(),
                selectedAmount = selectedUtxos.takeIf { it.isNotEmpty() }?.sumOf { it.utxo.satoshi }.toAmountLook(
                    session = session,
                    assetId = selectedAccountAsset.assetId,
                    withUnit = true,
                    withGrouping = true
                ),
                gdkPayloadUtxos = selectedUtxos
                    .groupBy { it.assetId }
                    .mapValues { (_, coins) -> coins.map { it.rawUtxo } }
            )
        )
        postSideEffect(SideEffects.NavigateBack())
    }

    private fun selectFilter(filter: CoinFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    private fun openFilters() {
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.CoinFilters(
                    selectedFilter = selectedFilter.value,
                    availableFilters = availableFilters
                )
            )
        )
    }

    private fun applyFilter() {
        _coins.value = allCoins.filter { coin ->
            when (_selectedFilter.value) {
                CoinFilter.ALL -> true
                CoinFilter.CSV -> coin.addressType == "csv"
                CoinFilter.P2WSH -> coin.addressType == "p2wsh"
                CoinFilter.P2SH -> coin.addressType == "p2sh"
                CoinFilter.DUST -> coin.satoshi < GREEN_DUST_COIN_THRESHOLD_SATS
                CoinFilter.NOT_CONFIDENTIAL -> coin.isBlinded != true
                CoinFilter.EXPIRED -> coin.expiryHeight?.let { it <= session.block(selectedAccountAsset.account.network).value.height } == true
            }
        }
    }

    private suspend fun updateSummary(coins: List<CoinSelectionListItem>) {
        val selected = coins.filter { it.isSelected }
        _summary.value = CoinSelectionSummary(
            count = selected.size,
            amount = selected.sumOf { it.satoshi }.toAmountLook(
                session = session,
                assetId = selectedAccountAsset.assetId,
                withUnit = true,
                withGrouping = true
            ),
            canConfirm = selected.isNotEmpty() || selectedUtxoIds.isNotEmpty()
        )
    }

    companion object {
        // App coin-control threshold used by Desktop/iOS for the Dust filter.
        // This is not GDK's network dust limit.
        private const val GREEN_DUST_COIN_THRESHOLD_SATS = 1092L
    }
}

class CoinSelectionViewModelPreview(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : CoinSelectionViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override val coins: StateFlow<List<CoinSelectionListItem>> = MutableStateFlow(
        listOf(
            CoinSelectionListItem(
                id = "coin-1",
                amount = "0.015 BTC",
                satoshi = 1_500_000,
                outpoint = "3a5f1e2b...9c8d7a6f:0",
                addressType = "p2wsh",
                blockHeight = 860_000,
                expiryHeight = null,
                isBlinded = false,
                isSelected = true
            ),
            CoinSelectionListItem(
                id = "coin-2",
                amount = "0.004 BTC",
                satoshi = 400_000,
                outpoint = "0f4b12aa...77c390de:1",
                addressType = "csv",
                blockHeight = 859_940,
                expiryHeight = null,
                isBlinded = false
            )
        )
    )
    override val coinsCount: StateFlow<Int> = MutableStateFlow(coins.value.size)
    override val summary: StateFlow<CoinSelectionSummary> = MutableStateFlow(
        CoinSelectionSummary(
            count = 1,
            amount = "0.015 BTC",
            canConfirm = true
        )
    )
    override val selectedFilter: StateFlow<CoinFilter> = MutableStateFlow(CoinFilter.ALL)
    override val availableFilters: List<CoinFilter> = CoinFilter.entries

    companion object {
        fun preview() = CoinSelectionViewModelPreview(
            greenWallet = previewWallet(),
            accountAsset = previewAccountAsset()
        )
    }
}
