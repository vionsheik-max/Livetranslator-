package com.livetranslate.app.overlay

import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode

data class OverlayUiState(
    val liveTranslationEnabled: Boolean = false,
    val targetLanguage: AppLanguage = AppLanguage.ENGLISH,
    val selectedMode: TranslationMode = TranslationMode.AUTOMATIC,
    val isExpanded: Boolean = false,
)
