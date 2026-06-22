package com.blockstream.domain.send

class SendUseCase constructor(
    val getSendAmountUseCase: GetSendAmountUseCase,
    val showFeeSelectorUseCase: ShowFeeSelectorUseCase,
    val prepareTransactionUseCase: PrepareTransactionUseCase,
    val getSpendableUtxosUseCase: GetSpendableUtxosUseCase,
    val getTransactionConfirmationUseCase: GetTransactionConfirmationUseCase,
)
