package com.blockstream.domain.receive

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Address

/**
 * Generates a new receive address for an account.
 *
 * Delegates to the account's backend (GDK, Greenlight or LWK) to produce a fresh
 * [Address], returning the network-specific implementation ([GdkAddress],
 * [GlAddress] or [LwkAddress]) behind the [Address] interface.
 */
class GetReceiveAddressUseCase {

    /**
     * Returns a new receive [Address] for the given [account].
     *
     * @param session the active GDK session used to reach the account backend.
     * @param account the account to generate an address for.
     */
    suspend operator fun invoke(session: GdkSession, account: Account): Address {
        return session.accountBackend(account).getReceiveAddress()
    }
}
