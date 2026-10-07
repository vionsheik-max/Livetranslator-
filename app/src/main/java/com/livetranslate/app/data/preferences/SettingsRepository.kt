package com.livetranslate.app.data.preferences

import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<LiveTranslateSettings>

    suspend fun setTargetLanguage(language: AppLanguage)

    suspend fun setLiveTranslationEnabled(enabled: Boolean)

    suspend fun setMode(mode: TranslationMode)
}
