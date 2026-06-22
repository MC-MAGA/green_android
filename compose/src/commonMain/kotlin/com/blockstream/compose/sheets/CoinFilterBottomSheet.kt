package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.check_circle
import blockstream_green.common.generated.resources.id_2fa_expired
import blockstream_green.common.generated.resources.id_all
import blockstream_green.common.generated.resources.id_csv
import blockstream_green.common.generated.resources.id_dust
import blockstream_green.common.generated.resources.id_filters
import blockstream_green.common.generated.resources.id_not_confidential
import blockstream_green.common.generated.resources.id_p2sh
import blockstream_green.common.generated.resources.id_p2wsh
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.models.send.CoinFilter
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
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
            availableFilters.forEach { filter ->
                CoinFilterRow(
                    filter = filter,
                    isSelected = filter == selectedFilter,
                    onClick = {
                        NavigateDestinations.CoinFilters.setResult(filter)
                        onDismissRequest()
                    }
                )

                HorizontalDivider()
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
            Text(
                text = filter.title(),
                style = titleSmall,
                color = if (isSelected) green else whiteHigh,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
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
fun CoinFilter.title(): String {
    return when (this) {
        CoinFilter.ALL -> stringResource(Res.string.id_all)
        CoinFilter.CSV -> stringResource(Res.string.id_csv)
        CoinFilter.P2WSH -> stringResource(Res.string.id_p2wsh)
        CoinFilter.P2SH -> stringResource(Res.string.id_p2sh)
        CoinFilter.DUST -> stringResource(Res.string.id_dust)
        CoinFilter.NOT_CONFIDENTIAL -> stringResource(Res.string.id_not_confidential)
        CoinFilter.EXPIRED -> stringResource(Res.string.id_2fa_expired)
    }
}
