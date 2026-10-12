package com.livetranslate.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.livetranslate.app.presentation.theme.AppTheme

object LauncherIconManager {
    fun apply(context: Context, theme: AppTheme) {
        val packageManager = context.packageManager
        AppTheme.entries.forEach { candidate ->
            val component = ComponentName(context, aliasName(candidate))
            val state = if (candidate == theme) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }

    private fun aliasName(theme: AppTheme): String = when (theme) {
        AppTheme.MIST -> "${BuildConfig.APPLICATION_ID}.LauncherMist"
        AppTheme.OCEAN -> "${BuildConfig.APPLICATION_ID}.LauncherOcean"
        AppTheme.LAVENDER -> "${BuildConfig.APPLICATION_ID}.LauncherLavender"
        AppTheme.SAGE -> "${BuildConfig.APPLICATION_ID}.LauncherSage"
        AppTheme.WARM_SAND -> "${BuildConfig.APPLICATION_ID}.LauncherWarmSand"
        AppTheme.MIDNIGHT -> "${BuildConfig.APPLICATION_ID}.LauncherMidnight"
    }
}
