package com.livetranslate.app.capture

sealed interface CaptureSessionState {
    data object PermissionRequired : CaptureSessionState

    data object Starting : CaptureSessionState

    data class Ready(
        val screenWidth: Int,
        val screenHeight: Int,
        val captureWidth: Int,
        val captureHeight: Int,
        val lastCaptureTimestampMillis: Long,
    ) : CaptureSessionState

    data class Error(
        val message: String,
    ) : CaptureSessionState
}

val CaptureSessionState.isReady: Boolean
    get() = this is CaptureSessionState.Ready
