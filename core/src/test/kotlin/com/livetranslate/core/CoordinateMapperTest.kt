package com.livetranslate.core

import com.google.common.truth.Truth.assertThat
import com.livetranslate.core.geometry.CoordinateMapper
import com.livetranslate.core.geometry.Size
import com.livetranslate.core.model.IntRect
import org.junit.Test

class CoordinateMapperTest {

    private val mapper = CoordinateMapper()

    @Test
    fun `maps bounds from capture coordinates to display coordinates`() {
        val result = mapper.map(
            sourceBounds = IntRect(100, 50, 300, 150),
            captureSize = Size(width = 1000, height = 500),
            displaySize = Size(width = 2000, height = 1000),
        )

        assertThat(result).isEqualTo(IntRect(200, 100, 600, 300))
    }
}
