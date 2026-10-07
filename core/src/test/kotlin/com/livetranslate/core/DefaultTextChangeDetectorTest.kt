package com.livetranslate.core

import com.google.common.truth.Truth.assertThat
import com.livetranslate.core.detector.ChangeType
import com.livetranslate.core.detector.DefaultTextChangeDetector
import com.livetranslate.core.model.DetectedTextFrame
import com.livetranslate.core.model.IntRect
import com.livetranslate.core.model.TextRegion
import org.junit.Test

class DefaultTextChangeDetectorTest {

    private val detector = DefaultTextChangeDetector(
        minimumTextSimilarity = 0.9,
        maximumCenterDistancePx = 100,
    )

    @Test
    fun `marks first frame as new`() {
        val current = frame(region(id = "1", text = "Hola"))

        val changes = detector.detectChanges(previous = null, current = current)

        assertThat(changes).hasSize(1)
        assertThat(changes.single().type).isEqualTo(ChangeType.NEW)
    }

    @Test
    fun `marks identical region as unchanged`() {
        val previous = frame(region(id = "1", text = "Bonjour"))
        val current = frame(region(id = "2", text = "Bonjour"))

        val changes = detector.detectChanges(previous = previous, current = current)

        assertThat(changes.single().type).isEqualTo(ChangeType.UNCHANGED)
    }

    @Test
    fun `marks moved text with changed content as changed`() {
        val previous = frame(region(id = "1", text = "Play"))
        val current = frame(
            region(
                id = "2",
                text = "Pause",
                bounds = IntRect(10, 10, 80, 40),
            ),
        )

        val changes = detector.detectChanges(previous = previous, current = current)

        assertThat(changes.single().type).isEqualTo(ChangeType.CHANGED)
    }

    @Test
    fun `marks unmatched previous region as removed`() {
        val previous = frame(region(id = "1", text = "Settings"))
        val current = frame()

        val changes = detector.detectChanges(previous = previous, current = current)

        assertThat(changes).hasSize(1)
        assertThat(changes.single().type).isEqualTo(ChangeType.REMOVED)
    }

    @Test
    fun `marks far away text as new and removed instead of changed`() {
        val previous = frame(
            region(
                id = "1",
                text = "Start",
                bounds = IntRect(0, 0, 60, 20),
            ),
        )
        val current = frame(
            region(
                id = "2",
                text = "Start",
                bounds = IntRect(500, 500, 560, 520),
            ),
        )

        val changes = detector.detectChanges(previous = previous, current = current)

        assertThat(changes.map { it.type }).containsExactly(ChangeType.NEW, ChangeType.REMOVED)
    }

    private fun frame(vararg regions: TextRegion): DetectedTextFrame =
        DetectedTextFrame(
            timestampMillis = 1L,
            regions = regions.toList(),
        )

    private fun region(
        id: String,
        text: String,
        bounds: IntRect = IntRect(0, 0, 60, 20),
    ): TextRegion = TextRegion(
        id = id,
        text = text,
        bounds = bounds,
    )
}
