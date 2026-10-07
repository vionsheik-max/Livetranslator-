package com.livetranslate.core.geometry

import com.livetranslate.core.model.IntRect
import kotlin.math.roundToInt

data class Size(
    val width: Int,
    val height: Int,
)

class CoordinateMapper {
    fun map(
        sourceBounds: IntRect,
        captureSize: Size,
        displaySize: Size,
    ): IntRect {
        require(captureSize.width > 0 && captureSize.height > 0) {
            "Capture size must be positive"
        }
        require(displaySize.width > 0 && displaySize.height > 0) {
            "Display size must be positive"
        }

        val horizontalScale = displaySize.width.toFloat() / captureSize.width.toFloat()
        val verticalScale = displaySize.height.toFloat() / captureSize.height.toFloat()

        return IntRect(
            left = (sourceBounds.left * horizontalScale).roundToInt(),
            top = (sourceBounds.top * verticalScale).roundToInt(),
            right = (sourceBounds.right * horizontalScale).roundToInt(),
            bottom = (sourceBounds.bottom * verticalScale).roundToInt(),
        )
    }
}
