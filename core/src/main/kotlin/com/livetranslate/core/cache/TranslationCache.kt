package com.livetranslate.core.cache

data class TranslationCacheKey(
    val sourceLanguage: String?,
    val targetLanguage: String,
    val sourceText: String,
)

interface TranslationCache {
    fun get(key: TranslationCacheKey): String?

    fun put(key: TranslationCacheKey, translatedText: String)

    fun clear()
}
