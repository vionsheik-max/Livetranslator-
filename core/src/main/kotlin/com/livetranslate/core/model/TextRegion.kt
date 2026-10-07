package com.livetranslate.core.model

data class IntRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right >= left) { "right must be >= left" }
        require(bottom >= top) { "bottom must be >= top" }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + (width / 2)
    val centerY: Int get() = top + (height / 2)
}

data class TextRegion(
    val id: String,
    val text: String,
    val bounds: IntRect,
    val confidence: Float? = null,
)

data class DetectedTextFrame(
    val timestampMillis: Long,
    val regions: List<TextRegion>,
)
