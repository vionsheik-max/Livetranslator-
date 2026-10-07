package com.livetranslate.core.cache

class InMemoryTranslationCache(
    private val maxEntries: Int = 256,
) : TranslationCache {

    private val entries = object : LinkedHashMap<TranslationCacheKey, String>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TranslationCacheKey, String>?): Boolean {
            return size > maxEntries
        }
    }

    override fun get(key: TranslationCacheKey): String? = entries[key]

    override fun put(
        key: TranslationCacheKey,
        translatedText: String,
    ) {
        entries[key] = translatedText
    }

    override fun clear() {
        entries.clear()
    }
}
