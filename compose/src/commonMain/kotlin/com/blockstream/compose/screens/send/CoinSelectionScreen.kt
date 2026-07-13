package com.blockstream.compose.screens.send

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.check_circle
import blockstream_green.common.generated.resources.id_2fa_expired
import blockstream_green.common.generated.resources.id_confirm_coin_selection
import blockstream_green.common.generated.resources.id_coin
import blockstream_green.common.generated.resources.id_coins
import blockstream_green.common.generated.resources.id_dust
import blockstream_green.common.generated.resources.id_no_coins_selected
import blockstream_green.common.generated.resources.id_no_utxos_found
import blockstream_green.common.generated.resources.id_select_all
import blockstream_green.common.generated.resources.id_unselect_all
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.models.send.CoinFilter
import com.blockstream.compose.models.send.CoinSelectionListItem
import com.blockstream.compose.models.send.CoinSelectionViewModelAbstract
import com.blockstream.compose.models.send.CoinSelectionViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelSmall
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoinSelectionScreen(
    viewModel: CoinSelectionViewModelAbstract
) {
    SetupScreen(viewModel = viewModel) {
        val coins by viewModel.coins.collectAsStateWithLifecycle()
        val coinsCount by viewModel.coinsCount.collectAsStateWithLifecycle()
        val summary by viewModel.summary.collectAsStateWithLifecycle()
        val allVisibleCoinsSelected by viewModel.allVisibleCoinsSelected.collectAsStateWithLifecycle()

        NavigateDestinations.CoinFilters.getResult<CoinFilter> {
            viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.SelectFilter(it))
        }

        if (coinsCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.id_no_utxos_found),
                    style = bodyMedium,
                    color = whiteMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            if (coins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.id_no_utxos_found),
                        style = bodyMedium,
                        color = whiteMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                GreenButton(
                    text = stringResource(
                        if (allVisibleCoinsSelected) {
                            Res.string.id_unselect_all
                        } else {
                            Res.string.id_select_all
                        }
                    ),
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.SMALL,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 8.dp)
                ) {
                    viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.ToggleVisibleCoinsSelection)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(coins) { coin ->
                        CoinSelectionCard(
                            coin = coin,
                            onClick = {
                                viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.ToggleCoin(coin.id))
                            }
                        )
                    }
                }
            }

            val selectionText = if (summary.count == 0) {
                stringResource(Res.string.id_no_coins_selected)
            } else {
                "${summary.count} ${stringResource(if (summary.count == 1) Res.string.id_coin else Res.string.id_coins)}"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectionText,
                    style = bodyMedium,
                    color = whiteMedium,
                    textAlign = if (summary.count == 0) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )

                summary.amount?.takeIf { summary.count > 0 }?.also {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = it,
                            style = titleSmall,
                            color = whiteHigh
                        )
                        summary.amountFiat?.also { amountFiat ->
                            Text(
                                text = amountFiat,
                                style = bodyMedium,
                                color = whiteMedium
                            )
                        }
                    }
                }
            }

            GreenButton(
                text = stringResource(Res.string.id_confirm_coin_selection),
                enabled = summary.canConfirm,
                size = GreenButtonSize.BIG,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.ConfirmSelection)
            }
        }
    }
}

@Composable
private fun CoinSelectionCard(
    coin: CoinSelectionListItem,
    onClick: () -> Unit
) {
    GreenDataLayout(
        withPadding = false,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val labels = coin.displayLabels()
                if (labels.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        labels.forEach {
                            CoinLabel(it)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = coin.amount,
                        style = titleSmall,
                        color = whiteHigh
                    )
                    coin.amountFiat?.also {
                        Text(
                            text = it,
                            style = bodySmall,
                            color = whiteMedium
                        )
                    }
                }

                Text(
                    text = coin.outpoint,
                    style = bodyMedium,
                    color = whiteMedium
                )
            }

            if (coin.isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.check_circle),
                    contentDescription = null,
                    tint = green
                )
            }
        }
    }
}

@Composable
private fun CoinLabel(label: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, whiteMedium.copy(alpha = 0.7f))
    ) {
        Text(
            text = label,
            style = labelSmall,
            color = whiteMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CoinSelectionListItem.displayLabels(): List<String> {
    return labels.mapNotNull {
        when (it) {
            CoinFilter.ALL -> null
            CoinFilter.DUST -> stringResource(Res.string.id_dust)
            CoinFilter.EXPIRED -> stringResource(Res.string.id_2fa_expired)
        }
    }
}

@Preview
@Composable
fun CoinSelectionScreenPreview() {
    GreenPreview {
        CoinSelectionScreen(viewModel = CoinSelectionViewModelPreview.preview())
    }
}

@Preview
@Composable
fun CoinSelectionCardsPreview() {
    GreenPreview {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-without-label",
                    amount = "2 226 sats",
                    amountFiat = "≈ 1.38 USD",
                    satoshi = 2_226,
                    outpoint = "a06ee2f9...5a849ed:0",
                    addressType = "p2wpkh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    labels = emptyList(),
                    isSelected = false
                ),
                onClick = {}
            )

            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-dust",
                    amount = "546 sats",
                    amountFiat = "≈ 0.33 USD",
                    satoshi = 546,
                    outpoint = "4ac7d6f8...ed6af123:1",
                    addressType = "p2wsh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    labels = listOf(CoinFilter.DUST),
                    isSelected = false
                ),
                onClick = {}
            )

            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-expired",
                    amount = "2 226 sats",
                    amountFiat = "≈ 1.38 USD",
                    satoshi = 2_226,
                    outpoint = "5ccf420f...bbf1208c:0",
                    addressType = "p2wsh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    labels = listOf(CoinFilter.EXPIRED),
                    isSelected = true
                ),
                onClick = {}
            )

            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-expired-dust",
                    amount = "900 sats",
                    amountFiat = "≈ 0.55 USD",
                    satoshi = 900,
                    outpoint = "9f1a1eb9...bcb168aa:2",
                    addressType = "p2wsh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    labels = listOf(CoinFilter.EXPIRED, CoinFilter.DUST),
                    isSelected = false
                ),
                onClick = {}
            )
        }
    }
}
