package com.livetranslate.app.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MediaProjectionScreenCaptureManager(
    context: Context,
    private val resolutionSelector: CaptureResolutionSelector = CaptureResolutionSelector(),
) : ScreenCaptureManager {
    private val appContext = context.applicationContext
    private val mediaProjectionManager = appContext.getSystemService(MediaProjectionManager::class.java)
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val projectionCallbackHandler = Handler(Looper.getMainLooper())
    private val mutableSessionState = MutableStateFlow<CaptureSessionState>(CaptureSessionState.PermissionRequired)
    private val lock = Any()

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var lastSessionSpec: SessionSpec? = null
    override val sessionState: StateFlow<CaptureSessionState> = mutableSessionState

    override fun hasActiveProjection(): Boolean = synchronized(lock) {
        mediaProjection != null
    }

    override fun startProjection(
        resultCode: Int,
        permissionData: Intent,
    ): Boolean {
        return startProjectionInternal(
            resultCode = resultCode,
            permissionData = permissionData,
        )
    }

    override suspend fun capture(): BitmapScreenFrame? {
        val spec = synchronized(lock) {
            ensureVirtualDisplay()
        } ?: return null

        val image = synchronized(lock) {
            imageReader?.acquireLatestImage()
        } ?: return null

        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val bitmap = bitmapFromImage(
                width = image.width,
                height = image.height,
                bufferReader = {
                    copyPixelsFromBuffer(plane.buffer)
                },
                rowStride = plane.rowStride,
                pixelStride = plane.pixelStride,
            )
            val timestamp = System.currentTimeMillis()
            mutableSessionState.value = CaptureSessionState.Ready(
                screenWidth = spec.screenWidth,
                screenHeight = spec.screenHeight,
                captureWidth = spec.captureWidth,
                captureHeight = spec.captureHeight,
                lastCaptureTimestampMillis = timestamp,
            )
            BitmapScreenFrame(
                bitmap = bitmap,
                screenWidth = spec.screenWidth,
                screenHeight = spec.screenHeight,
                captureWidth = spec.captureWidth,
                captureHeight = spec.captureHeight,
                captureScaleFactor = spec.scaleFactor,
                capturedAtMillis = timestamp,
            )
        } catch (throwable: Throwable) {
            mutableSessionState.value = CaptureSessionState.Error(
                throwable.message ?: "Screen capture failed.",
            )
            null
        } finally {
            image.close()
        }
    }

    override fun stopProjection() {
        synchronized(lock) {
            releaseVirtualDisplay()
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
            mediaProjection = null
        }
        mutableSessionState.value = CaptureSessionState.PermissionRequired
    }

    private fun startProjectionInternal(
        resultCode: Int,
        permissionData: Intent,
    ): Boolean {
        return try {
            val projection = mediaProjectionManager.getMediaProjection(resultCode, permissionData)
            synchronized(lock) {
                releaseVirtualDisplay()
                mediaProjection?.unregisterCallback(projectionCallback)
                mediaProjection?.stop()
                mediaProjection = projection
                mediaProjection?.registerCallback(projectionCallback, projectionCallbackHandler)
                lastSessionSpec = null
            }
            mutableSessionState.value = CaptureSessionState.Starting
            synchronized(lock) {
                ensureVirtualDisplay()
            }
            true
        } catch (securityException: SecurityException) {
            mutableSessionState.value = CaptureSessionState.Error(
                "Screen capture permission was rejected by the system.",
            )
            false
        } catch (throwable: Throwable) {
            mutableSessionState.value = CaptureSessionState.Error(
                throwable.message ?: "Unable to start screen capture.",
            )
            false
        }
    }

    private fun ensureVirtualDisplay(): SessionSpec? {
        val projection = mediaProjection ?: return null
        val desiredSpec = currentSessionSpec()
        if (desiredSpec == lastSessionSpec && virtualDisplay != null && imageReader != null) {
            return desiredSpec
        }

        releaseVirtualDisplay()
        val reader = ImageReader.newInstance(
            desiredSpec.captureWidth,
            desiredSpec.captureHeight,
            PixelFormat.RGBA_8888,
            2,
        )
        val display = projection.createVirtualDisplay(
            "LiveTranslateCapture",
            desiredSpec.captureWidth,
            desiredSpec.captureHeight,
            desiredSpec.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            projectionCallbackHandler,
        )
        imageReader = reader
        virtualDisplay = display
        lastSessionSpec = desiredSpec
        return desiredSpec
    }

    private fun releaseVirtualDisplay() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        virtualDisplay = null
        imageReader = null
        lastSessionSpec = null
    }

    private fun currentSessionSpec(): SessionSpec {
        val metrics = currentScreenMetrics()
        val dimensions = resolutionSelector.select(
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
        )
        return SessionSpec(
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            captureWidth = dimensions.width,
            captureHeight = dimensions.height,
            densityDpi = metrics.densityDpi,
            scaleFactor = dimensions.scaleFactor,
        )
    }

    private fun currentScreenMetrics(): DisplayMetrics {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                densityDpi = appContext.resources.displayMetrics.densityDpi
            }
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            displayMetrics
        }
    }

    private fun bitmapFromImage(
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        bufferReader: Bitmap.() -> Unit,
    ): Bitmap {
        val rowPadding = rowStride - (pixelStride * width)
        val paddedBitmap = Bitmap.createBitmap(
            width + (rowPadding / pixelStride),
            height,
            Bitmap.Config.ARGB_8888,
        ).apply(bufferReader)
        return Bitmap.createBitmap(paddedBitmap, 0, 0, width, height).also { cropped ->
            if (cropped != paddedBitmap) {
                paddedBitmap.recycle()
            }
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            synchronized(lock) {
                releaseVirtualDisplay()
                mediaProjection?.unregisterCallback(this)
                mediaProjection = null
            }
            mutableSessionState.value = CaptureSessionState.PermissionRequired
        }
    }

    private data class SessionSpec(
        val screenWidth: Int,
        val screenHeight: Int,
        val captureWidth: Int,
        val captureHeight: Int,
        val densityDpi: Int,
        val scaleFactor: Float,
    )
}
