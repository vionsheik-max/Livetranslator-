package com.livetranslate.app.presentation.home

import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import com.livetranslate.app.presentation.theme.AppTheme

data class HomeUiState(
    val description: String = "Translate what is on your screen.",
    val targetLanguage: AppLanguage = AppLanguage.ENGLISH,
    val liveTranslationEnabled: Boolean = false,
    val voiceInterpreterEnabled: Boolean = false,
    val selectedMode: TranslationMode = TranslationMode.AUTOMATIC,
    val availableLanguages: List<AppLanguage> = AppLanguage.entries,
    val availableModes: List<TranslationMode> = TranslationMode.entries,
    val languagePackState: LanguagePackState = LanguagePackState.Idle,
    val theme: AppTheme = AppTheme.MIST,
    val orbSizeDp: Float = 48f,
)

sealed interface LanguagePackState {
    data object Idle : LanguagePackState
    data object Downloading : LanguagePackState
    data class Ready(val language: AppLanguage) : LanguagePackState
    data class Failed(val message: String) : LanguagePackState
}
