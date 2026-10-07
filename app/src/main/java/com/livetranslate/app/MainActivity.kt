package com.livetranslate.app

import android.app.Activity
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livetranslate.app.capture.ScreenCaptureManager
import com.livetranslate.app.data.preferences.SettingsRepository
import com.livetranslate.app.overlay.OverlayPermissionManager
import com.livetranslate.app.presentation.home.HomeScreen
import com.livetranslate.app.presentation.home.HomeViewModel
import com.livetranslate.app.presentation.theme.LiveTranslateTheme
import com.livetranslate.app.service.LiveTranslateOverlayService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val overlayPermissionGranted = mutableStateOf(false)
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
        overlayPermissionGranted.value = OverlayPermissionManager.canDrawOverlays(this)

        setContent {
            LiveTranslateTheme {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(settingsRepository),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val captureSessionState by screenCaptureManager.sessionState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    overlayPermissionGranted = overlayPermissionGranted.value,
                    captureSessionState = captureSessionState,
                    onTargetLanguageSelected = viewModel::onTargetLanguageSelected,
                    onLiveTranslationToggled = ::handleLiveTranslationToggle,
                    onModeSelected = viewModel::onModeSelected,
                    onRequestOverlayPermission = ::requestOverlayPermission,
                    onRequestScreenCapturePermission = ::requestScreenCapturePermission,
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
}
