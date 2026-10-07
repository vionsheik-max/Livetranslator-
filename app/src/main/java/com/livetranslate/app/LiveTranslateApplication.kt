package com.livetranslate.app

import android.app.Application
import com.livetranslate.app.di.AppContainer

class LiveTranslateApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(applicationContext)
    }
}
