package com.livetranslate.app.capture

import android.content.Intent
import android.graphics.Bitmap
import com.livetranslate.core.pipeline.ScreenCaptureProvider
import kotlinx.coroutines.flow.StateFlow

data class BitmapScreenFrame(
    val bitmap: Bitmap,
    val screenWidth: Int,
    val screenHeight: Int,
    val captureWidth: Int,
    val captureHeight: Int,
    val captureScaleFactor: Float,
    val capturedAtMillis: Long,
)

interface ScreenCaptureManager : ScreenCaptureProvider<BitmapScreenFrame> {
    val sessionState: StateFlow<CaptureSessionState>

    fun hasActiveProjection(): Boolean

    fun startProjection(
        resultCode: Int,
        permissionData: Intent,
    ): Boolean

    fun stopProjection()
}
