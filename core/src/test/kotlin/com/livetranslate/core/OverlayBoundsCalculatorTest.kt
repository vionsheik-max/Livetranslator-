package com.livetranslate.core

import com.google.common.truth.Truth.assertThat
import com.livetranslate.core.geometry.NormalizedPoint
import com.livetranslate.core.geometry.OverlayBoundsCalculator
import com.livetranslate.core.geometry.ScreenPoint
import com.livetranslate.core.geometry.ScreenSize
import org.junit.Test

class OverlayBoundsCalculatorTest {

    private val calculator = OverlayBoundsCalculator(edgePaddingPx = 24)
    private val screenSize = ScreenSize(width = 1080, height = 1920)
    private val overlaySize = ScreenSize(width = 280, height = 160)

    @Test
    fun `clamps overlay inside screen padding`() {
        val clamped = calculator.clamp(
            position = ScreenPoint(x = 2000, y = -500),
            overlaySize = overlaySize,
            screenSize = screenSize,
        )

        assertThat(clamped).isEqualTo(ScreenPoint(x = 776, y = 24))
    }

    @Test
    fun `restores position from normalized fractions`() {
        val point = calculator.fromNormalized(
            normalizedPoint = NormalizedPoint(xFraction = 1f, yFraction = 0.5f),
            overlaySize = overlaySize,
            screenSize = screenSize,
        )

        assertThat(point).isEqualTo(ScreenPoint(x = 776, y = 892))
    }

    @Test
    fun `converts clamped point back to normalized fractions`() {
        val normalized = calculator.toNormalized(
            position = ScreenPoint(x = 900, y = 892),
            overlaySize = overlaySize,
            screenSize = screenSize,
        )

        assertThat(normalized.xFraction).isWithin(0.001f).of(1f)
        assertThat(normalized.yFraction).isWithin(0.001f).of(0.5f)
    }

    @Test
    fun `handles small screens without negative bounds`() {
        val clamped = calculator.clamp(
            position = ScreenPoint(x = 200, y = 200),
            overlaySize = ScreenSize(width = 300, height = 200),
            screenSize = ScreenSize(width = 240, height = 180),
        )

        assertThat(clamped).isEqualTo(ScreenPoint(x = 0, y = 0))
    }
}
