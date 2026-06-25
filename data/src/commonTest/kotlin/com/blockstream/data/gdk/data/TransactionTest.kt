package com.blockstream.data.gdk.data

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [Transaction] display projections (utxoViews/assets), covering how
 * per-asset amounts are derived from the wallet-relative satoshi map and the
 * unblinded inputs/outputs provided by the account backend.
 */
class TransactionTest {

    private val lbtc = "6f0279e9ed041c3d710a9f57d0c02928416460c4b722ae3457a11eec381c526d"
    private val asset = "ce091c998b83c78bb71a632313ba3760f1763d9cfcffae02258ffa9865a37bd2"

    private val amp2Network = Network(
        network = Network.LiquidAmp2Mainnet,
        name = "AMP2",
        isMainnet = true,
        isLiquid = true,
        isDevelopment = false,
        policyAsset = lbtc
    )

    private val amp2Account = Account(
        networkInjected = amp2Network,
        gdkName = "AMP2 Account",
        pointer = 0,
        type = AccountType.AMP2_ACCOUNT
    )

    @Test
    fun amp2_outgoing_send_displays_sent_amount_not_change() {
        // LWK can only unblind wallet-owned outputs, so for an external send the
        // recipient output is absent and only the change output is available.
        val changeOutput = InputOutput(
            address = "lq1qqchange",
            assetId = lbtc,
            satoshi = 2340,
            isOutput = true,
            isInternal = true,
            isChange = true,
            isRelevant = true
        )

        val transaction = Transaction(
            accountInjected = amp2Account,
            blockHeight = 100,
            createdAtTs = 0,
            inputs = listOf(),
            outputs = listOf(changeOutput),
            fee = 50,
            feeRate = 100,
            memo = "",
            spvVerified = "disabled",
            txHash = "00".repeat(32),
            type = "outgoing",
            satoshi = mapOf(lbtc to -50L, asset to -1000L)
        )

        assertEquals(1, transaction.utxoViews.size)

        val view = transaction.utxoViews.first()
        assertEquals(asset, view.assetId)
        assertEquals(-1000L, view.satoshi)
    }
}
