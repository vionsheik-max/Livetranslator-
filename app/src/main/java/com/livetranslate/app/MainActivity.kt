package com.livetranslate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livetranslate.app.data.preferences.DataStoreSettingsRepository
import com.livetranslate.app.presentation.home.HomeScreen
import com.livetranslate.app.presentation.home.HomeViewModel
import com.livetranslate.app.presentation.theme.LiveTranslateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DataStoreSettingsRepository(applicationContext)

        setContent {
            LiveTranslateTheme {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(repository),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onTargetLanguageSelected = viewModel::onTargetLanguageSelected,
                    onLiveTranslationToggled = viewModel::onLiveTranslationToggled,
                    onModeSelected = viewModel::onModeSelected,
                )
            }
        }
    }
}
