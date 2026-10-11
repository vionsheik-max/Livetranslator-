package com.livetranslate.app.translation

import android.graphics.Bitmap
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.core.cache.TranslationCache
import com.livetranslate.core.cache.TranslationCacheKey
import com.livetranslate.core.detector.DefaultTextChangeDetector
import com.livetranslate.core.geometry.CoordinateMapper
import com.livetranslate.core.geometry.Size
import com.livetranslate.core.model.DetectedTextFrame
import com.livetranslate.core.model.IntRect
import com.livetranslate.core.model.TextRegion
import com.livetranslate.core.pipeline.RenderedTranslation
import com.livetranslate.core.pipeline.TranslationProvider
import kotlinx.coroutines.tasks.await

class OnDeviceTranslationPipeline(
    private val cache: TranslationCache,
    private val translationProvider: TranslationProvider = MlKitTranslationProvider(),
) {
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS,
    )
    private val languageIdentifier = LanguageIdentification.getClient()
    private val detector = DefaultTextChangeDetector()
    private val coordinateMapper = CoordinateMapper()
    private var previousFrame: DetectedTextFrame? = null

    suspend fun process(
        bitmap: Bitmap,
        captureWidth: Int,
        captureHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
        targetLanguage: AppLanguage,
    ): List<RenderedTranslation> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).await()
        val regions = text.textBlocks.flatMapIndexed { blockIndex, block ->
            block.lines.mapIndexed { lineIndex, line ->
                val bounds = line.boundingBox ?: block.boundingBox
                TextRegion(
                    id = "$blockIndex:$lineIndex:${line.text.hashCode()}",
                    text = line.text.trim(),
                    bounds = IntRect(
                        left = bounds?.left ?: 0,
                        top = bounds?.top ?: 0,
                        right = bounds?.right ?: 0,
                        bottom = bounds?.bottom ?: 0,
                    ),
                )
            }
        }.filter { it.text.isNotBlank() && it.bounds.width > 0 && it.bounds.height > 0 }
        val frame = DetectedTextFrame(System.currentTimeMillis(), regions)
        val changes = detector.detectChanges(previousFrame, frame)
        previousFrame = frame
        val changed = changes.filter { it.current != null && it.type != com.livetranslate.core.detector.ChangeType.REMOVED }
        changed.forEach { change ->
            val region = change.current ?: return@forEach
            val sourceTag = runCatching { languageIdentifier.identifyLanguage(region.text).await() }.getOrNull()
            if (sourceTag == targetLanguage.tag || sourceTag == "und") return@forEach
            val key = TranslationCacheKey(sourceTag, targetLanguage.tag, region.text)
            if (cache.get(key) == null) {
                runCatching {
                    cache.put(key, translationProvider.translate(region.text, sourceTag, targetLanguage.tag))
                }
            }
        }
        return frame.regions.mapNotNull { region ->
            val sourceTag = runCatching { languageIdentifier.identifyLanguage(region.text).await() }.getOrNull()
            if (sourceTag == null || sourceTag == targetLanguage.tag || sourceTag == "und") return@mapNotNull null
            val translated = cache.get(TranslationCacheKey(sourceTag, targetLanguage.tag, region.text))
                ?: return@mapNotNull null
            RenderedTranslation(
                bounds = coordinateMapper.map(
                    sourceBounds = region.bounds,
                    captureSize = Size(captureWidth, captureHeight),
                    displaySize = Size(displayWidth, displayHeight),
                ),
                translatedText = translated,
                sourceLanguage = sourceTag,
                targetLanguage = targetLanguage.tag,
            )
        }
    }

    fun reset() {
        previousFrame = null
        cache.clear()
    }

    fun close() {
        recognizer.close()
        languageIdentifier.close()
    }
}

private class MlKitTranslationProvider : TranslationProvider {
    private val translators = mutableMapOf<String, Translator>()

    override suspend fun translate(text: String, sourceLanguage: String?, targetLanguage: String): String {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage ?: "") ?: return text
        val target = TranslateLanguage.fromLanguageTag(targetLanguage) ?: return text
        if (source == target) return text
        val key = "$source->$target"
        val translator = translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build(),
            )
        }
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        return translator.translate(text).await()
    }
}
