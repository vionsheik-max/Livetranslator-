package com.livetranslate.core.detector

import com.livetranslate.core.model.TextRegion

enum class ChangeType {
    NEW,
    CHANGED,
    UNCHANGED,
    REMOVED,
}

data class TextRegionChange(
    val type: ChangeType,
    val current: TextRegion? = null,
    val previous: TextRegion? = null,
)
