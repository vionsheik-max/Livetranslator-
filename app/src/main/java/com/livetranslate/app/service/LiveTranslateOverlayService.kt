package com.livetranslate.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.livetranslate.app.LiveTranslateApplication
import com.livetranslate.app.MainActivity
import com.livetranslate.app.R
import com.livetranslate.app.data.preferences.SettingsRepository
import com.livetranslate.app.overlay.OverlayPermissionManager
import com.livetranslate.app.overlay.OverlayUiState
import com.livetranslate.app.overlay.OverlayWindowController
import com.livetranslate.app.overlay.TranslationTextOverlayController
import com.livetranslate.app.translation.OnDeviceTranslationPipeline
import com.livetranslate.core.cache.InMemoryTranslationCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveTranslateOverlayService : LifecycleService() {
    private val settingsRepository: SettingsRepository
        get() = (application as LiveTranslateApplication).appContainer.settingsRepository
    private val captureManager
        get() = (application as LiveTranslateApplication).appContainer.screenCaptureManager
    private val translationPipeline = OnDeviceTranslationPipeline(InMemoryTranslationCache())
    private val textOverlayController by lazy { TranslationTextOverlayController(this, this) }
    private var settingsJob: Job? = null
    private var translationJob: Job? = null

    private val overlayController: OverlayWindowController by lazy {
        OverlayWindowController(
            context = this,
            lifecycleOwner = this,
            onPositionChanged = { position -> lifecycleScope.launch { settingsRepository.setOverlayPosition(position) } },
            onExpandedChanged = { expanded -> lifecycleScope.launch { settingsRepository.setOverlayExpanded(expanded) } },
            onLiveTranslationToggled = { enabled -> lifecycleScope.launch { settingsRepository.setLiveTranslationEnabled(enabled) } },
            onTargetLanguageSelected = { language -> lifecycleScope.launch { settingsRepository.setTargetLanguage(language) } },
            onModeSelected = { mode -> lifecycleScope.launch { settingsRepository.setMode(mode) } },
            onRefresh = translationPipeline::reset,
            onOpenApp = ::openMainApp,
            onStopSession = { stopSession(resetSetting = true) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSession(resetSetting = true)
            return START_NOT_STICKY
        }
        startSession()
        return START_STICKY
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        translationJob?.cancel()
        overlayController.remove()
        textOverlayController.remove()
        translationPipeline.close()
        super.onDestroy()
    }

    private fun startSession() {
        if (!OverlayPermissionManager.canDrawOverlays(this)) {
            lifecycleScope.launch { settingsRepository.setLiveTranslationEnabled(false) }
            stopSelf()
            return
        }
        try {
            createNotificationChannel()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0,
            )
            overlayController.show()
            textOverlayController.show()
        } catch (error: RuntimeException) {
            lifecycleScope.launch { settingsRepository.setLiveTranslationEnabled(false) }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (settingsJob != null) return
        settingsJob = lifecycleScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                if (!settings.liveTranslationEnabled) {
                    stopSession(resetSetting = false)
                    return@collectLatest
                }
                overlayController.update(
                    state = OverlayUiState(
                        liveTranslationEnabled = true,
                        targetLanguage = settings.targetLanguage,
                        selectedMode = settings.selectedMode,
                        isExpanded = settings.overlayExpanded,
                        themeId = settings.themeId,
                        orbSizeDp = settings.orbSizeDp,
                    ),
                    overlayPosition = settings.overlayPosition,
                )
                translationJob?.cancel()
                translationJob = launch(Dispatchers.Default) { runTranslationLoop(settings.targetLanguage) }
            }
        }
    }

    private suspend fun runTranslationLoop(targetLanguage: com.livetranslate.app.domain.model.AppLanguage) {
        while (true) {
            val startedAt = SystemClock.elapsedRealtime()
            val frame = captureManager.capture()
            if (frame != null) {
                val translated = runCatching {
                    translationPipeline.process(
                        bitmap = frame.bitmap,
                        captureWidth = frame.captureWidth,
                        captureHeight = frame.captureHeight,
                        displayWidth = frame.screenWidth,
                        displayHeight = frame.screenHeight,
                        targetLanguage = targetLanguage,
                    )
                }.getOrDefault(emptyList())
                textOverlayController.render(translated)
                frame.bitmap.recycle()
            }
            // Pace from capture start, not after OCR. Slow devices never build a backlog.
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            delay((FRAME_INTERVAL_MS - elapsed).coerceAtLeast(MIN_FRAME_GAP_MS))
        }
    }

    private fun stopSession(resetSetting: Boolean) {
        settingsJob?.cancel(); settingsJob = null
        translationJob?.cancel(); translationJob = null
        overlayController.remove(); textOverlayController.remove()
        if (resetSetting) {
            lifecycleScope.launch {
                settingsRepository.setOverlayExpanded(false)
                settingsRepository.setLiveTranslationEnabled(false)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun openMainApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 100,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_body))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.overlay_notification_channel_description) },
        )
    }

    companion object {
        private const val ACTION_START = "com.livetranslate.app.action.START_OVERLAY"
        private const val ACTION_STOP = "com.livetranslate.app.action.STOP_OVERLAY"
        private const val NOTIFICATION_CHANNEL_ID = "live_translate_overlay"
        private const val NOTIFICATION_ID = 404
        private const val FRAME_INTERVAL_MS = 180L
        private const val MIN_FRAME_GAP_MS = 24L

        fun start(context: Context) = ContextCompat.startForegroundService(
            context,
            Intent(context, LiveTranslateOverlayService::class.java).setAction(ACTION_START),
        )
        fun stop(context: Context) = context.startService(
            Intent(context, LiveTranslateOverlayService::class.java).setAction(ACTION_STOP),
        )
    }
}
