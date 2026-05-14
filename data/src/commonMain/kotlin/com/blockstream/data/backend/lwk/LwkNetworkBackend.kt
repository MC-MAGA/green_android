package com.blockstream.data.backend.lwk

import com.blockstream.data.backend.AccountBackend
import com.blockstream.data.backend.gdk.GdkNetworkBackend
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.LoginData
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.device.GdkHardwareWallet
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.DeviceParams
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.data.utils.toHex
import com.blockstream.jade.Loggable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lwk.Address
import lwk.AssetId
import lwk.ForeignStore
import lwk.ForeignStoreLink
import lwk.LwkException
import lwk.Mnemonic
import lwk.Signer
import lwk.Transaction
import lwk.Wollet
import lwk.WolletBuilder
import lwk.WolletDescriptor
import lwk.Network as LwkNetwork

// WIP
class LwkNetworkBackend constructor(
    private val dataDir: String,
    gdk: Gdk,
    network: Network,
    gdkHwWallet: GdkHardwareWallet?,
    networkAssetManager: NetworkAssetManager,
) : GdkNetworkBackend(
    gdk = gdk,
    network = network,
    gdkHwWallet = gdkHwWallet,
    networkAssetManager = networkAssetManager,
) {
    private var signer: Signer? = null
    val lwkNetwork = if (network.isTestnet) LwkNetwork.testnet() else LwkNetwork.mainnet()

    private val client = LwkNetworkClient(isTestnet = network.isTestnet)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tipJob: Job? = null

    override suspend fun login(
        deviceParams: DeviceParams,
        loginCredentialsParams: LoginCredentialsParams,
        hardwareWalletResolver: HardwareWalletResolver?
    ): LoginData {
        signer?.let { existing ->
            syncSwallowing()
            return LoginData(xpubHashId = existing.fingerprint(), networkHashId = network.id)
        }

        gdk.loginUser(
            session = gaSession,
            deviceParams = deviceParams,
            loginCredentialsParams = loginCredentialsParams
        ).resolve(hardwareWalletResolver = hardwareWalletResolver)

        val signer = Signer(Mnemonic(loginCredentialsParams.mnemonic!!), lwkNetwork)

        this.signer = signer

        isLoggedIn = true
        logger.d { "LWK login complete for ${network.id}" }

        blockHeaderPolling()

        syncSwallowing()

        return LoginData(xpubHashId = signer.fingerprint(), networkHashId = network.id)
    }

    private fun blockHeaderPolling() {
        if (tipJob != null) return
        tipJob = scope.launch {
            while (coroutineContext.isActive) {
                try {
                    val header = client.tip()
                    updateBlock(
                        Block(
                            hash = header.blockHash(),
                            height = header.height().toLong(),
                            timestamp = header.time().toLong()
                        ).also {
                            logger.d { "Update block $it" }
                        }
                    )
                    scanBlockchain()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.w { "LWK tip poll failed: ${e.message}" }
                }
                delay(TIP_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun scanBlockchain() {
        accountBackends.mapNotNull { (id, backend) ->
            if(backend is LwkAccountBackend) {
                client.fullScanToIndex(wollet = backend.wollet)?.also { update ->
                    logger.d { "Update for $id : ${update.serialize().toHex()}" }
                    backend.wollet.applyUpdate(update)
                }
            }
        }
    }

    private suspend fun syncSwallowing() {
        try {
            sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w { "LWK sync on login failed: ${e.message}" }
        }
    }

    override fun createAccountBackend(account: Account): AccountBackend {
        if(account.type != AccountType.BIP84_SEGWIT || account.bip32Pointer > 0){
            return super.createAccountBackend(account)
        }

        val lwkNetwork = if (network.isTestnet) LwkNetwork.testnet() else LwkNetwork.mainnet()
        // TODO fix the suspend getCredentials
        val signer = Signer(Mnemonic(runBlocking { getCredentials().mnemonic!! }), lwkNetwork)
        val descriptor = signer.wpkhSlip77Descriptor()

        val datadir = "$dataDir/lwk/${network.id}/0"

        val wollet = WolletBuilder(lwkNetwork, descriptor).also {
            it.withLegacyFsStore(datadir)
        }.build()

//        val store = mutableMapOf<String, ByteArray>()

//        val wollet = Wollet.withCustomStore(network = lwkNetwork, descriptor = descriptor, store = ForeignStoreLink(object : ForeignStore{
//            override fun get(key: String): ByteArray? {
//                return store[key]
//            }
//
//            override fun put(key: String, value: ByteArray) {
//                store[key] = value
//            }
//
//            override fun remove(key: String) {
//                store.remove(key)
//            }
//        }))

        return LwkAccountBackend(
            networkBackend = this,
            wollet = wollet,
            signer = signer,
            account = account,
        )
    }

    override suspend fun broadcastTransaction(broadcastTransaction: BroadcastTransactionParams): ProcessedTransactionDetails {
        requireNotNull(broadcastTransaction.transaction)

        val txId = client.broadcast(Transaction.fromString(broadcastTransaction.transaction))

        return ProcessedTransactionDetails(
            signedTransaction = broadcastTransaction.transaction,
            txHash = txId.toString()
        )
    }

    override suspend fun blindTransaction(createTransaction: CreateTransaction): CreateTransaction = createTransaction

    override suspend fun transformAccounts(accounts: List<Account>): List<Account> =
        accounts.map {
            it.copy(gdkName = "${it.name} [${if(accountBackend(it) is LwkAccountBackend) "LWK" else "GDK"}]")
        }

    suspend fun sync() {
        if (!isLoggedIn) return

    }

    override suspend fun disconnect() {
        super.disconnect()

        scope.cancel()
        tipJob = null

        signer?.close()
        signer = null
    }

    companion object : Loggable() {

        const val TIP_POLL_INTERVAL_MS = 60_000L
    }
}
