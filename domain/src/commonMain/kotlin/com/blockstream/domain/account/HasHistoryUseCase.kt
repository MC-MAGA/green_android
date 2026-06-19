package com.blockstream.domain.account

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HasHistoryUseCase : Loggable() {

    private val accountsWithHistory = mutableSetOf<String>()
    private val mutex = Mutex()

    suspend operator fun invoke(
        session: GdkSession,
        wallet: GreenWallet,
        account: Account
    ): Boolean = withContext(Dispatchers.Default) {
        cachedHasHistory(key = "${wallet.id}:${account.id}") {
            account.bip44Discovered == true || account.isFunded(session) || session.accountBackend(account)
                .getTransactions().transactions.isNotEmpty()
        }
    }

    // Account history is monotonic: once an account has history it always does.
    // A positive result is therefore cached permanently and never recomputed; a
    // negative result is left uncached because history can appear later.
    internal suspend fun cachedHasHistory(key: String, compute: suspend () -> Boolean): Boolean {
        mutex.withLock {
            if (accountsWithHistory.contains(key)) {
                return true
            }
        }

        return compute().also { hasHistory ->
            if (hasHistory) {
                mutex.withLock { accountsWithHistory.add(key) }
            }
        }
    }
}