package com.livetranslate.app.presentation.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val primary: Color,
    val secondary: Color,
    val glass: Color,
) {
    MIST("mist", "Mist", "Soft neutral", Color(0xFF52666B), Color(0xFF26353D), Color(0xFFE5F1EF), Color(0xFFB9D8D6), Color(0x66DCEBE8)),
    OCEAN("ocean", "Ocean", "Cool blue", Color(0xFF426B87), Color(0xFF1D3343), Color(0xFFB7E2F4), Color(0xFF8BC4DF), Color(0x6694C7DF)),
    LAVENDER("lavender", "Lavender", "Gentle purple", Color(0xFF756B8C), Color(0xFF302B43), Color(0xFFE5D6FF), Color(0xFFC7B1EC), Color(0x66D3C2ED)),
    SAGE("sage", "Sage", "Calming green", Color(0xFF667B6F), Color(0xFF2B3C35), Color(0xFFD8EAD6), Color(0xFFB4D1B6), Color(0x6692B99D)),
    WARM_SAND("warm_sand", "Warm Sand", "Warm beige", Color(0xFF927F72), Color(0xFF443B39), Color(0xFFFFE2C7), Color(0xFFE5BFA0), Color(0x66E7C6AA)),
    MIDNIGHT("midnight", "Midnight", "Dark & calm", Color(0xFF26374C), Color(0xFF111A29), Color(0xFFBFD7FF), Color(0xFF88A9D7), Color(0x667891B5)),
    ;

    companion object {
        fun fromId(id: String): AppTheme = entries.firstOrNull { it.id == id } ?: MIST
    }
}
