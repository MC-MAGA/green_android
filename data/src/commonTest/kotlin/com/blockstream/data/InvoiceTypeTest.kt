package com.blockstream.data

import com.blockstream.data.lightning.LightningInputType
import com.blockstream.data.lightning.LightningInvoice
import com.blockstream.data.lightning.LnUrlAuthData
import com.blockstream.data.lightning.LnUrlPayData
import com.blockstream.data.lightning.LnUrlWithdrawData
import com.blockstream.data.lightning.invoiceType
import com.blockstream.data.lwk.Bolt12AmountMode
import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.data.lwk.invoiceType
import com.blockstream.glsdk.LnUrlPayRequestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvoiceTypeTest {

    @Test
    fun test_lwkInstructionMapping() {
        assertEquals(
            InvoiceType.BOLT11,
            PaymentInstruction.Bolt11(invoice = "lnbc1", amountSats = 1_000).invoiceType()
        )
        assertEquals(
            InvoiceType.BOLT12,
            PaymentInstruction.Bolt12(
                offer = "lno1",
                amountMode = Bolt12AmountMode.WITH_AMOUNT,
                amountSats = 1_000
            ).invoiceType()
        )
        assertEquals(
            InvoiceType.LNURL,
            PaymentInstruction.LnUrl(
                raw = "lnurl1",
                minSats = 1,
                maxSats = 1_000,
                description = null,
                metadata = "[]"
            ).invoiceType()
        )
    }

    @Test
    fun test_greenlightInputMapping() {
        assertEquals(InvoiceType.BOLT11, LightningInputType.Bolt11(invoice = bolt11()).invoiceType())
        assertEquals(InvoiceType.LNURL, LightningInputType.LnUrlPay(data = lnUrlPay()).invoiceType())
    }

    // Withdraw and auth aren't outgoing payments, so labelling them would tag a receive or a login
    // as a send. Null lets the caller fall through to lwk instead.
    @Test
    fun test_greenlightNonPaymentInputsAreUnclassified() {
        assertNull(LightningInputType.LnUrlWithdraw(data = lnUrlWithdraw()).invoiceType())
        assertNull(LightningInputType.LnUrlAuth(data = lnUrlAuth()).invoiceType())
    }

    // These strings are the countly wire contract, not an implementation detail: changing one
    // silently splits a dashboard series in two.
    @Test
    fun test_segmentationValues() {
        assertEquals("bolt11", InvoiceType.BOLT11.toString())
        assertEquals("bolt12", InvoiceType.BOLT12.toString())
        assertEquals("lnurl", InvoiceType.LNURL.toString())
        assertEquals("unknown", InvoiceType.UNKNOWN.toString())
    }

    private fun bolt11() = LightningInvoice(
        bolt11 = "lnbc1",
        amountSatoshi = 1_000,
        timestamp = 0,
        expiry = 3_600,
        paymentHash = "hash",
        description = null
    )

    private fun lnUrlPay() = LnUrlPayData(
        domain = "example.com",
        minSendable = 1_000uL,
        maxSendable = 1_000_000uL,
        metadataStr = "[]",
        raw = LnUrlPayRequestData(
            callback = "https://example.com/lnurlp",
            minSendable = 1_000uL,
            maxSendable = 1_000_000uL,
            metadata = "[]",
            commentAllowed = 0uL,
            description = "",
            lnurl = "lnurl1"
        )
    )

    private fun lnUrlWithdraw() = LnUrlWithdrawData(
        callback = "https://example.com/lnurlw",
        k1 = "k1",
        defaultDescription = "",
        minWithdrawable = 1_000uL,
        maxWithdrawable = 1_000_000uL
    )

    private fun lnUrlAuth() = LnUrlAuthData(
        k1 = "k1",
        domain = "example.com",
        url = "https://example.com/lnurla"
    )
}
