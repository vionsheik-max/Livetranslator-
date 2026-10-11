package com.livetranslate.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.livetranslate.app.LiveTranslateApplication
import com.livetranslate.app.R
import com.livetranslate.app.data.preferences.SettingsRepository
import com.livetranslate.app.overlay.CaptionOverlayController
import com.livetranslate.app.translation.SpeechTranslationEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VoiceInterpreterService : LifecycleService(), RecognitionListener {
    private val settingsRepository: SettingsRepository
        get() = (application as LiveTranslateApplication).appContainer.settingsRepository
    private val engine = SpeechTranslationEngine()
    private val restartHandler = Handler(Looper.getMainLooper())
    private val captionOverlay by lazy { CaptionOverlayController(this, this) }
    private var recognizer: SpeechRecognizer? = null
    private var translationJob: Job? = null
    private var targetLanguage = com.livetranslate.app.domain.model.AppLanguage.ENGLISH

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSession()
            return START_NOT_STICKY
        }
        lifecycleScope.launch {
            targetLanguage = settingsRepository.settings.first().targetLanguage
            startSession()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        translationJob?.cancel()
        engine.close()
        captionOverlay.remove()
        super.onDestroy()
    }

    private fun startSession() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSession()
            return
        }
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0,
        )
        captionOverlay.show()
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
                it.setRecognitionListener(this)
            }
        }
        startListening()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    private fun stopListening() {
        restartHandler.removeCallbacksAndMessages(null)
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun stopSession() {
        lifecycleScope.launch { settingsRepository.setVoiceInterpreterEnabled(false) }
        stopListening()
        captionOverlay.remove()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun translateAndRender(text: String) {
        translationJob?.cancel()
        translationJob = lifecycleScope.launch {
            val translated = runCatching { engine.translate(text, targetLanguage) }.getOrDefault(text)
            captionOverlay.render(translated)
        }
    }

    override fun onPartialResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.let(::translateAndRender)
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.let(::translateAndRender)
        restartHandler.postDelayed(::startListening, 120L)
    }

    override fun onEndOfSpeech() {
        restartHandler.postDelayed(::startListening, 120L)
    }

    override fun onError(error: Int) {
        if (error != SpeechRecognizer.ERROR_CLIENT) {
            restartHandler.postDelayed(::startListening, 400L)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle(getString(R.string.voice_notification_title))
        .setContentText(getString(R.string.voice_notification_body))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.voice_notification_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        private const val ACTION_START = "com.livetranslate.app.action.START_VOICE"
        private const val ACTION_STOP = "com.livetranslate.app.action.STOP_VOICE"
        private const val CHANNEL_ID = "live_translate_voice"
        private const val NOTIFICATION_ID = 405

        fun start(context: Context) = ContextCompat.startForegroundService(
            context,
            Intent(context, VoiceInterpreterService::class.java).setAction(ACTION_START),
        )
        fun stop(context: Context) = context.startService(
            Intent(context, VoiceInterpreterService::class.java).setAction(ACTION_STOP),
        )
    }
}
