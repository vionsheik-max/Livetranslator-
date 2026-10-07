package com.livetranslate.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    context: Context,
) : SettingsRepository {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) },
    )

    override val settings: Flow<LiveTranslateSettings> = dataStore.data
        .catch { exception ->
            if (exception !is IOException) {
                throw exception
            }
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { preferences ->
            LiveTranslateSettings(
                targetLanguage = AppLanguage.fromTag(
                    preferences[TARGET_LANGUAGE_KEY] ?: AppLanguage.ENGLISH.tag,
                ),
                liveTranslationEnabled = preferences[LIVE_TRANSLATION_ENABLED_KEY] ?: false,
                selectedMode = preferences[TRANSLATION_MODE_KEY]
                    ?.let { runCatching { TranslationMode.valueOf(it) }.getOrNull() }
                    ?: TranslationMode.AUTOMATIC,
            )
        }

    override suspend fun setTargetLanguage(language: AppLanguage) {
        dataStore.edit { it[TARGET_LANGUAGE_KEY] = language.tag }
    }

    override suspend fun setLiveTranslationEnabled(enabled: Boolean) {
        dataStore.edit { it[LIVE_TRANSLATION_ENABLED_KEY] = enabled }
    }

    override suspend fun setMode(mode: TranslationMode) {
        dataStore.edit { it[TRANSLATION_MODE_KEY] = mode.name }
    }

    private companion object {
        const val DATA_STORE_NAME = "live_translate_settings"

        val TARGET_LANGUAGE_KEY = stringPreferencesKey("target_language")
        val LIVE_TRANSLATION_ENABLED_KEY = booleanPreferencesKey("live_translation_enabled")
        val TRANSLATION_MODE_KEY = stringPreferencesKey("translation_mode")
    }
}
