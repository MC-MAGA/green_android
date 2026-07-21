package com.blockstream.domain.swap

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession

class IsSwapAvailableUseCase {
    operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        asset: EnrichedAsset? = null
    ): Boolean {

        if (session.deviceModel?.supportsLightningMnemonicDerivation == false) return false

        if (wallet.isWatchOnly && !wallet.isHardware) return false

        if (!wallet.isMainnet || wallet.isEphemeral) return false

        // Only policy assets (BTC, L-BTC) and Lightning (when actually enabled and connected) can be swapped.
        if (asset != null && !asset.isPolicyAsset(session) && !(asset.isLightning && session.hasLightning && session.lightningSdkOrNull != null)) return false

        // Also require at least 2 distinct swappable networks (Bitcoin, Liquid, Lightning) before showing the swap button.
        val swappableNetworks = session.accountAsset.value
            .filter { it.isSwappableAsset(session) }
            .map { it.account.network.canonicalNetworkId }
            .distinct()

        return swappableNetworks.size >= 2
    }
}
