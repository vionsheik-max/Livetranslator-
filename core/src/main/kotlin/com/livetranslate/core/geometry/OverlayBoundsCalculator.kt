package com.livetranslate.core.geometry

import kotlin.math.max
import kotlin.math.roundToInt

data class ScreenPoint(
    val x: Int,
    val y: Int,
)

data class ScreenSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width >= 0) { "width must be >= 0" }
        require(height >= 0) { "height must be >= 0" }
    }
}

data class NormalizedPoint(
    val xFraction: Float,
    val yFraction: Float,
) {
    init {
        require(xFraction.isFinite()) { "xFraction must be finite" }
        require(yFraction.isFinite()) { "yFraction must be finite" }
    }
}

class OverlayBoundsCalculator(
    private val edgePaddingPx: Int = 16,
) {

    fun clamp(
        position: ScreenPoint,
        overlaySize: ScreenSize,
        screenSize: ScreenSize,
    ): ScreenPoint = ScreenPoint(
        x = clampAxis(
            rawValue = position.x,
            overlayAxis = overlaySize.width,
            screenAxis = screenSize.width,
        ),
        y = clampAxis(
            rawValue = position.y,
            overlayAxis = overlaySize.height,
            screenAxis = screenSize.height,
        ),
    )

    fun fromNormalized(
        normalizedPoint: NormalizedPoint,
        overlaySize: ScreenSize,
        screenSize: ScreenSize,
    ): ScreenPoint {
        val xRange = axisRange(
            overlayAxis = overlaySize.width,
            screenAxis = screenSize.width,
        )
        val yRange = axisRange(
            overlayAxis = overlaySize.height,
            screenAxis = screenSize.height,
        )

        return ScreenPoint(
            x = xRange.first + ((xRange.last - xRange.first) * normalizedPoint.xFraction.coerceIn(0f, 1f)).roundToInt(),
            y = yRange.first + ((yRange.last - yRange.first) * normalizedPoint.yFraction.coerceIn(0f, 1f)).roundToInt(),
        )
    }

    fun toNormalized(
        position: ScreenPoint,
        overlaySize: ScreenSize,
        screenSize: ScreenSize,
    ): NormalizedPoint {
        val clamped = clamp(
            position = position,
            overlaySize = overlaySize,
            screenSize = screenSize,
        )
        val xRange = axisRange(
            overlayAxis = overlaySize.width,
            screenAxis = screenSize.width,
        )
        val yRange = axisRange(
            overlayAxis = overlaySize.height,
            screenAxis = screenSize.height,
        )

        return NormalizedPoint(
            xFraction = normalizeAxis(clamped.x, xRange.first, xRange.last),
            yFraction = normalizeAxis(clamped.y, yRange.first, yRange.last),
        )
    }

    private fun normalizeAxis(
        value: Int,
        min: Int,
        max: Int,
    ): Float {
        if (max <= min) {
            return 0f
        }
        return ((value - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
    }

    private fun clampAxis(
        rawValue: Int,
        overlayAxis: Int,
        screenAxis: Int,
    ): Int {
        val range = axisRange(
            overlayAxis = overlayAxis,
            screenAxis = screenAxis,
        )
        return rawValue.coerceIn(range.first, range.last)
    }

    private fun axisRange(
        overlayAxis: Int,
        screenAxis: Int,
    ): IntRange {
        val safePadding = max(0, edgePaddingPx)
        val maxValue = max(0, screenAxis - overlayAxis - safePadding)
        val minValue = minOf(safePadding, maxValue)
        return minValue..maxValue
    }
}
