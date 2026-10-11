package com.livetranslate.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.livetranslate.app.presentation.theme.LiveTranslateTheme

class CaptionOverlayController(
    context: Context,
    lifecycleOwner: LifecycleOwner,
) {
    private val windowManager = context.applicationContext.getSystemService(WindowManager::class.java)
    private val caption = mutableStateOf("")
    private var view: ComposeView? = null

    fun show() {
        if (view != null) return
        val overlay = ComposeView(context.applicationContext).apply {
            ViewTreeLifecycleOwner.set(this, lifecycleOwner)
            ViewCompat.setAccessibilityPaneTitle(this, "Live translated captions")
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                LiveTranslateTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        if (caption.value.isNotBlank()) {
                            Text(
                                text = caption.value,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 72.dp)
                                    .background(Color.Black.copy(alpha = 0.42f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        view = overlay
        windowManager.addView(overlay, params)
    }

    fun render(translatedText: String) {
        caption.value = translatedText
        view?.postInvalidate()
    }

    fun remove() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        caption.value = ""
    }
}
