package com.livetranslate.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.livetranslate.app.presentation.theme.LiveTranslateTheme
import com.livetranslate.core.pipeline.RenderedTranslation
import kotlin.math.roundToInt

class TranslationTextOverlayController(
    context: Context,
    lifecycleOwner: LifecycleOwner,
) {
    private val appContext = context.applicationContext
    private val lifecycleOwner = lifecycleOwner
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val translations = mutableStateOf<List<RenderedTranslation>>(emptyList())
    private var view: ComposeView? = null

    fun show() {
        if (view != null) return
        val overlay = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            ViewCompat.setAccessibilityPaneTitle(this, "Live translation text")
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                LiveTranslateTheme { TranslationTextLayer(translations.value) }
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

    fun render(next: List<RenderedTranslation>) {
        translations.value = next
        view?.postInvalidate()
    }

    fun remove() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        translations.value = emptyList()
    }
}

@Composable
private fun TranslationTextLayer(items: List<RenderedTranslation>) {
    Box(Modifier.fillMaxSize()) {
        items.forEach { item ->
            Text(
                text = item.translatedText,
                modifier = Modifier
                    .offset { IntOffset(item.bounds.left, item.bounds.top) }
                    .size(
                        width = item.bounds.width.coerceAtLeast(1).dp,
                        height = item.bounds.height.coerceAtLeast(1).dp,
                    ),
                color = Color.White,
                fontSize = item.bounds.height.coerceAtLeast(10).sp,
                fontWeight = FontWeight.Medium,
                lineHeight = TextUnit.Unspecified,
                maxLines = 3,
            )
        }
    }
}
