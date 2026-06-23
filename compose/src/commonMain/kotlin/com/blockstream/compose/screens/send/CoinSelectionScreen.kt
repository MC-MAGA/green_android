package com.blockstream.compose.screens.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.check_circle
import blockstream_green.common.generated.resources.id_confirm_coin_selection
import blockstream_green.common.generated.resources.id_coin
import blockstream_green.common.generated.resources.id_coins
import blockstream_green.common.generated.resources.id_no_coins_selected
import blockstream_green.common.generated.resources.id_no_utxos_found
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.models.send.CoinFilter
import com.blockstream.compose.models.send.CoinSelectionViewModelAbstract
import com.blockstream.compose.models.send.CoinSelectionViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(coins) { coin ->
                        GreenDataLayout(
                            withPadding = false,
                            onClick = {
                                viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.ToggleCoin(coin.id))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
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

@Preview
@Composable
fun CoinSelectionScreenPreview() {
    GreenPreview {
        CoinSelectionScreen(viewModel = CoinSelectionViewModelPreview.preview())
    }
}
