package com.blockstream.domain.send

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sendModule = module {
    factoryOf(::GetSendAssetsUseCase)
    factoryOf(::GetSendFlowUseCase)
    singleOf(::GetSendAccountsUseCase)
    singleOf(::GetSendAmountUseCase)
    singleOf(::PrepareTransactionUseCase)
    singleOf(::ShowFeeSelectorUseCase)
    singleOf(::GetTransactionConfirmationUseCase)
    singleOf(::SendUseCase)
}
