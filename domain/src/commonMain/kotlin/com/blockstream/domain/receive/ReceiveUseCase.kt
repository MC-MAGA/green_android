package com.blockstream.domain.receive

class ReceiveUseCase(
    val getReceiveAccountsUseCase: GetReceiveAccountsUseCase,
    val saveAndShareQrCodeUseCase: SaveAndShareQrCodeUseCase,
    val getLightningReceiveAmountStateUseCase: GetLightningReceiveAmountStateUseCase
)
