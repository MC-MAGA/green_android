package com.blockstream.data.backend.lwk

import com.blockstream.data.backend.AbstractAccountBackend
import com.blockstream.data.comparators.ComparatorAssets
import com.blockstream.data.extensions.toSortedLinkedHashMap
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Address
import com.blockstream.data.gdk.data.Addressee
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.Transactions
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.lwk.LwkAddress
import com.blockstream.data.utils.toHex
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import lwk.AssetId
import lwk.Pset
import lwk.Recipient
import lwk.Signer
import lwk.Wollet
import lwk.Address as LwkLibAddress

class LwkAccountBackend constructor(
    val networkBackend: LwkNetworkBackend,
    val wollet: Wollet,
    val signer: Signer,
    val account: Account
) : AbstractAccountBackend() {
    private var nextAddressIndex: Int = 0

    override suspend fun getReceiveAddress(): Address {
        val firstUnusedIndex = wollet.address(null).index().toInt()

        // Hand out addresses past the wallet's first-unused index, but stay
        // within the BIP44 gap limit so we don't generate addresses the
        // wallet won't scan.
        if (nextAddressIndex < firstUnusedIndex || nextAddressIndex - firstUnusedIndex >= GAP_LIMIT) {
            nextAddressIndex = firstUnusedIndex
        }

        val address = wollet.address(nextAddressIndex.toUInt())
        nextAddressIndex++

        logger.d { "Address #${address.index()} ${address.address()}" }

        return LwkAddress(
            address = address.address().toString(),
            index = address.index().toLong()
        )
    }

    override suspend fun getBalance(confirmations: Int): Map<String, Long> {
        return wollet.balance().mapValues {
            it.value.toLong()
        }.also {
            logger.d { "Balance $it" }
            setAssets(Assets(it.toSortedLinkedHashMap(ComparatorAssets(networkBackend, networkBackend.networkAssetManager, networkBackend))))
        }
    }

    override suspend fun getTransactions(
        params: TransactionParams
    ): Transactions {
        val txs = wollet.transactions().map { walletTx ->
            Transaction(
                blockHeight = walletTx.height()?.toLong() ?: 0L,
                createdAtTs = (walletTx.timestamp()?.toLong() ?: 0L) * 1_000_000L,
                inputs = listOf(),
                outputs = listOf(),
                fee = walletTx.fee().toLong(),
                feeRate = 0L,
                memo = "",
                spvVerified = "disabled",
                txHash = walletTx.txid().toString(),
                type = walletTx.type(),
                satoshi = walletTx.balance(),
            ).also {
                it.accountInjected = account
                it.confirmationsMaxInjected = it.getConfirmationsMax(networkBackend.blockStateFlow.value.height)
            }
        }
        return Transactions(txs)
    }

    override suspend fun createTransaction(
        params: CreateTransactionParams
    ): CreateTransaction {
        val addressees = params.addresseesAsParams.orEmpty()
        check(addressees.size <= 1)

        val recipient = addressees.first()
        val recipientAsset = recipient.assetId ?: throw Exception("Recipient without assetId")

        val builder = networkBackend.lwkNetwork.txBuilder()

        params.feeRate?.let { builder.feeRate(it.toFloat()) }

        if (recipient.isGreedy) {
            throw Exception("isGreedy not yet supported")
        }else {
            builder.addRecipient(
                address = LwkLibAddress(recipient.address),
                satoshi = recipient.satoshi.toULong(),
                asset = recipientAsset,
            )
        }

        val pset = builder.finish(wollet)
        val balance = wollet.psetDetails(pset).balance()
        val tx = pset.extractTx()

        val outputs = balance.recipients().map {
            Output(
                address = it.address()?.toString(),
                assetId = it.asset(),
                satoshi = it.value()?.toLong() ?: 0L,
                isChange = false,
            )
        }

        return CreateTransaction(
            addressees = addressees.map {
                Addressee(
                    address = it.address,
                    assetId = it.assetId,
                    satoshi = it.satoshi,
                )
            },
            satoshi = balance.balances().mapValues {
                // Remove the lbtc fee from the amount
                if(it.key == networkBackend.network.policyAsset) it.value + balance.fee().toLong() else it.value
            },
            fee = balance.fee().toLong(),
            feeRate = params.feeRate,
            outputs = outputs,
            memo = params.memo,
            transaction = tx.toBytes().toHex(),
            txHash = tx.txid().toString(),
            pset = pset.toString()
        )
    }

    override suspend fun getUnspentOutputs(isBump: Boolean, isExpired: Boolean, expiredAt: Long?): UnspentOutputs {
        return UnspentOutputs()
    }

    override suspend fun getOutputDescriptors(): String = wollet.descriptor().toString()

    override suspend fun signTransaction(createTransaction: CreateTransaction): CreateTransaction {
        requireNotNull(createTransaction.pset)

        val signedTransaction = signer.sign(Pset(createTransaction.pset)).finalize()

        return createTransaction.copy(
            transaction = signedTransaction.toString()
        )
    }

    companion object: Loggable() {
        private const val GAP_LIMIT = 20
    }
}
