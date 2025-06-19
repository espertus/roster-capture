package com.ellenspertus.qroster

sealed class AppException(message: String, val restartable: Boolean) : Exception(message) {
    /**
     * Exception caused by user behavior, such as not granting permissions
     * or not installing AnkiDroid.
     */
    class AppUserException(message: String, restartable: Boolean = false) : AppException(message, restartable)

    /**
     * Exception caused by programmer error (whether this app or AnkiDroid).
     */
    class AppInternalException(message: String) : AppException(message, false)
}
