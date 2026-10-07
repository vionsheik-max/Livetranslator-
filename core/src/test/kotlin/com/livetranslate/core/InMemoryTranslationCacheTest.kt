package com.livetranslate.core

import com.google.common.truth.Truth.assertThat
import com.livetranslate.core.cache.InMemoryTranslationCache
import com.livetranslate.core.cache.TranslationCacheKey
import org.junit.Test

class InMemoryTranslationCacheTest {

    @Test
    fun `returns cached translations by full cache key`() {
        val cache = InMemoryTranslationCache(maxEntries = 2)
        val key = TranslationCacheKey(
            sourceLanguage = "ja",
            targetLanguage = "en",
            sourceText = "こんにちは",
        )

        cache.put(key, "Hello")

        assertThat(cache.get(key)).isEqualTo("Hello")
        assertThat(
            cache.get(
                key.copy(targetLanguage = "fr"),
            ),
        ).isNull()
    }

    @Test
    fun `evicts least recently used entry when capacity is exceeded`() {
        val cache = InMemoryTranslationCache(maxEntries = 2)
        val first = TranslationCacheKey("es", "en", "hola")
        val second = TranslationCacheKey("fr", "en", "bonjour")
        val third = TranslationCacheKey("de", "en", "hallo")

        cache.put(first, "hello")
        cache.put(second, "hello")
        cache.get(first)
        cache.put(third, "hello")

        assertThat(cache.get(first)).isEqualTo("hello")
        assertThat(cache.get(second)).isNull()
        assertThat(cache.get(third)).isEqualTo("hello")
    }
}
