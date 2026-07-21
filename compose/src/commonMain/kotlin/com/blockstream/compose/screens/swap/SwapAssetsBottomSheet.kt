package com.blockstream.compose.screens.swap

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.data.gdk.data.AssetBalanceList

// Compact asset picker for the Swap screen: a short, wrap-content list without search.
// For long searchable lists use AssetsBottomSheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapAssetsBottomSheet(
    viewModel: GreenViewModel,
    assetBalance: AssetBalanceList,
    title: String? = null,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = title,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        ),
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(
            padding = 0,
            space = 4,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            assetBalance.list.forEach { assetBalance ->
                GreenAsset(
                    assetBalance = assetBalance,
                    session = viewModel.sessionOrNull,
                    onClick = {
                        NavigateDestinations.SwapAssets.setResult(assetBalance)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}
