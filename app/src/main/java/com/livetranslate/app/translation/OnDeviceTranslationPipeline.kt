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
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OnDeviceTranslationPipeline(
    private val cache: TranslationCache,
    private val translationProvider: TranslationProvider = MlKitTranslationProvider(),
) {
    private val recognizers: List<TextRecognizer> = listOf(
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
    )
    private val languageIdentifier = LanguageIdentification.getClient()
    private val detector = DefaultTextChangeDetector()
    private val coordinateMapper = CoordinateMapper()
    private val sourceLanguageCache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 512
    }
    private var previousFrame: DetectedTextFrame? = null
    private var lastFingerprint: Long? = null
    private var lastTargetLanguage: String? = null
    private var lastRendered: List<RenderedTranslation> = emptyList()

    suspend fun process(
        bitmap: Bitmap,
        captureWidth: Int,
        captureHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
        targetLanguage: AppLanguage,
    ): List<RenderedTranslation> {
        val fingerprint = bitmapFingerprint(bitmap)
        if (fingerprint == lastFingerprint && targetLanguage.tag == lastTargetLanguage) {
            return lastRendered
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        val blocks = coroutineScope {
            recognizers.map { recognizer ->
                async {
                    // Optional script models can be unavailable on a device. Keep the
                    // bundled Latin recognizer and every other healthy recognizer alive.
                    runCatching { recognizer.process(image).await().textBlocks }
                        .getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }
        val regions = blocks.flatMapIndexed { blockIndex, block ->
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
            .distinctBy { region ->
                val normalized = region.text.lowercase().replace(Regex("\\s+"), " ").trim()
                "$normalized:${region.bounds.left}:${region.bounds.top}:${region.bounds.right}:${region.bounds.bottom}"
            }
        val frame = DetectedTextFrame(System.currentTimeMillis(), regions)
        val changes = detector.detectChanges(previousFrame, frame)
        previousFrame = frame
        val changed = changes.filter { it.current != null && it.type != com.livetranslate.core.detector.ChangeType.REMOVED }
        val sourceTags = ConcurrentHashMap<String, String>()
        coroutineScope {
            changed.mapNotNull { it.current }
                .distinctBy { it.text }
                .map { region ->
                    async {
                        val sourceTag = identifySource(region.text)
                        if (sourceTag != null) sourceTags[region.text] = sourceTag
                        if (sourceTag == null) return@async
                        if (sourceTag == targetLanguage.tag || sourceTag == "und") return@async
                        val key = TranslationCacheKey(sourceTag, targetLanguage.tag, region.text)
                        if (cache.get(key) == null) {
                            runCatching {
                                translationProvider.translate(region.text, sourceTag, targetLanguage.tag)
                            }.onSuccess { cache.put(key, it) }
                        }
                    }
                }
                .awaitAll()
        }
        val rendered = frame.regions.mapNotNull { region ->
            val sourceTag = sourceTags[region.text] ?: identifySource(region.text)
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
        lastFingerprint = fingerprint
        lastTargetLanguage = targetLanguage.tag
        lastRendered = rendered
        return rendered
    }

    fun reset() {
        previousFrame = null
        lastFingerprint = null
        lastTargetLanguage = null
        lastRendered = emptyList()
        cache.clear()
        synchronized(sourceLanguageCache) { sourceLanguageCache.clear() }
    }

    fun close() {
        recognizers.forEach { it.close() }
        languageIdentifier.close()
    }

    private suspend fun identifySource(text: String): String? {
        synchronized(sourceLanguageCache) {
            sourceLanguageCache[text]?.let { return it }
        }
        val identified = runCatching { languageIdentifier.identifyLanguage(text).await() }.getOrNull()
        if (identified != null) {
            synchronized(sourceLanguageCache) { sourceLanguageCache[text] = identified }
        }
        return identified
    }

    /** A cheap visual fingerprint avoids running five OCR models on unchanged frames. */
    private fun bitmapFingerprint(bitmap: Bitmap): Long {
        val sampleWidth = 24
        val sampleHeight = 14
        var hash = -3750763034362895579L
        for (y in 0 until sampleHeight) {
            val sourceY = (y * bitmap.height / sampleHeight).coerceAtMost(bitmap.height - 1)
            for (x in 0 until sampleWidth) {
                val sourceX = (x * bitmap.width / sampleWidth).coerceAtMost(bitmap.width - 1)
                val pixel = bitmap.getPixel(sourceX, sourceY)
                hash = (hash xor (pixel.toLong() and 0x00ffffffL)) * 0x100000001b3L
            }
        }
        return hash xor (bitmap.width.toLong() shl 32) xor bitmap.height.toLong()
    }
}

private class MlKitTranslationProvider : TranslationProvider {
    private val translators = ConcurrentHashMap<String, Translator>()
    private val modelLocks = ConcurrentHashMap<String, Mutex>()
    private val readyModels = ConcurrentHashMap.newKeySet<String>()

    override suspend fun translate(text: String, sourceLanguage: String?, targetLanguage: String): String {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage ?: "") ?: return text
        val target = TranslateLanguage.fromLanguageTag(targetLanguage) ?: return text
        if (source == target) return text
        val key = "$source->$target"
        val translator = translators.computeIfAbsent(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build(),
            )
        }
        if (!readyModels.contains(key)) {
            modelLocks.computeIfAbsent(key) { Mutex() }.withLock {
                if (readyModels.add(key)) {
                    runCatching {
                        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                    }.onFailure { readyModels.remove(key) }.getOrThrow()
                }
            }
        }
        return translator.translate(text).await()
    }
}
