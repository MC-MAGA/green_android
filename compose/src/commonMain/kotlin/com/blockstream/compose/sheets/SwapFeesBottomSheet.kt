package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_square_out
import blockstream_green.common.generated.resources.id_covers_swap_service
import blockstream_green.common.generated.resources.id_covers_transaction_confirmation
import blockstream_green.common.generated.resources.id_covers_your_first_lightning_payment
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_lightning_setup_fee
import blockstream_green.common.generated.resources.id_network_fee
import blockstream_green.common.generated.resources.id_swap_fee
import blockstream_green.common.generated.resources.id_total
import blockstream_green.common.generated.resources.id_total_fees
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.sideeffects.OpenBrowserType
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.data.Urls
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapFeesBottomSheet(
    networkFee: String,
    swapFee: String,
    totalFees: String,
    totalFeesFiat: String?,
    lightningSetupFee: String? = null,
    onDismissRequest: () -> Unit,
) {
    val platformManager = LocalPlatformManager.current

    GreenBottomSheet(
        title = stringResource(Res.string.id_total_fees),
        // Open fully expanded so the trailing link is never stuck behind the navigation bar
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(
            padding = 0,
            space = 16,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.id_network_fee),
                            style = labelLarge,
                            color = whiteHigh
                        )
                        Text(
                            text = stringResource(Res.string.id_covers_transaction_confirmation),
                            style = bodySmall,
                            color = whiteMedium
                        )
                    }
                    Text(
                        text = networkFee,
                        style = labelLarge,
                        color = whiteHigh
                    )
                }
            }

            lightningSetupFee?.let { setupFee ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.id_lightning_setup_fee),
                                style = labelLarge,
                                color = whiteHigh
                            )
                            Text(
                                text = stringResource(Res.string.id_covers_your_first_lightning_payment),
                                style = bodySmall,
                                color = whiteMedium
                            )
                        }
                        Text(
                            text = setupFee,
                            style = labelLarge,
                            color = whiteHigh
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.id_swap_fee),
                            style = labelLarge,
                            color = whiteHigh
                        )
                        Text(
                            text = stringResource(Res.string.id_covers_swap_service),
                            style = bodySmall,
                            color = whiteMedium
                        )
                    }
                    Text(
                        text = swapFee,
                        style = labelLarge,
                        color = whiteHigh
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(Res.string.id_total),
                    style = titleSmall,
                    color = whiteHigh,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totalFees,
                        style = titleSmall,
                        color = whiteHigh
                    )
                    totalFeesFiat?.let {
                        Text(
                            text = it,
                            style = labelLarge,
                            color = whiteMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        platformManager.openBrowser(
                            url = Urls.HELP_FEES,
                            type = OpenBrowserType.IN_APP
                        )
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.id_learn_more),
                        style = bodyMedium,
                        color = green,
                        textDecoration = TextDecoration.Underline
                    )
                    Icon(
                        painter = painterResource(Res.drawable.arrow_square_out),
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun SwapFeesBottomSheetPreview() {
    GreenPreview {
        SwapFeesBottomSheet(
            networkFee = "0.00000020 BTC",
            swapFee = "0.00012646 BTC",
            totalFees = "0.00013003 BTC",
            totalFeesFiat = "≈ 12.09 USD",
            onDismissRequest = {

            }
        )
    }
}
