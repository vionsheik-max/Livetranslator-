package com.livetranslate.app

import android.app.Activity
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livetranslate.app.capture.ScreenCaptureManager
import com.livetranslate.app.billing.BillingState
import com.livetranslate.app.billing.PlayBillingManager
import com.livetranslate.app.data.preferences.SettingsRepository
import com.livetranslate.app.overlay.OverlayPermissionManager
import com.livetranslate.app.presentation.home.HomeScreen
import com.livetranslate.app.presentation.home.HomeViewModel
import com.livetranslate.app.presentation.theme.LiveTranslateTheme
import com.livetranslate.app.service.LiveTranslateOverlayService
import com.livetranslate.app.service.VoiceInterpreterService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var billingManager: PlayBillingManager
    private val overlayPermissionGranted = mutableStateOf(false)
    private var pendingVoiceStart = false
    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingVoiceStart) {
            pendingVoiceStart = false
            lifecycleScope.launch { settingsRepository.setVoiceInterpreterEnabled(true) }
            VoiceInterpreterService.start(this)
        } else if (!granted) {
            pendingVoiceStart = false
            lifecycleScope.launch { settingsRepository.setVoiceInterpreterEnabled(false) }
        }
    }
    private val screenCapturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        handleScreenCapturePermissionResult(
            resultCode = result.resultCode,
            projectionData = result.data,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as LiveTranslateApplication).appContainer
        settingsRepository = appContainer.settingsRepository
        screenCaptureManager = appContainer.screenCaptureManager
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        billingManager = PlayBillingManager(this)
        overlayPermissionGranted.value = OverlayPermissionManager.canDrawOverlays(this)

        setContent {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(settingsRepository),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val billingState by billingManager.state.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.theme) {
                LauncherIconManager.apply(this@MainActivity, uiState.theme)
            }
            LiveTranslateTheme(theme = uiState.theme) {
                val captureSessionState by screenCaptureManager.sessionState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    overlayPermissionGranted = overlayPermissionGranted.value,
                    captureSessionState = captureSessionState,
                    onTargetLanguageSelected = viewModel::onTargetLanguageSelected,
                    onLiveTranslationToggled = ::handleLiveTranslationToggle,
                    onVoiceInterpreterToggled = { enabled ->
                        if (enabled && billingManager.state.value !is BillingState.Pro) {
                            billingManager.launchPurchase(this@MainActivity)
                        } else {
                            handleVoiceInterpreterToggle(enabled)
                        }
                    },
                    onModeSelected = viewModel::onModeSelected,
                    onDownloadLanguagePack = { language ->
                        if (billingManager.state.value is BillingState.Pro) {
                            viewModel.downloadLanguagePack(language)
                        } else {
                            billingManager.launchPurchase(this@MainActivity)
                        }
                    },
                    onThemeSelected = viewModel::onThemeSelected,
                    onOrbSizeChanged = viewModel::onOrbSizeChanged,
                    onRequestOverlayPermission = ::requestOverlayPermission,
                    onRequestScreenCapturePermission = ::requestScreenCapturePermission,
                    isPro = billingState is BillingState.Pro,
                    proPrice = (billingState as? BillingState.Ready)?.price,
                    onUpgrade = { billingManager.launchPurchase(this@MainActivity) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val permissionGranted = OverlayPermissionManager.canDrawOverlays(this)
        overlayPermissionGranted.value = permissionGranted

        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.liveTranslationEnabled && !permissionGranted) {
                settingsRepository.setOverlayExpanded(false)
                settingsRepository.setLiveTranslationEnabled(false)
                screenCaptureManager.stopProjection()
                LiveTranslateOverlayService.stop(this@MainActivity)
            } else if (settings.liveTranslationEnabled && !screenCaptureManager.hasActiveProjection()) {
                settingsRepository.setOverlayExpanded(false)
                settingsRepository.setLiveTranslationEnabled(false)
                LiveTranslateOverlayService.stop(this@MainActivity)
            } else if (settings.liveTranslationEnabled && permissionGranted) {
                LiveTranslateOverlayService.start(this@MainActivity)
            }
            if (settings.voiceInterpreterEnabled) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    VoiceInterpreterService.start(this@MainActivity)
                } else {
                    settingsRepository.setVoiceInterpreterEnabled(false)
                    VoiceInterpreterService.stop(this@MainActivity)
                }
            }
        }
    }

    private fun handleLiveTranslationToggle(enabled: Boolean) {
        if (enabled && !OverlayPermissionManager.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        if (enabled) {
            if (screenCaptureManager.hasActiveProjection()) {
                lifecycleScope.launch {
                    settingsRepository.setLiveTranslationEnabled(true)
                }
                LiveTranslateOverlayService.start(this)
            } else {
                requestScreenCapturePermission()
            }
        } else {
            lifecycleScope.launch {
                settingsRepository.setOverlayExpanded(false)
                settingsRepository.setLiveTranslationEnabled(false)
            }
            screenCaptureManager.stopProjection()
            LiveTranslateOverlayService.stop(this)
        }
    }

    private fun handleVoiceInterpreterToggle(enabled: Boolean) {
        if (!enabled) {
            lifecycleScope.launch { settingsRepository.setVoiceInterpreterEnabled(false) }
            VoiceInterpreterService.stop(this)
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingVoiceStart = true
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        lifecycleScope.launch { settingsRepository.setVoiceInterpreterEnabled(true) }
        VoiceInterpreterService.start(this)
    }

    private fun requestOverlayPermission() {
        startActivity(OverlayPermissionManager.createPermissionIntent(this))
    }

    private fun requestScreenCapturePermission() {
        screenCapturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun handleScreenCapturePermissionResult(
        resultCode: Int,
        projectionData: android.content.Intent?,
    ) {
        if (resultCode != Activity.RESULT_OK || projectionData == null) {
            lifecycleScope.launch {
                settingsRepository.setLiveTranslationEnabled(false)
            }
            screenCaptureManager.stopProjection()
            LiveTranslateOverlayService.stop(this)
            return
        }

        lifecycleScope.launch {
            val projectionStarted = screenCaptureManager.startProjection(
                resultCode = resultCode,
                permissionData = projectionData,
            )
            if (!projectionStarted) {
                settingsRepository.setLiveTranslationEnabled(false)
                LiveTranslateOverlayService.stop(this@MainActivity)
                return@launch
            }

            settingsRepository.setLiveTranslationEnabled(true)
            settingsRepository.setOverlayExpanded(false)
            overlayPermissionGranted.value = OverlayPermissionManager.canDrawOverlays(this@MainActivity)
            if (!overlayPermissionGranted.value) {
                settingsRepository.setLiveTranslationEnabled(false)
                screenCaptureManager.stopProjection()
                return@launch
            }

            LiveTranslateOverlayService.start(this@MainActivity)
        }
    }

    override fun onDestroy() {
        if (::billingManager.isInitialized) billingManager.close()
        super.onDestroy()
    }
}
