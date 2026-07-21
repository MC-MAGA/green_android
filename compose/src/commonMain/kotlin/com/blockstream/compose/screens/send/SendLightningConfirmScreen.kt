package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.bitcoin_lightning
import androidx.compose.material3.HorizontalDivider
import blockstream_green.common.generated.resources.id_amount
import blockstream_green.common.generated.resources.id_from
import blockstream_green.common.generated.resources.id_to
import blockstream_green.common.generated.resources.info
import blockstream_green.common.generated.resources.id_total_fees
import blockstream_green.common.generated.resources.id_total_spent
import blockstream_green.common.generated.resources.id_total_to_receive
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.data.transaction.TransactionConfirmation
import blockstream_green.common.generated.resources.id_asset
import blockstream_green.common.generated.resources.id_lightning_bitcoin
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_recipient
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.domain.swap.hasMultipleSwapAccounts
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenConfirmButton
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.models.send.SendLightningConfirmViewModel
import com.blockstream.compose.models.send.SendLightningConfirmViewModelAbstract
import com.blockstream.compose.models.send.SendLightningConfirmViewModelPreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.displaySmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.dialogs.TransactionFailedDialog
import com.blockstream.compose.dialogs.TransactionSuccessDialog
import com.blockstream.compose.utils.SetupScreen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendLightningConfirmScreen(
    viewModel: SendLightningConfirmViewModelAbstract,
) {
    val transactionConfirmation by viewModel.transactionConfirmation.collectAsStateWithLifecycle()
    val invoiceAmount by viewModel.invoiceAmount.collectAsStateWithLifecycle()
    val invoiceAmountFiat by viewModel.invoiceAmountFiat.collectAsStateWithLifecycle()
    val onProgressSending by viewModel.onProgressSending.collectAsStateWithLifecycle()
    val successAmount by viewModel.successAmount.collectAsStateWithLifecycle()
    val failureMessage by viewModel.failureMessage.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()

    successAmount?.also {
        TransactionSuccessDialog(
            amount = it,
            onDismissRequest = viewModel::onSuccessAcknowledged,
        )
    }

    failureMessage?.also {
        TransactionFailedDialog(
            message = it,
            onDismissRequest = viewModel::onFailureAcknowledged,
        )
    }

    SetupScreen(
        viewModel = viewModel,
        onProgressStyle = if (onProgressSending) OnProgressStyle.Full(bluBackground = true) else OnProgressStyle.Disabled,
        withPadding = false,
    ) {
        GreenColumn(
            padding = 0,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            val isSwap = transactionConfirmation?.isSwap == true

            GreenColumn(
                padding = 0,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (isSwap) {
                    transactionConfirmation?.from?.also {
                        GreenAccountAsset(
                            accountAssetBalance = it.accountAssetBalance,
                            session = viewModel.sessionOrNull,
                            title = stringResource(Res.string.id_from),
                            withAsset = true,
                            withAccountName = hasMultipleSwapAccounts(viewModel.sessionOrNull, it)
                        )
                    }

                    transactionConfirmation?.to?.also {
                        GreenAccountAsset(
                            accountAssetBalance = it.accountAssetBalance,
                            session = viewModel.sessionOrNull,
                            title = stringResource(Res.string.id_to),
                            withAsset = true,
                            withAccountName = hasMultipleSwapAccounts(viewModel.sessionOrNull, it)
                        )
                    }
                } else {
                    GreenDataLayout(title = stringResource(Res.string.id_asset), withPadding = false) {
                        LightningAssetRowContent()
                    }

                    GreenDataLayout(title = stringResource(Res.string.id_recipient), withPadding = false) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 70.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            ChunkedInvoice(invoice = viewModel.invoice, isLightning = true)
                        }
                    }
                }

                (invoiceAmount ?: transactionConfirmation?.amount)?.also { amount ->
                    GreenDataLayout(title = stringResource(Res.string.id_amount), withPadding = false) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 70.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = amount,
                                style = displaySmall.copy(fontWeight = FontWeight.Medium),
                                color = whiteHigh,
                                textAlign = TextAlign.Center,
                            )
                            (invoiceAmountFiat ?: transactionConfirmation?.amountFiat)?.also {
                                Text(
                                    text = "≈ $it",
                                    style = bodyMedium,
                                    color = whiteMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = note.isNotBlank() && !isSwap) {
                    GreenDataLayout(
                        title = stringResource(Res.string.id_note),
                        withPadding = false,
                    ) {
                        Text(
                            text = note,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }
                }
            }

            transactionConfirmation?.takeIf { it.isSwap }?.also { look ->
                SwapTotalsSection(
                    look = look,
                    // For a reverse swap the paid invoice is the total spent (fees are inside it).
                    totalSpent = invoiceAmount ?: look.amount
                ) {
                    viewModel.postEvent(SendLightningConfirmViewModel.LocalEvents.ClickTotalFees)
                }
            }

            GreenConfirmButton(viewModel = viewModel) {
                viewModel.postEvent(CreateTransactionViewModelAbstract.LocalEvents.SignTransaction())
            }
        }
    }
}

@Composable
private fun LightningAssetRowContent() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.bitcoin_lightning),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(Res.string.id_lightning_bitcoin),
            style = bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = whiteHigh,
        )
    }
}

@Composable
private fun SwapTotalsSection(look: TransactionConfirmation, totalSpent: String?, onTotalFeesClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.id_total_fees),
                style = labelLarge,
                color = whiteMedium,
            )
            IconButton(onClick = onTotalFeesClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.info),
                    contentDescription = null,
                    tint = whiteMedium,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = look.totalFees ?: "", style = labelLarge, color = whiteHigh)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.id_total_spent),
                style = labelLarge,
                color = whiteMedium,
                modifier = Modifier.weight(1f),
            )
            Text(text = totalSpent ?: "", style = labelLarge, color = whiteHigh)
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.id_total_to_receive),
                style = titleSmall,
                color = whiteHigh,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(text = look.recipientReceives ?: "", style = titleSmall, color = whiteHigh)
                look.recipientReceivesFiat?.also {
                    Text(text = it, style = labelLarge, color = whiteMedium)
                }
            }
        }
    }
}

@Composable
@Preview
fun SendLightningConfirmScreenPreview() {
    GreenPreview {
        SendLightningConfirmScreen(viewModel = SendLightningConfirmViewModelPreview.preview())
    }
}
