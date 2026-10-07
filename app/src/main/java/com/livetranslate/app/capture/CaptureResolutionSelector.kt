package com.livetranslate.app.capture

data class CaptureDimensions(
    val width: Int,
    val height: Int,
    val scaleFactor: Float,
)

class CaptureResolutionSelector(
    private val maxDimension: Int = 1440,
) {
    fun select(
        screenWidth: Int,
        screenHeight: Int,
    ): CaptureDimensions {
        require(screenWidth > 0) { "screenWidth must be positive" }
        require(screenHeight > 0) { "screenHeight must be positive" }

        val longestEdge = maxOf(screenWidth, screenHeight)
        if (longestEdge <= maxDimension) {
            return CaptureDimensions(
                width = screenWidth,
                height = screenHeight,
                scaleFactor = 1f,
            )
        }

        val scaleFactor = maxDimension.toFloat() / longestEdge.toFloat()
        return CaptureDimensions(
            width = (screenWidth * scaleFactor).toInt().coerceAtLeast(1),
            height = (screenHeight * scaleFactor).toInt().coerceAtLeast(1),
            scaleFactor = scaleFactor,
        )
    }
}
