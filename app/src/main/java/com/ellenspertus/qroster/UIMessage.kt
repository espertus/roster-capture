package com.ellenspertus.qroster

sealed class UiMessage {
    data class Success(val text: String) : UiMessage()
    data class Failure(val text: String) : UiMessage()
}
