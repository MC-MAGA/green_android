package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.amp_asset
import blockstream_green.common.generated.resources.id_create_new_account
import blockstream_green.common.generated.resources.id_any_amp_asset
import blockstream_green.common.generated.resources.id_any_amp_legacy_asset
import blockstream_green.common.generated.resources.id_any_liquid_asset
import blockstream_green.common.generated.resources.id_s_is_a_liquid_asset_you_can
import blockstream_green.common.generated.resources.id_s_is_a_liquid_asset_you_need_a
import blockstream_green.common.generated.resources.id_s_is_an_amp_asset_you_can
import blockstream_green.common.generated.resources.id_s_is_an_amp_asset_you_need_an
import blockstream_green.common.generated.resources.id_s_is_an_amp_legacy_asset_you_can
import blockstream_green.common.generated.resources.id_s_is_an_amp_legacy_asset_you_need_an
import blockstream_green.common.generated.resources.id_you_need_a_liquid_account_in
import blockstream_green.common.generated.resources.id_you_need_an_amp_account_in
import blockstream_green.common.generated.resources.id_you_need_an_amp_legacy_account_in
import blockstream_green.common.generated.resources.liquid_asset
import blockstream_green.common.generated.resources.shield_warning
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.extensions.nameStringHolder
import com.blockstream.compose.extensions.policyAndType
import com.blockstream.compose.extensions.policyIcon
import com.blockstream.compose.extensions.previewAccount
import com.blockstream.compose.extensions.previewEnrichedAsset
import com.blockstream.compose.theme.GreenChromePreview
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.green20
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.extensions.getAssetNameOrNull
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountAsset
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GreenAssetAccounts(
    modifier: Modifier = Modifier,
    asset: EnrichedAsset,
    accounts: List<Account> = listOf(),
    isExpanded: Boolean = false,
    session: GdkSession? = null,
    onAccountClick: ((AccountAsset) -> Unit) = { },
    onExpandClick: (EnrichedAsset) -> Unit = { },
    onCreateNewAccount: (EnrichedAsset) -> Unit = {}
) {

    val warningMessage = stringResource(
        when {
            asset.isAnyAsset && !asset.isAmp2OrLegacy -> {
                Res.string.id_you_need_a_liquid_account_in
            }

            asset.isAnyAsset && asset.isAmp -> {
                Res.string.id_you_need_an_amp_account_in
            }

            asset.isAnyAsset && asset.isAmpLegacy -> {
                Res.string.id_you_need_an_amp_legacy_account_in
            }

            accounts.isNotEmpty() && asset.isAmp -> {
                Res.string.id_s_is_an_amp_asset_you_can
            }

            accounts.isNotEmpty() && asset.isAmpLegacy -> {
                Res.string.id_s_is_an_amp_legacy_asset_you_can
            }

            accounts.isNotEmpty() && !asset.isAmp2OrLegacy -> {
                Res.string.id_s_is_a_liquid_asset_you_can
            }

            accounts.isEmpty() && asset.isAmp -> {
                Res.string.id_s_is_an_amp_asset_you_need_an
            }

            accounts.isEmpty() && asset.isAmpLegacy -> {
                Res.string.id_s_is_an_amp_legacy_asset_you_need_an
            }

            else -> {
                Res.string.id_s_is_a_liquid_asset_you_need_a
            }
        }, asset.assetId.getAssetNameOrNull(session) ?: asset.assetId.slice(
            0 until asset.assetId.length.coerceAtMost(5)
        )
    )

    GreenCard(
        modifier = modifier,
        padding = 0,
        border = if (isExpanded) BorderStroke(1.dp, green) else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column {
            Row(
                modifier = Modifier.clickable {
                    onExpandClick(asset)
                }.padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Image(
                        painter = when {
                            asset.isAnyAsset && asset.isAmp -> painterResource(Res.drawable.amp_asset)
                            asset.isAnyAsset && asset.isAmpLegacy -> painterResource(Res.drawable.amp_asset)
                            asset.isAnyAsset && !asset.isAmp2OrLegacy -> painterResource(Res.drawable.liquid_asset)
                            else -> asset.assetId.assetIcon(session = session)
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .padding(end = 7.dp)
                            .size(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .padding(end = 10.dp)
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (asset.isAnyAsset) {
                        val canCreateAmp = session?.isTestnet == true && session.liquidAmp2 != null
                        Text(
                            text = stringResource(
                                when {
                                    asset.isAnyAsset && asset.isAmp -> Res.string.id_any_amp_asset
                                    asset.isAnyAsset && asset.isAmpLegacy -> if (canCreateAmp) Res.string.id_any_amp_legacy_asset else Res.string.id_any_amp_asset
                                    else -> Res.string.id_any_liquid_asset
                                }
                            ),
                            style = titleSmall,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Asset
                                Text(
                                    text = asset.nameStringHolder(session).string(),
                                    style = titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = green)

                    if (session != null && !asset.assetId.isPolicyAsset(session)) {
                        GreenRow(
                            padding = 0, space = 8, modifier = Modifier.fillMaxWidth()
                                .background(green20)
                                .padding(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.shield_warning),
                                contentDescription = null,
                                tint = green
                            )
                            Text(warningMessage, style = labelMedium, color = green)
                        }

                        HorizontalDivider(color = green)
                    }

                    accounts.forEach {
                        GreenRow(
                            padding = 0,
                            space = 16,
                            modifier = Modifier.clickable {
                                onAccountClick(AccountAsset(asset = asset, account = it))
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                                Text(it.policyAndType(session), style = labelLarge)

                                Row {
                                    Icon(
                                        painter = painterResource(it.policyIcon()),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = whiteMedium
                                    )

                                    Text(
                                        it.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = labelMedium,
                                        color = whiteMedium
                                    )
                                }
                            }

                            CaretRight()
                        }

                        HorizontalDivider()
                    }

                    if (session?.isWatchOnlyValue != true && !asset.isLightning) {
                        Row(
                            modifier = Modifier.clickable {
                                onCreateNewAccount(asset)
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(Res.string.id_create_new_account),
                                style = labelLarge,
                                modifier = Modifier.weight(1f).padding(start = 6.dp)
                            )

                            CaretRight()
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun GreenAssetAccountsPreview() {
    GreenChromePreview {
        GreenColumn {
            var selected by remember {
                mutableStateOf(0)
            }
            GreenAssetAccounts(
                accounts = listOf(previewAccount(), previewAccount()),
                asset = previewEnrichedAsset(),
                isExpanded = selected == 0,
                onExpandClick = {
                    selected = 0
                }
            )
            GreenAssetAccounts(
                accounts = listOf(previewAccount(), previewAccount()),
                asset = previewEnrichedAsset(isLiquid = true),
                isExpanded = selected == 1,
                onExpandClick = {
                    selected = 1
                }
            )
            GreenAssetAccounts(
                accounts = listOf(previewAccount()),
                asset = previewEnrichedAsset(isLiquid = true),
                isExpanded = selected == 2,
                onExpandClick = {
                    selected = 2
                }
            )
            GreenAssetAccounts(
                asset = previewEnrichedAsset(true).copy(
                    name = "123456789098765432112345678987654321234567890",
                ),
                isExpanded = selected == 3,
                onExpandClick = {
                    selected = 3
                }
            )
            GreenAssetAccounts(
                asset = previewEnrichedAsset(true).copy(isAnyAsset = true),
                isExpanded = selected == 5,
                onExpandClick = {
                    selected = 5
                }
            )
            GreenAssetAccounts(
                asset = previewEnrichedAsset(true).copy(isAnyAsset = true, isAmp = true),
                isExpanded = selected == 5,
                onExpandClick = {
                    selected = 5
                }
            )
            GreenAssetAccounts(
                asset = previewEnrichedAsset(true).copy(isAnyAsset = true, isAmpLegacy = true),
                isExpanded = selected == 5,
                onExpandClick = {
                    selected = 5
                }
            )
        }
    }
}
