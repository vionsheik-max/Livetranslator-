package com.livetranslate.app.presentation.home

import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode

data class HomeUiState(
    val description: String = "Translate text in place with a calm, permission-aware live session.",
    val targetLanguage: AppLanguage = AppLanguage.ENGLISH,
    val liveTranslationEnabled: Boolean = false,
    val voiceInterpreterEnabled: Boolean = false,
    val selectedMode: TranslationMode = TranslationMode.AUTOMATIC,
    val availableLanguages: List<AppLanguage> = AppLanguage.entries,
    val availableModes: List<TranslationMode> = TranslationMode.entries,
)
