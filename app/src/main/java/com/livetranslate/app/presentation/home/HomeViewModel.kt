package com.livetranslate.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.livetranslate.app.data.preferences.SettingsRepository
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val languagePackState = MutableStateFlow<LanguagePackState>(LanguagePackState.Idle)

    val uiState: StateFlow<HomeUiState> = settingsRepository.settings
        .combine(languagePackState) { settings, packState ->
            HomeUiState(
                targetLanguage = settings.targetLanguage,
                liveTranslationEnabled = settings.liveTranslationEnabled,
                voiceInterpreterEnabled = settings.voiceInterpreterEnabled,
                selectedMode = settings.selectedMode,
                languagePackState = packState,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState(),
        )

    fun onTargetLanguageSelected(language: AppLanguage) = viewModelScope.launch {
        settingsRepository.setTargetLanguage(language)
    }

    fun onLiveTranslationToggled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setLiveTranslationEnabled(enabled)
    }

    fun onVoiceInterpreterToggled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setVoiceInterpreterEnabled(enabled)
    }

    fun onModeSelected(mode: TranslationMode) = viewModelScope.launch {
        settingsRepository.setMode(mode)
    }

    fun downloadLanguagePack(language: AppLanguage) {
        if (languagePackState.value is LanguagePackState.Downloading) return
        if (language == AppLanguage.ENGLISH) {
            languagePackState.value = LanguagePackState.Ready(language)
            return
        }
        languagePackState.value = LanguagePackState.Downloading
        viewModelScope.launch {
            val target = TranslateLanguage.fromLanguageTag(language.tag)
            if (target == null) {
                languagePackState.value = LanguagePackState.Failed(
                    "This language is not available for offline translation yet.",
                )
                return@launch
            }
            val translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(target)
                    .build(),
            )
            try {
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                languagePackState.value = LanguagePackState.Ready(language)
            } catch (error: Exception) {
                languagePackState.value = LanguagePackState.Failed(
                    error.message ?: "Download failed. Check your connection and try again.",
                )
            } finally {
                translator.close()
            }
        }
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(repository) as T
            }
    }
}
