package com.blockstream.data.swap

// Which side of the swap an amount error refers to, so the UI can highlight the offending value.
enum class SwapErrorSide { NONE, FROM, TO }

data class SwapAmount constructor(
    val quote: Quote? = null,
    val amountFrom: String = "",
    val amountFromExchange: String? = null,
    val amountTo: String = "",
    val amountToExchange: String? = null,
    val error: String? = null,
    val errorSide: SwapErrorSide = SwapErrorSide.NONE,
    val isValid: Boolean = false
)
