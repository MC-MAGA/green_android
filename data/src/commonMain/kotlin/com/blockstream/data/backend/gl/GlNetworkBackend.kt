package com.blockstream.data.backend.gl

import com.blockstream.data.LN_BTC_POLICY_ASSET
import com.blockstream.data.backend.AccountBackend
import com.blockstream.data.backend.NetworkBackend
import com.blockstream.data.backend.NetworkEvent
import com.blockstream.data.data.ExceptionWithSupportData
import com.blockstream.data.data.SupportData
import com.blockstream.data.extensions.logException
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.HardwareWalletResolver
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Addressee
import com.blockstream.data.gdk.data.Block
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.ProcessedTransactionDetails
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.ConnectionParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.lightning.ConnectStatus
import com.blockstream.data.lightning.GreenlightMnemonicAndCredentials
import com.blockstream.data.lightning.LightningEvent
import com.blockstream.data.lightning.LightningInputType
import com.blockstream.data.lightning.LightningSdk
import com.blockstream.data.lightning.LnUrlPayOutcome
import com.blockstream.data.lightning.expireIn
import com.blockstream.data.lightning.fromInvoice
import com.blockstream.data.lightning.fromLnUrlPay
import com.blockstream.data.lightning.isExpired
import com.blockstream.data.lightning.maxPayableSatoshi
import com.blockstream.data.lightning.maxSendableSatoshi
import com.blockstream.data.lightning.minSendableSatoshi
import com.blockstream.data.lightning.sendableSatoshi
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.data.managers.NetworkAssetManager
import com.blockstream.data.utils.toAmountLook
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

class GlNetworkBackend(
    val sdk: LightningSdk,
    override val network: Network,
    val assetsProvider: AssetsProvider,
    val networkAssetManager: NetworkAssetManager
) : NetworkBackend {

    val account by lazy {
        Account(
            gdkName = "Lightning",
            pointer = 0,
            type = AccountType.LIGHTNING
        ).also {
            runBlocking { it.setup(networkAssetManager, assetsProvider, network) }
        }
    }

    private val accountBackend = GlAccountBackend(sdk, account)
    private val scope = CoroutineScope(Dispatchers.IO)

    override var isConnected: Boolean = false
        private set

    override var isLoggedIn: Boolean = false
        private set

    final override val blockStateFlow: StateFlow<Block>
        field = MutableStateFlow(Block())

    final override val networkEventsFlow: SharedFlow<NetworkEvent>
        field = MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    final override val accounts: StateFlow<List<Account>>
        field = MutableStateFlow<List<Account>>(listOf(account))

    override suspend fun connect(params: ConnectionParams) {
        // no op
    }

    // Can emit the same value multiple times
    val eventSharedFlow = sdk.eventSharedFlow.distinctUntilChanged()

    init {
        eventSharedFlow.onEach { event ->
            when (event) {
                is LightningEvent.NewBlock -> {
                    blockStateFlow.value = Block(height = event.block.toLong())
                    networkEventsFlow.emit(NetworkEvent.Block(height = event.block.toLong()))
                }

                is LightningEvent.InvoicePaid -> {
                    networkEventsFlow.emit(NetworkEvent.Transaction(accountPointers = listOf(account.pointer)))
                }

                else -> {

                }
            }
        }.launchIn(scope)
    }

    override fun accountBackend(account: Account): AccountBackend = accountBackend

    override suspend fun isPolicyAsset(assetId: String?): Boolean = assetId == LN_BTC_POLICY_ASSET

    suspend fun connect(
        mnemonicAndCredentials: GreenlightMnemonicAndCredentials,
        parentXpubHashId: String?,
        isRestore: Boolean = false,
        quickResponse: Boolean = false
    ): ConnectStatus {
        return sdk.connect(
            mnemonicAndCredentials = mnemonicAndCredentials,
            parentXpubHashId = parentXpubHashId,
            isRestore = isRestore,
            quickResponse = quickResponse,
        ).also {
            if (it == ConnectStatus.Connect) {
                isConnected = true
                isLoggedIn = true
            }
        }
    }

    override suspend fun isAddressValid(address: String): Boolean {
        return try {
            sdk.parseBoltOrLNUrlAndCache(address) != null
        } catch (_: Exception) {
            null
        } ?: false
    }

    suspend fun addressType(input: String): LightningInputType? = sdk.parseBoltOrLNUrlAndCache(input)

    override suspend fun getAccounts(refresh: Boolean) = listOf(account).also { accounts.value = it }

    override suspend fun getAccount(account: Account): Account = account

    override suspend fun disconnect() {
        scope.cancel()
        sdk.disconnect()

        accounts.value = emptyList()

        isConnected = false
        isLoggedIn = false
    }

    override suspend fun createAccount(
        params: SubAccountParams,
        hardwareWalletResolver: HardwareWalletResolver?
    ): Account {
        throw Exception("Can't create a Lightning account")
    }

    override suspend fun broadcastTransaction(broadcastTransaction: BroadcastTransactionParams): ProcessedTransactionDetails {
        throw Exception("Use sendLightningTransaction")
    }

    suspend fun sendLightningTransaction(params: CreateTransaction, comment: String?): ProcessedTransactionDetails {
        val invoiceOrLnUrl = params.addressees.first().address
        val satoshi = params.addressees.first().satoshi?.absoluteValue ?: 0L
        val baselineLightningSatoshi = accountBackend.assets.value.policyAsset

        return when (val inputType = sdk.parseBoltOrLNUrlAndCache(invoiceOrLnUrl)) {
            is LightningInputType.Bolt11 -> {
                // Check for expiration
                if (inputType.invoice.isExpired()) {
                    throw Exception("id_invoice_expired")
                }

                logger.d { "Sending invoice ${inputType.invoice.bolt11}" }

                try {
                    val response = sdk.sendPayment(
                        invoice = inputType.invoice,
                        satoshi = satoshi.takeIf { inputType.invoice.amountSatoshi == null }
                    )

                    refreshLightningUntilBalanceSettles(baselineLightningSatoshi)

                    ProcessedTransactionDetails(paymentId = response.paymentId)
                } catch (e: Exception) {
                    throw ExceptionWithSupportData(
                        throwable = e,
                        supportData = SupportData.create(
                            throwable = e,
                            paymentHash = inputType.invoice.paymentHash,
                            network = network,
                            supportId = sdk.nodeInfoStateFlow.value.id
                        )
                    )
                }
            }

            is LightningInputType.LnUrlPay -> {
                when (val result = sdk.payLnUrl(
                    requestData = inputType.data,
                    amount = satoshi,
                    comment = comment ?: ""
                )) {
                    is LnUrlPayOutcome.Success -> {
                        refreshLightningUntilBalanceSettles(baselineLightningSatoshi)
                        ProcessedTransactionDetails.create(result)
                    }

                    is LnUrlPayOutcome.Error -> {
                        throw Exception(result.reason)
                    }

                    is LnUrlPayOutcome.PayError -> {
                        val exception = Exception(result.reason)
                        throw ExceptionWithSupportData(
                            throwable = exception,
                            supportData = SupportData.create(
                                throwable = exception,
                                paymentHash = result.paymentHash,
                                network = network,
                                supportId = sdk.nodeInfoStateFlow.value.id
                            )
                        )
                    }
                }
            }

            else -> {
                throw Exception("id_invalid")
            }
        }.also {
            networkEventsFlow.emit(NetworkEvent.Transaction(listOf(account.pointer)))
        }
    }

    private fun refreshLightningUntilBalanceSettles(
        baselineSatoshi: Long,
        attempts: Int = 20,
        intervalMs: Long = 2_000L
    ) {
        scope.launch(context = logException()) {
            repeat(attempts) {
                delay(intervalMs.milliseconds)
                if (!isConnected) return@launch
                accountBackend.getBalance()
                if (accountBackend.assets.value.policyAsset != baselineSatoshi) return@launch
            }
        }
    }

    override suspend fun refreshAssets(params: AssetsParams) {}

    override suspend fun getAssets(params: GetAssetsParams): LiquidAssets? = null

    companion object : Loggable()
}