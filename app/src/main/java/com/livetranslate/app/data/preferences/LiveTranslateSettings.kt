package com.livetranslate.app.data.preferences

import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.OverlayPosition
import com.livetranslate.app.domain.model.TranslationMode

data class LiveTranslateSettings(
    val targetLanguage: AppLanguage = AppLanguage.ENGLISH,
    val liveTranslationEnabled: Boolean = false,
    val selectedMode: TranslationMode = TranslationMode.AUTOMATIC,
    val overlayPosition: OverlayPosition = OverlayPosition(),
    val overlayExpanded: Boolean = false,
)
