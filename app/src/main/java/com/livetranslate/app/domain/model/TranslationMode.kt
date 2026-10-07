package com.livetranslate.app.domain.model

enum class TranslationMode(
    val title: String,
    val summary: String,
) {
    AUTOMATIC(
        title = "Automatic",
        summary = "Auto-translates new text",
    ),
    ON_DEMAND(
        title = "On Demand",
        summary = "Trigger manually",
    ),
    GAME(
        title = "Game",
        summary = "Fast for games",
    ),
    READING(
        title = "Reading",
        summary = "For static text",
    ),
}
