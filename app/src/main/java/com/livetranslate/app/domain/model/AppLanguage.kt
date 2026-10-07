package com.livetranslate.app.domain.model

enum class AppLanguage(
    val tag: String,
    val displayName: String,
) {
    ENGLISH("en", "English"),
    SPANISH("es", "Spanish"),
    FRENCH("fr", "French"),
    GERMAN("de", "German"),
    JAPANESE("ja", "Japanese"),
    KOREAN("ko", "Korean"),
    CHINESE_SIMPLIFIED("zh-CN", "Chinese (Simplified)"),
    PORTUGUESE("pt", "Portuguese");

    companion object {
        fun fromTag(tag: String): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
