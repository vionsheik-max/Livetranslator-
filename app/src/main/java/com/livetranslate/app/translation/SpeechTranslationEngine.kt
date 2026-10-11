package com.livetranslate.app.translation

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.livetranslate.app.domain.model.AppLanguage
import kotlinx.coroutines.tasks.await

class SpeechTranslationEngine {
    private val languageIdentifier = LanguageIdentification.getClient()
    private val translators = mutableMapOf<String, Translator>()

    suspend fun translate(text: String, target: AppLanguage): String {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return ""
        val source = languageIdentifier.identifyLanguage(cleanText).await()
        if (source == "und" || source == target.tag) return cleanText
        val sourceLanguage = TranslateLanguage.fromLanguageTag(source) ?: return cleanText
        val targetLanguage = TranslateLanguage.fromLanguageTag(target.tag) ?: return cleanText
        if (sourceLanguage == targetLanguage) return cleanText
        val key = "$sourceLanguage->$targetLanguage"
        val translator = translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build(),
            )
        }
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        return translator.translate(cleanText).await()
    }

    fun close() {
        languageIdentifier.close()
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
