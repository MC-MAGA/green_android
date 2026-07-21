package com.blockstream.domain.swap

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.SwapDetails
import com.blockstream.domain.receive.GetReceiveAddressUseCase

/**
 * Orchestrates the creation of various swap types: Chain, Normal Submarine, or Reverse Submarine.
 *
 * This use case serves as a router that selects and delegates to the appropriate specialized
 * swap use case based on the combination of source (from) and destination (to) networks.
 */
class CreateSwapUseCase(
    private val createChainSwapUseCase: CreateChainSwapUseCase,
    private val createReverseSubmarineSwapUseCase: CreateReverseSubmarineSwapUseCase,
    private val createNormalSubmarineSwapUseCase: CreateNormalSubmarineSwapUseCase,
    private val getReceiveAddressUseCase: GetReceiveAddressUseCase
) {

    /**
     * Executes the swap creation by determining the swap type based on account properties.
     *
     * Routing Rules:
     * - **Chain Swap**: Used when both source and destination are on-chain (Bitcoin or Liquid).
     * - **Normal Submarine Swap**: Used when swapping from an on-chain account (Liquid or Bitcoin) to
     *   Lightning. An invoice is generated from the user's own Lightning account and then funded via
     *   an on-chain transaction.
     * - **Reverse Submarine Swap**: Used when swapping from Lightning to an on-chain account (Liquid or
     *   Bitcoin). A BOLT11 invoice is created for the user to pay from their Lightning balance, claiming
     *   to their own on-chain address.
     *
     * @param wallet the active [GreenWallet] identifying the user's wallet
     * @param session the current [GdkSession]
     * @param from the source [AccountAsset] (where funds come from)
     * @param to the destination [AccountAsset] (where funds will go)
     * @param fees optional [Fees] information for calculation (mainly for Chain and Reverse swaps)
     * @param amount the amount to swap (in satoshis)
     * @return [SwapDetails] containing the necessary information to proceed with the swap
     * @throws Exception if the networks are identical or if the requested swap pair is not supported
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        from: AccountAsset,
        to: AccountAsset,
        quote: Quote?,
        amount: Long?
    ): SwapDetails {

        // Single source of truth with the UI validation: blocks unsupported pairs
        // (Liquid <-> Lightning, same network) even if invoked with stale state.
        if (!isSwapPairSupported(from, to)) {
            throw Exception("id_this_swap_pair_is_not_supported_yet")
        }

        val amountNotNull = requireNotNull(amount) { "Amount is required for swap creation" }

        return when {
            // Chain
            listOf(from.account.isLightning, to.account.isLightning).all { !it } -> {

                createChainSwapUseCase(
                    wallet = wallet,
                    session = session,
                    fromAccount = from.account,
                    toAccount = to.account,
                    quote = quote,
                    amount = amountNotNull,
                    address = getReceiveAddressUseCase(session, to.account).address
                )
            }

            // On-chain (Bitcoin/Liquid) -> Lightning, normal submarine. Bitcoin source also carries
            // the Lightning channel-open setup fee.
            to.account.isLightning -> {

                // Mirror the chain-swap model: the entered amount is what the user spends, and the
                // invoice is the quoted receive amount (entered minus swap fees) - the same figure
                // the swap screen quotes and the review screen confirms.
                val invoice = session.createLightningInvoice(satoshi = quote?.receiveAmount ?: amountNotNull, description = "")

                createNormalSubmarineSwapUseCase(
                    wallet = wallet,
                    session = session,
                    isAutoSwap = false,
                    account = from.account,
                    invoice = invoice.invoice.bolt11
                ).let { if (from.account.isBitcoin) it.copy(lightningSetupFee = invoice.openingFeeSatoshi) else it }
            }

            // Lightning -> on-chain (Bitcoin/Liquid), reverse submarine. Claim onto the Bitcoin
            // destination account when present; the Liquid path claims via the Lightning account's
            // (Liquid) receive address.
            from.account.isLightning -> {

                createReverseSubmarineSwapUseCase(
                    wallet = wallet,
                    session = session,
                    isAutoSwap = false,
                    account = if (to.account.isBitcoin) to.account else from.account,
                    amount = amountNotNull
                ).let { swap ->
                    SwapDetails(
                        swapId = swap.swapId(),
                        address = swap.bolt11Invoice().toString(),
                        fromAmount = swap.bolt11Invoice().amountMilliSatoshis()!!.satoshi(),
                        toAmount = quote?.receiveAmount,
                        fromAssetId = from.assetId,
                        toAssetId = to.assetId,
                        providerFee = quote?.boltzFee ?: 0,
                        claimNetworkFee = quote?.claimNetworkFee ?: 0,
                    )
                }
            }

            else -> throw Exception("Invalid swap from $from to $to")
        }
    }
}
