package com.livetranslate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    private val overlayPermissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = (application as LiveTranslateApplication).appContainer.settingsRepository
        overlayPermissionGranted.value = OverlayPermissionManager.canDrawOverlays(this)

        setContent {
            LiveTranslateTheme {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(settingsRepository),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    overlayPermissionGranted = overlayPermissionGranted.value,
                    onTargetLanguageSelected = viewModel::onTargetLanguageSelected,
                    onLiveTranslationToggled = { enabled ->
                        handleLiveTranslationToggle(enabled, viewModel)
                    },
                    onModeSelected = viewModel::onModeSelected,
                    onRequestOverlayPermission = ::requestOverlayPermission,
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
                LiveTranslateOverlayService.stop(this@MainActivity)
            } else if (settings.liveTranslationEnabled && permissionGranted) {
                LiveTranslateOverlayService.start(this@MainActivity)
            }
        }
    }

    private fun handleLiveTranslationToggle(
        enabled: Boolean,
        viewModel: HomeViewModel,
    ) {
        if (enabled && !OverlayPermissionManager.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        viewModel.onLiveTranslationToggled(enabled)
        if (enabled) {
            LiveTranslateOverlayService.start(this)
        } else {
            lifecycleScope.launch {
                settingsRepository.setOverlayExpanded(false)
            }
            LiveTranslateOverlayService.stop(this)
        }
    }

    private fun requestOverlayPermission() {
        startActivity(OverlayPermissionManager.createPermissionIntent(this))
    }
}
