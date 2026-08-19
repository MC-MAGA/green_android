package com.blockstream.compose.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_square_out
import blockstream_green.common.generated.resources.id_coin_details
import blockstream_green.common.generated.resources.id_received_on
import blockstream_green.common.generated.resources.id_transaction_id
import blockstream_green.common.generated.resources.id_view_in_explorer
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.extensions.colorTextEdges
import com.blockstream.compose.models.sheets.CoinInfoViewModelPreview
import com.blockstream.compose.models.sheets.CoinInfoViewModelAbstract
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.compose.utils.StringHolder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoinInfoBottomSheet(
    viewModel: CoinInfoViewModelAbstract,
    onDismissRequest: () -> Unit
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_coin_details),
        titleTextAlign = TextAlign.Start,
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {
        val data by viewModel.data.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        AnimatedContent(
            targetState = data,
            label = "CoinInfoData"
        ) { items ->
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach { pair ->
                    CoinInfoDataListItem(
                        title = pair.first,
                        data = pair.second
                    )
                }
            }
        }

        if (onProgress) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = green, strokeWidth = 2.dp)
            }
        }

        TextButton(
            onClick = {
                viewModel.postEvent(CoinInfoViewModelAbstract.LocalEvents.ViewInExplorer)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.id_view_in_explorer),
                style = bodyLarge,
                color = green,
                textDecoration = TextDecoration.Underline
            )
            Icon(
                painter = painterResource(Res.drawable.arrow_square_out),
                contentDescription = null,
                tint = green,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun CoinInfoDataListItem(
    title: StringHolder,
    data: StringHolder
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title.string(),
            style = bodyLarge,
            color = whiteMedium,
            modifier = Modifier.weight(1f)
        )
        when (title.stringResource) {
            Res.string.id_received_on -> {
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    CompactAddress(address = data.string())
                }
            }

            Res.string.id_transaction_id -> {
                val transactionId = data.string()
                CopyContainer(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(RoundedCornerShape(6.dp)),
                    value = transactionId,
                    withSelection = false
                ) {
                    Text(
                        text = transactionId.shortMiddle(),
                        style = bodyMedium,
                        color = whiteHigh,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> {
                Text(
                    text = data.string(),
                    style = bodyMedium,
                    color = whiteHigh,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.15f)
                )
            }
        }
    }
}

@Composable
private fun CompactAddress(address: String) {
    val formattedAddress = remember(address) {
        address.shortAddress()
    }

    CopyContainer(value = address, withSelection = false) {
        Text(
            text = colorTextEdges(text = formattedAddress, numberOfSections = 2),
            fontFamily = MonospaceFont(),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun String.shortAddress(): String =
    if (length > 32) {
        "${take(12).chunked(4).joinToString(" ")} ... ${takeLast(12).chunked(4).joinToString(" ")}"
    } else {
        chunked(4).joinToString(" ")
    }

private fun String.shortMiddle(): String =
    if (length > 18) "${take(6)}...${takeLast(12)}" else this

@Preview
@Composable
fun CoinInfoBottomSheetPreview() {
    GreenPreview {
        CoinInfoBottomSheet(
            viewModel = CoinInfoViewModelPreview(),
            onDismissRequest = {}
        )
    }
}
