package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.check_circle
import blockstream_green.common.generated.resources.id_2fa_expired
import blockstream_green.common.generated.resources.id_all
import blockstream_green.common.generated.resources.id_all_available_coins
import blockstream_green.common.generated.resources.id_amount_below_the_dust_threshold_s
import blockstream_green.common.generated.resources.id_dust
import blockstream_green.common.generated.resources.id_filters
import blockstream_green.common.generated.resources.id_timelock_passed_spend_to_refresh_protection
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.models.send.CoinFilter
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.domain.send.GetSpendableUtxosUseCase
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoinFilterBottomSheet(
    selectedFilter: CoinFilter,
    availableFilters: List<CoinFilter>,
    onDismissRequest: () -> Unit
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_filters),
        withHorizontalPadding = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            val filters = listOf(CoinFilter.ALL) + availableFilters
            filters.forEachIndexed { index, filter ->
                CoinFilterRow(
                    filter = filter,
                    isSelected = filter == selectedFilter,
                    onClick = {
                        NavigateDestinations.CoinFilters.setResult(filter)
                        onDismissRequest()
                    }
                )

                if (index < filters.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CoinFilterRow(
    filter: CoinFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        GreenRow(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = filter.title(),
                    style = titleSmall,
                    color = if (isSelected) green else whiteHigh
                )

                filter.description()?.also {
                    Text(
                        text = it,
                        style = bodySmall,
                        color = whiteLow,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.check_circle),
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun CoinFilter.title(): String {
    return when (this) {
        CoinFilter.ALL -> stringResource(Res.string.id_all)
        CoinFilter.DUST -> stringResource(Res.string.id_dust)
        CoinFilter.EXPIRED -> stringResource(Res.string.id_2fa_expired)
    }
}

@Composable
private fun CoinFilter.description(): String? {
    return when (this) {
        CoinFilter.ALL -> stringResource(Res.string.id_all_available_coins)
        CoinFilter.DUST -> stringResource(
            Res.string.id_amount_below_the_dust_threshold_s,
            GetSpendableUtxosUseCase.DUST_COIN_THRESHOLD_SATS
        )
        CoinFilter.EXPIRED -> stringResource(Res.string.id_timelock_passed_spend_to_refresh_protection)
    }
}
