package com.livetranslate.core.detector

import com.livetranslate.core.model.DetectedTextFrame
import com.livetranslate.core.model.IntRect
import com.livetranslate.core.model.TextRegion
import kotlin.math.abs
import kotlin.math.max

class DefaultTextChangeDetector(
    private val minimumTextSimilarity: Double = 0.82,
    private val maximumCenterDistancePx: Int = 72,
) : TextChangeDetector {

    override fun detectChanges(
        previous: DetectedTextFrame?,
        current: DetectedTextFrame,
    ): List<TextRegionChange> {
        if (previous == null || previous.regions.isEmpty()) {
            return current.regions.map { region ->
                TextRegionChange(type = ChangeType.NEW, current = region)
            }
        }

        val remainingPrevious = previous.regions.toMutableList()
        val changes = mutableListOf<TextRegionChange>()

        current.regions.forEach { currentRegion ->
            val bestMatch = remainingPrevious
                .map { candidate -> candidate to score(currentRegion, candidate) }
                .maxByOrNull { it.second }

            if (bestMatch == null || bestMatch.second <= 0.0) {
                changes += TextRegionChange(type = ChangeType.NEW, current = currentRegion)
                return@forEach
            }

            val previousRegion = bestMatch.first
            remainingPrevious.remove(previousRegion)

            val similarity = textSimilarity(currentRegion.text, previousRegion.text)
            val type = if (similarity >= minimumTextSimilarity) {
                ChangeType.UNCHANGED
            } else {
                ChangeType.CHANGED
            }

            changes += TextRegionChange(
                type = type,
                current = currentRegion,
                previous = previousRegion,
            )
        }

        remainingPrevious.forEach { removedRegion ->
            changes += TextRegionChange(type = ChangeType.REMOVED, previous = removedRegion)
        }

        return changes
    }

    private fun score(
        current: TextRegion,
        previous: TextRegion,
    ): Double {
        val distance = centerDistance(current.bounds, previous.bounds)
        if (distance > maximumCenterDistancePx) {
            return -1.0
        }

        val similarity = textSimilarity(current.text, previous.text)
        return similarity + (1.0 - (distance.toDouble() / maximumCenterDistancePx.toDouble()))
    }

    private fun centerDistance(
        first: IntRect,
        second: IntRect,
    ): Int {
        return abs(first.centerX - second.centerX) + abs(first.centerY - second.centerY)
    }

    private fun textSimilarity(
        currentText: String,
        previousText: String,
    ): Double {
        if (currentText == previousText) {
            return 1.0
        }
        if (currentText.isBlank() || previousText.isBlank()) {
            return 0.0
        }

        val distance = levenshtein(currentText, previousText)
        val maxLength = max(currentText.length, previousText.length)
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    private fun levenshtein(
        first: String,
        second: String,
    ): Int {
        val previous = IntArray(second.length + 1) { it }
        val current = IntArray(second.length + 1)

        first.forEachIndexed { firstIndex, firstChar ->
            current[0] = firstIndex + 1
            second.forEachIndexed { secondIndex, secondChar ->
                val substitutionCost = if (firstChar == secondChar) 0 else 1
                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + substitutionCost,
                )
            }
            current.copyInto(previous)
        }

        return previous[second.length]
    }
}
