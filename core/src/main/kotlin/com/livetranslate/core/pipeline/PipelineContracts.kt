package com.livetranslate.core.pipeline

import com.livetranslate.core.model.DetectedTextFrame
import com.livetranslate.core.model.IntRect

interface ScreenCaptureProvider<TFrame> {
    suspend fun capture(): TFrame?
}

interface OcrProvider<TFrame> {
    suspend fun extractText(frame: TFrame): DetectedTextFrame
}

interface LanguageDetector {
    suspend fun detectLanguage(text: String): String?
}

interface TranslationProvider {
    suspend fun translate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String,
    ): String
}

interface TranslationRenderer {
    suspend fun render(overlays: List<RenderedTranslation>)
}

data class RenderedTranslation(
    val bounds: IntRect,
    val translatedText: String,
    val sourceLanguage: String?,
    val targetLanguage: String,
)
