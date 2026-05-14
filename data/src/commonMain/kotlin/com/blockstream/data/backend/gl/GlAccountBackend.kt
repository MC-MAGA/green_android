package com.blockstream.data.backend.gl

import com.blockstream.data.LN_BTC_POLICY_ASSET
import com.blockstream.data.backend.AbstractAccountBackend
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Address
import com.blockstream.data.gdk.data.Addressee
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.Transactions
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.greenlight.GlAddress
import com.blockstream.data.lightning.LightningInputType
import com.blockstream.data.lightning.LightningReceivePayment
import com.blockstream.data.lightning.LightningSdk
import com.blockstream.data.lightning.expireIn
import com.blockstream.data.lightning.fromInvoice
import com.blockstream.data.lightning.fromLnUrlPay
import com.blockstream.data.lightning.fromPayment
import com.blockstream.data.lightning.isExpired
import com.blockstream.data.lightning.maxPayableSatoshi
import com.blockstream.data.lightning.maxSendableSatoshi
import com.blockstream.data.lightning.minSendableSatoshi
import com.blockstream.data.lightning.sendableSatoshi
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GlAccountBackend constructor(private val sdk: LightningSdk, private val account: Account) : AbstractAccountBackend() {

    override suspend fun getReceiveAddress(): Address = GlAddress(address = sdk.receiveOnchain().bech32)

    override suspend fun getBalance(
        confirmations: Int
    ): Map<String, Long> {
        return mapOf(LN_BTC_POLICY_ASSET to sdk.balanceOnChannel()).also {
            setAssets(Assets(it))
        }
    }

    override suspend fun getTransactions(
        params: TransactionParams
    ): Transactions {
        return sdk.getTransactions()?.map {
            Transaction.fromPayment(payment = it, account = account)
        }.let {
            Transactions(it ?: listOf())
        }
    }

    override suspend fun getOutputDescriptors(): String = sdk.nodeInfoStateFlow.value.id

    suspend fun createInvoice(satoshi: Long, description: String): LightningReceivePayment = sdk.createInvoice(satoshi, description)

    override suspend fun createTransaction(
        params: CreateTransactionParams
    ) : CreateTransaction {
        val address = params.addresseesAsParams?.firstOrNull()?.address ?: ""
        val userInputSatoshi = params.addresseesAsParams?.firstOrNull()?.satoshi

        return when (val lightningInputType = sdk.parseBoltOrLNUrlAndCache(address)) {
            is LightningInputType.Bolt11 -> {
                val invoice = lightningInputType.invoice

                logger.d { "Expire in ${invoice.expireIn()}" }

                var sendableSatoshi = invoice.sendableSatoshi(userInputSatoshi)

                var error = generateLightningError(satoshi = sendableSatoshi)

                // Check expiration
                if (invoice.isExpired()) {
                    error = "id_invoice_expired"
                }

                if (sendableSatoshi == null || sendableSatoshi == 0L) {
                    error = "id_invalid_amount"
                }

                // Make it not null
                sendableSatoshi = sendableSatoshi ?: 0L

                CreateTransaction(
                    addressees = listOf(Addressee.fromInvoice(invoice, sendableSatoshi)),
                    satoshi = mapOf(LN_BTC_POLICY_ASSET to (sendableSatoshi)),
                    outputs = listOf(Output.fromInvoice(invoice, sendableSatoshi)),
                    memo = invoice.description,
                    isLightning = true,
                    error = error
                )
            }

            is LightningInputType.LnUrlPay -> {

                val requestData = lightningInputType.data

                var sendableSatoshi = requestData.sendableSatoshi(userInputSatoshi)

                val error = generateLightningError(
                    satoshi = sendableSatoshi,
                    min = requestData.minSendableSatoshi(),
                    max = requestData.maxSendableSatoshi()
                )

                // Make it not null
                sendableSatoshi = sendableSatoshi ?: 0L

                CreateTransaction(
                    addressees = listOf(Addressee.fromLnUrlPay(requestData, address, sendableSatoshi)),
                    outputs = listOf(Output.fromLnUrlPay(requestData, address, sendableSatoshi)),
                    satoshi = mapOf(LN_BTC_POLICY_ASSET to sendableSatoshi),
                    isLightning = true,
                    isLightningDescriptionEditable = true,
                    error = error
                )
            }

            else -> {
                CreateTransaction(error = "id_invalid_address", isLightning = true)
            }
        }
    }

    override suspend fun signTransaction(createTransaction: CreateTransaction): CreateTransaction = createTransaction

    override suspend fun getUnspentOutputs(isBump: Boolean, isExpired: Boolean, expiredAt: Long?): UnspentOutputs {
        return UnspentOutputs()
    }

    private fun generateLightningError(
        satoshi: Long?,
        min: Long? = null,
        max: Long? = null
    ): String? {
        val balance = sdk.nodeInfoStateFlow.value.maxPayableSatoshi()

        return if (satoshi == null || satoshi < 0L) {
            "id_invalid_amount"
        } else if (min != null && satoshi < min) {
            "id_amount_must_be_at_least_s|$${min} sats"
        } else if (satoshi > balance) {
            "id_insufficient_funds"
        } else if (max != null && satoshi > max) {
            "id_amount_must_be_at_most_s|${max} sats"
        } else {
            null
        }
    }

    companion object: Loggable()
}