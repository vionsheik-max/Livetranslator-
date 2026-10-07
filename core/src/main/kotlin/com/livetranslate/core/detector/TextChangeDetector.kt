package com.livetranslate.core.detector

import com.livetranslate.core.model.DetectedTextFrame

interface TextChangeDetector {
    fun detectChanges(
        previous: DetectedTextFrame?,
        current: DetectedTextFrame,
    ): List<TextRegionChange>
}
