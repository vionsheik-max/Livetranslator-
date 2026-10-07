package com.livetranslate.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.livetranslate.app.data.preferences.SettingsRepository
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = settingsRepository.settings
        .map { settings ->
            HomeUiState(
                targetLanguage = settings.targetLanguage,
                liveTranslationEnabled = settings.liveTranslationEnabled,
                selectedMode = settings.selectedMode,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState(),
        )

    fun onTargetLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setTargetLanguage(language)
        }
    }

    fun onLiveTranslationToggled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLiveTranslationEnabled(enabled)
        }
    }

    fun onModeSelected(mode: TranslationMode) {
        viewModelScope.launch {
            settingsRepository.setMode(mode)
        }
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
