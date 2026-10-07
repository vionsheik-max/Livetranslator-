package com.livetranslate.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.OverlayPosition
import com.livetranslate.app.domain.model.TranslationMode
import com.livetranslate.app.presentation.theme.DeepOcean
import com.livetranslate.app.presentation.theme.ElectricBlue
import com.livetranslate.app.presentation.theme.LiveTranslateTheme
import com.livetranslate.app.presentation.theme.OverlayBorder
import com.livetranslate.app.presentation.theme.OverlayGlass
import com.livetranslate.app.presentation.theme.SoftMint
import com.livetranslate.core.geometry.NormalizedPoint
import com.livetranslate.core.geometry.OverlayBoundsCalculator
import com.livetranslate.core.geometry.ScreenPoint
import com.livetranslate.core.geometry.ScreenSize
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayWindowController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val boundsCalculator: OverlayBoundsCalculator = OverlayBoundsCalculator(),
    private val onPositionChanged: (OverlayPosition) -> Unit,
    private val onExpandedChanged: (Boolean) -> Unit,
    private val onLiveTranslationToggled: (Boolean) -> Unit,
    private val onTargetLanguageSelected: (AppLanguage) -> Unit,
    private val onModeSelected: (TranslationMode) -> Unit,
    private val onOpenApp: () -> Unit,
    private val onStopSession: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val uiState = MutableStateFlow(OverlayUiState())

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var requestedPosition: OverlayPosition = OverlayPosition()

    fun show() {
        if (composeView != null) {
            composeView?.post { restorePositionFromSettings() }
            return
        }

        val overlayView = ComposeView(context).apply {
            ViewTreeLifecycleOwner.set(this, lifecycleOwner)
            ViewCompat.setAccessibilityPaneTitle(this, "Live Translate overlay")
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                LiveTranslateTheme {
                    val state by uiState.collectAsState()
                    OverlayContent(
                        uiState = state,
                        onToggleExpanded = {
                            onExpandedChanged(!state.isExpanded)
                        },
                        onToggleLiveTranslation = onLiveTranslationToggled,
                        onCycleLanguage = {
                            onTargetLanguageSelected(state.targetLanguage.next())
                        },
                        onCycleMode = {
                            onModeSelected(state.selectedMode.next())
                        },
                        onOpenApp = onOpenApp,
                        onClose = onStopSession,
                        onDrag = ::moveBy,
                    )
                }
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                clampWithinBoundsAndPersist()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        composeView = overlayView
        layoutParams = params
        windowManager.addView(overlayView, params)
        overlayView.post { restorePositionFromSettings() }
    }

    fun update(
        state: OverlayUiState,
        overlayPosition: OverlayPosition,
    ) {
        uiState.value = state
        requestedPosition = overlayPosition
        composeView?.post { restorePositionFromSettings() }
    }

    fun remove() {
        val overlayView = composeView ?: return
        composeView = null
        layoutParams = null
        runCatching {
            windowManager.removeView(overlayView)
        }
    }

    private fun moveBy(
        deltaX: Int,
        deltaY: Int,
    ) {
        val params = layoutParams ?: return
        applyPosition(
            ScreenPoint(
                x = params.x + deltaX,
                y = params.y + deltaY,
            ),
            persist = true,
        )
    }

    private fun restorePositionFromSettings() {
        val view = composeView ?: return
        if (view.width == 0 || view.height == 0) {
            view.post { restorePositionFromSettings() }
            return
        }

        val restored = boundsCalculator.fromNormalized(
            normalizedPoint = NormalizedPoint(
                xFraction = requestedPosition.xFraction,
                yFraction = requestedPosition.yFraction,
            ),
            overlaySize = overlaySize(view),
            screenSize = currentScreenSize(),
        )
        applyPosition(restored, persist = false)
    }

    private fun clampWithinBoundsAndPersist() {
        val params = layoutParams ?: return
        applyPosition(
            position = ScreenPoint(x = params.x, y = params.y),
            persist = true,
        )
    }

    private fun applyPosition(
        position: ScreenPoint,
        persist: Boolean,
    ) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val clamped = boundsCalculator.clamp(
            position = position,
            overlaySize = overlaySize(view),
            screenSize = currentScreenSize(),
        )
        if (params.x != clamped.x || params.y != clamped.y) {
            params.x = clamped.x
            params.y = clamped.y
            runCatching {
                windowManager.updateViewLayout(view, params)
            }
        }
        if (persist) {
            val normalized = boundsCalculator.toNormalized(
                position = clamped,
                overlaySize = overlaySize(view),
                screenSize = currentScreenSize(),
            )
            onPositionChanged(
                OverlayPosition(
                    xFraction = normalized.xFraction,
                    yFraction = normalized.yFraction,
                ),
            )
        }
    }

    private fun overlaySize(view: ComposeView): ScreenSize = ScreenSize(
        width = view.width.coerceAtLeast(view.measuredWidth).coerceAtLeast(1),
        height = view.height.coerceAtLeast(view.measuredHeight).coerceAtLeast(1),
    )

    private fun currentScreenSize(): ScreenSize {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            ScreenSize(
                width = bounds.width(),
                height = bounds.height(),
            )
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            ScreenSize(
                width = displayMetrics.widthPixels,
                height = displayMetrics.heightPixels,
            )
        }
    }
}

@Composable
private fun OverlayContent(
    uiState: OverlayUiState,
    onToggleExpanded: () -> Unit,
    onToggleLiveTranslation: (Boolean) -> Unit,
    onCycleLanguage: () -> Unit,
    onCycleMode: () -> Unit,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Int, Int) -> Unit,
) {
    if (uiState.isExpanded) {
        ExpandedOverlay(
            uiState = uiState,
            onToggleExpanded = onToggleExpanded,
            onToggleLiveTranslation = onToggleLiveTranslation,
            onCycleLanguage = onCycleLanguage,
            onCycleMode = onCycleMode,
            onOpenApp = onOpenApp,
            onClose = onClose,
            onDrag = onDrag,
        )
    } else {
        CollapsedOverlay(
            onTap = onToggleExpanded,
            onDrag = onDrag,
        )
    }
}

@Composable
private fun CollapsedOverlay(
    onTap: () -> Unit,
    onDrag: (Int, Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                }
            },
        shape = CircleShape,
        color = OverlayGlass,
        border = BorderStroke(1.dp, OverlayBorder),
        shadowElevation = 18.dp,
        onClick = onTap,
    ) {
        Box(
            modifier = Modifier.background(
                Brush.radialGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = 0.32f),
                        SoftMint.copy(alpha = 0.18f),
                        DeepOcean.copy(alpha = 0.28f),
                    ),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "LT",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ExpandedOverlay(
    uiState: OverlayUiState,
    onToggleExpanded: () -> Unit,
    onToggleLiveTranslation: (Boolean) -> Unit,
    onCycleLanguage: () -> Unit,
    onCycleMode: () -> Unit,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Int, Int) -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 320.dp),
        shape = RoundedCornerShape(26.dp),
        color = OverlayGlass,
        border = BorderStroke(1.dp, OverlayBorder),
        shadowElevation = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.05f),
                            DeepOcean.copy(alpha = 0.32f),
                        ),
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Live Translate",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Floating controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenApp) {
                        Text("App")
                    }
                    OutlinedButton(onClick = onToggleExpanded) {
                        Text("Min")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Live Translation",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = if (uiState.liveTranslationEnabled) {
                            "Overlay session is active"
                        } else {
                            "Session is paused"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.liveTranslationEnabled,
                    onCheckedChange = onToggleLiveTranslation,
                )
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCycleLanguage,
            ) {
                Text("Target: ${uiState.targetLanguage.displayName}")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCycleMode,
            ) {
                Text("Mode: ${uiState.selectedMode.title}")
            }

            Text(
                text = "Capture, OCR, and in-place translation rendering continue in the next milestone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose,
            ) {
                Text("Close session")
            }
        }
    }
}

private fun AppLanguage.next(): AppLanguage {
    val languages = AppLanguage.entries
    val currentIndex = languages.indexOf(this)
    return languages[(currentIndex + 1) % languages.size]
}

private fun TranslationMode.next(): TranslationMode {
    val modes = TranslationMode.entries
    val currentIndex = modes.indexOf(this)
    return modes[(currentIndex + 1) % modes.size]
}
