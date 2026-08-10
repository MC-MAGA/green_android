package com.blockstream.domain.swap

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module definition for swap-related domain use cases.
 */
val swapModule = module {
    singleOf(::CreateReverseSubmarineSwapUseCase)
    singleOf(::CreateNormalSubmarineSwapUseCase)
    singleOf(::CreateChainSwapUseCase)
    singleOf(::CreateSwapUseCase)
    singleOf(::HandleSwapEventsUseCase)
    singleOf(::ResetSwapUseCase)
    singleOf(::ResetWalletSwapsUseCase)
    singleOf(::HasWalletSwapsUseCase)
    singleOf(::IsSwapsEnabledUseCase)
    singleOf(::CanSwapsBeDisabledUseCase)
    singleOf(::GetWalletFromSwapUseCase)
    singleOf(::IsInvoiceSwappableUseCase)
    singleOf(::IsLiquidToLightningSwapUseCase)
    singleOf(::PrepareSwapTransactionUseCase)
    singleOf(::IsSwapAvailableUseCase)
    single { SwapAvailability.Current }
    singleOf(::IsSwapDirectionAvailableUseCase)
    singleOf(::GetQuoteUseCase)
    singleOf(::SwapUseCase)
    singleOf(::GetSwapAmountUseCase)
}
