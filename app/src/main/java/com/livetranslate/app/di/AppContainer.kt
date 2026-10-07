package com.livetranslate.app.di

import android.content.Context
import com.livetranslate.app.data.preferences.DataStoreSettingsRepository
import com.livetranslate.app.data.preferences.SettingsRepository

class AppContainer(
    context: Context,
) {
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(
        context = context.applicationContext,
    )
}
