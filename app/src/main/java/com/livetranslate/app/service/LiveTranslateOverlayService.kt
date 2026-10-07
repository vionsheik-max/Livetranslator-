package com.livetranslate.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveTranslateOverlayService : LifecycleService() {

    private val settingsRepository: SettingsRepository
        get() = (application as LiveTranslateApplication).appContainer.settingsRepository

    private val overlayController: OverlayWindowController by lazy {
        OverlayWindowController(
            context = this,
            lifecycleOwner = this,
            onPositionChanged = { position ->
                lifecycleScope.launch {
                    settingsRepository.setOverlayPosition(position)
                }
            },
            onExpandedChanged = { expanded ->
                lifecycleScope.launch {
                    settingsRepository.setOverlayExpanded(expanded)
                }
            },
            onLiveTranslationToggled = { enabled ->
                lifecycleScope.launch {
                    settingsRepository.setLiveTranslationEnabled(enabled)
                }
            },
            onTargetLanguageSelected = { language ->
                lifecycleScope.launch {
                    settingsRepository.setTargetLanguage(language)
                }
            },
            onModeSelected = { mode ->
                lifecycleScope.launch {
                    settingsRepository.setMode(mode)
                }
            },
            onOpenApp = ::openMainApp,
            onStopSession = {
                stopSession(resetSetting = true)
            },
        )
    }

    private var settingsJob: Job? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSession(resetSetting = true)
                return START_NOT_STICKY
            }

            ACTION_START,
            null -> startSession()

            else -> startSession()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        overlayController.remove()
        super.onDestroy()
    }

    private fun startSession() {
        if (!OverlayPermissionManager.canDrawOverlays(this)) {
            lifecycleScope.launch {
                settingsRepository.setLiveTranslationEnabled(false)
            }
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        overlayController.show()

        if (settingsJob == null) {
            settingsJob = lifecycleScope.launch {
                settingsRepository.settings.collectLatest { settings ->
                    if (!settings.liveTranslationEnabled) {
                        stopSession(resetSetting = false)
                        return@collectLatest
                    }

                    overlayController.update(
                        state = OverlayUiState(
                            liveTranslationEnabled = settings.liveTranslationEnabled,
                            targetLanguage = settings.targetLanguage,
                            selectedMode = settings.selectedMode,
                            isExpanded = settings.overlayExpanded,
                        ),
                        overlayPosition = settings.overlayPosition,
                    )
                }
            }
        }
    }

    private fun stopSession(resetSetting: Boolean) {
        settingsJob?.cancel()
        settingsJob = null
        overlayController.remove()

        if (resetSetting) {
            lifecycleScope.launch {
                settingsRepository.setOverlayExpanded(false)
                settingsRepository.setLiveTranslationEnabled(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun openMainApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.overlay_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.overlay_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "com.livetranslate.app.action.START_OVERLAY"
        private const val ACTION_STOP = "com.livetranslate.app.action.STOP_OVERLAY"
        private const val NOTIFICATION_CHANNEL_ID = "live_translate_overlay"
        private const val NOTIFICATION_ID = 404

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LiveTranslateOverlayService::class.java).apply {
                    action = ACTION_START
                },
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LiveTranslateOverlayService::class.java).apply {
                    action = ACTION_STOP
                },
            )
        }
    }
}
