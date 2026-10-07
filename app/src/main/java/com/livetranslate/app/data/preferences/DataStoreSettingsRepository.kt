package com.livetranslate.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.OverlayPosition
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
                overlayPosition = OverlayPosition(
                    xFraction = (preferences[OVERLAY_X_FRACTION_KEY] ?: DEFAULT_OVERLAY_X_FRACTION)
                        .coerceIn(0f, 1f),
                    yFraction = (preferences[OVERLAY_Y_FRACTION_KEY] ?: DEFAULT_OVERLAY_Y_FRACTION)
                        .coerceIn(0f, 1f),
                ),
                overlayExpanded = preferences[OVERLAY_EXPANDED_KEY] ?: false,
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

    override suspend fun setOverlayPosition(position: OverlayPosition) {
        dataStore.edit {
            it[OVERLAY_X_FRACTION_KEY] = position.xFraction.coerceIn(0f, 1f)
            it[OVERLAY_Y_FRACTION_KEY] = position.yFraction.coerceIn(0f, 1f)
        }
    }

    override suspend fun setOverlayExpanded(expanded: Boolean) {
        dataStore.edit { it[OVERLAY_EXPANDED_KEY] = expanded }
    }

    private companion object {
        const val DATA_STORE_NAME = "live_translate_settings"
        const val DEFAULT_OVERLAY_X_FRACTION = 1f
        const val DEFAULT_OVERLAY_Y_FRACTION = 0.35f

        val TARGET_LANGUAGE_KEY = stringPreferencesKey("target_language")
        val LIVE_TRANSLATION_ENABLED_KEY = booleanPreferencesKey("live_translation_enabled")
        val TRANSLATION_MODE_KEY = stringPreferencesKey("translation_mode")
        val OVERLAY_X_FRACTION_KEY = floatPreferencesKey("overlay_x_fraction")
        val OVERLAY_Y_FRACTION_KEY = floatPreferencesKey("overlay_y_fraction")
        val OVERLAY_EXPANDED_KEY = booleanPreferencesKey("overlay_expanded")
    }
}
