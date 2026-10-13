package com.livetranslate.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.livetranslate.app.capture.CaptureSessionState
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import com.livetranslate.app.diagnostics.TranslationRuntimeState
import com.livetranslate.app.presentation.theme.AppTheme

private enum class AppTab { HOME, LANGUAGES, SETTINGS }

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    overlayPermissionGranted: Boolean,
    captureSessionState: CaptureSessionState,
    runtimeState: TranslationRuntimeState = TranslationRuntimeState.Idle,
    onTargetLanguageSelected: (AppLanguage) -> Unit,
    onLiveTranslationToggled: (Boolean) -> Unit,
    onVoiceInterpreterToggled: (Boolean) -> Unit,
    onModeSelected: (TranslationMode) -> Unit,
    onDownloadLanguagePack: (AppLanguage) -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
    onOrbSizeChanged: (Float) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit,
    isPro: Boolean = false,
    proPrice: String? = null,
    onUpgrade: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    val theme = uiState.theme
    Box(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(theme.backgroundTop, theme.backgroundBottom)),
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    AppTab.HOME -> HomeTab(uiState, overlayPermissionGranted, captureSessionState, runtimeState, onTargetLanguageSelected, onLiveTranslationToggled, onVoiceInterpreterToggled, onModeSelected, onRequestOverlayPermission, onRequestScreenCapturePermission)
                    AppTab.LANGUAGES -> LanguagesTab(uiState, onTargetLanguageSelected, onDownloadLanguagePack)
                    AppTab.SETTINGS -> SettingsTab(uiState, onThemeSelected, onOrbSizeChanged, isPro, proPrice, onUpgrade)
                }
            }
            BottomBar(tab = tab, theme = theme, onTabSelected = { tab = it })
        }
    }
}

@Composable
private fun HomeTab(
    uiState: HomeUiState,
    overlayReady: Boolean,
    captureState: CaptureSessionState,
    runtimeState: TranslationRuntimeState,
    onTargetLanguageSelected: (AppLanguage) -> Unit,
    onLiveTranslationToggled: (Boolean) -> Unit,
    onVoiceInterpreterToggled: (Boolean) -> Unit,
    onModeSelected: (TranslationMode) -> Unit,
    onOverlayPermission: () -> Unit,
    onCapturePermission: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ReferenceHeader(title = "Live Translator", subtitle = "Break language barriers")
        LanguagePairRow(uiState.targetLanguage, uiState.availableLanguages, onTargetLanguageSelected)
        LiveTranslationHero(uiState, onLiveTranslationToggled)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureTile(Icons.Rounded.ScreenShare, "Screen\nTranslation", "Translate text on screen", Modifier.weight(1f), uiState.liveTranslationEnabled, uiState.theme.primary)
            FeatureTile(Icons.Rounded.Mic, "Voice\nInterpreter", "Two-way conversation", Modifier.weight(1f), uiState.voiceInterpreterEnabled, uiState.theme.secondary)
        }
        FeatureTile(Icons.Rounded.PlayArrow, "Video Captions", "Translated captions for videos", Modifier.fillMaxWidth(), false, uiState.theme.secondary)
        SessionStatus(overlayReady, captureState, runtimeState, onOverlayPermission, onCapturePermission)
        ModeRow(uiState.selectedMode, uiState.availableModes, onModeSelected)
    }
}

@Composable
private fun LiveTranslationHero(uiState: HomeUiState, onToggle: (Boolean) -> Unit) {
    val theme = uiState.theme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = theme.backgroundBottom.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.primary.copy(alpha = 0.22f)),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.background(Brush.linearGradient(listOf(theme.glass, Color.Transparent))).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = theme.primary.copy(alpha = 0.18f), border = androidx.compose.foundation.BorderStroke(1.dp, theme.primary.copy(alpha = 0.30f)), modifier = Modifier.size(66.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Translate, null, tint = theme.primary, modifier = Modifier.size(34.dp)) }
                }
                Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("Live translation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (uiState.liveTranslationEnabled) "Watching your screen now" else "Translate text wherever you are", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = if (uiState.liveTranslationEnabled) theme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.size(12.dp)) {}
            }
            Button(onClick = { onToggle(!uiState.liveTranslationEnabled) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(if (uiState.liveTranslationEnabled) "Stop translation" else "Start live translation")
            }
            Text("Private on-device processing • Floating translations stay aligned to the screen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LanguagesTab(
    uiState: HomeUiState,
    onTargetLanguageSelected: (AppLanguage) -> Unit,
    onDownload: (AppLanguage) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ReferenceHeader(title = "Language Manager", subtitle = "Download packs for offline translation")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = true, onClick = {}, label = { Text("All") })
            FilterChip(selected = false, onClick = {}, label = { Text("Downloaded") })
            FilterChip(selected = false, onClick = {}, label = { Text("Online") })
        }
        uiState.availableLanguages.forEach { language ->
            LanguagePackRow(language, language == uiState.targetLanguage, uiState.languagePackState, onTargetLanguageSelected, onDownload)
        }
        Text("OCR support is optimized for Latin, Chinese, Japanese, Korean, and Devanagari scripts.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsTab(uiState: HomeUiState, onThemeSelected: (AppTheme) -> Unit, onOrbSizeChanged: (Float) -> Unit, isPro: Boolean, proPrice: String?, onUpgrade: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ReferenceHeader(title = "Settings", subtitle = "Make Live Translator feel like yours")
        ProCard(isPro, proPrice, onUpgrade)
        SettingsRow(Icons.Rounded.Translate, "Translation", "Language, offline, cache")
        SettingsRow(Icons.Rounded.ScreenShare, "Overlay", "Floating toggle, position, display")
        SettingsRow(Icons.Rounded.Mic, "Voice", "Input, output, speech, playback")
        SettingsRow(Icons.Rounded.PlayArrow, "Captions", "Position, size, style")
        GlassPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Palette, null, tint = uiState.theme.primary)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                    Text("Themes, toggle size, transparency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ThemeGallery(uiState.theme, onThemeSelected)
            Text("Floating toggle size", style = MaterialTheme.typography.labelMedium)
            Slider(value = uiState.orbSizeDp, onValueChange = onOrbSizeChanged, valueRange = 36f..76f)
            Text("${uiState.orbSizeDp.toInt()} dp  •  The overlay uses this size immediately.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsRow(Icons.Rounded.Security, "Performance & Privacy", "Battery, data, processing")
        SettingsRow(Icons.Rounded.Language, "Language Manager", "Download language packs")
    }
}

@Composable
private fun ReferenceHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun LanguagePairRow(selected: AppLanguage, languages: List<AppLanguage>, onSelected: (AppLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Language, null, tint = MaterialTheme.colorScheme.primary)
            Text("English", modifier = Modifier.padding(start = 10.dp).weight(1f))
            Icon(Icons.Rounded.SwapHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(12.dp)).padding(4.dp)) {
                    Text(selected.displayName)
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(30.dp)) { Icon(Icons.Rounded.ExpandMore, "Choose target") }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    languages.forEach { language -> DropdownMenuItem(text = { Text(language.displayName) }, onClick = { onSelected(language); expanded = false }) }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleCard(icon: ImageVector, title: String, subtitle: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.22f), modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) } }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun FeatureTile(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, checked: Boolean, accent: Color) {
    GlassPanel(modifier) {
        Surface(shape = CircleShape, color = accent.copy(alpha = 0.20f), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) } }
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (checked) Text("Active", style = MaterialTheme.typography.labelSmall, color = accent)
    }
}

@Composable
private fun LanguagePackRow(language: AppLanguage, selected: Boolean, state: LanguagePackState, onSelected: (AppLanguage) -> Unit, onDownload: (AppLanguage) -> Unit) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text(language.displayName.take(2), style = MaterialTheme.typography.labelMedium) } }
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) { Text(language.displayName, fontWeight = FontWeight.SemiBold); Text(if (selected) "Current target" else "Offline model available on demand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (selected) Icon(Icons.Rounded.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
            else OutlinedButton(onClick = { onDownload(language); onSelected(language) }) { Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(17.dp)); Text("Download") }
        }
    }
}

@Composable
private fun SessionStatus(overlayReady: Boolean, captureState: CaptureSessionState, runtimeState: TranslationRuntimeState, onOverlay: () -> Unit, onCapture: () -> Unit) {
    if (overlayReady && captureState is CaptureSessionState.Ready && runtimeState is TranslationRuntimeState.Ready) return
    GlassPanel {
        Text("Setup", fontWeight = FontWeight.SemiBold)
        val message = when (runtimeState) {
            TranslationRuntimeState.Idle -> if (!overlayReady) "Allow overlay access so the floating toggle can stay above other apps." else "Allow screen capture to start translating the visible screen."
            TranslationRuntimeState.Starting -> "Starting the live translator…"
            TranslationRuntimeState.WaitingForText -> "Live and scanning. No foreign text is visible yet."
            is TranslationRuntimeState.Ready -> "Live and translating ${runtimeState.visibleLines} visible line${if (runtimeState.visibleLines == 1) "" else "s"}."
            is TranslationRuntimeState.Error -> runtimeState.message
        }
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!overlayReady) OutlinedButton(onClick = onOverlay, modifier = Modifier.fillMaxWidth()) { Text("Allow overlay") }
        if (captureState is CaptureSessionState.PermissionRequired || captureState is CaptureSessionState.Error) Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) { Text("Allow screen capture") }
    }
}

@Composable
private fun ModeRow(selected: TranslationMode, modes: List<TranslationMode>, onSelect: (TranslationMode) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Mode", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        modes.take(3).forEach { mode -> FilterChip(selected = mode == selected, onClick = { onSelect(mode) }, label = { Text(mode.title) }, modifier = Modifier.padding(start = 5.dp)) }
    }
}

@Composable
private fun ProCard(isPro: Boolean, price: String?, onUpgrade: () -> Unit) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(if (isPro) "Live Translator Pro" else "Unlock Live Translator Pro", fontWeight = FontWeight.SemiBold)
                Text(if (isPro) "All premium features are active." else "Unlimited scanning, voice interpreter, captions, and offline packs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!isPro) {
            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                Text(if (price == null) "See Pro" else "Unlock Pro • $price")
            }
            Text("One-time purchase. No ads and no recurring fee.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Thank you for supporting independent development.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.17f), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) } }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ThemeGallery(selected: AppTheme, onSelected: (AppTheme) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppTheme.entries.forEach { theme ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(82.dp)) {
                Surface(onClick = { onSelected(theme) }, shape = RoundedCornerShape(16.dp), color = theme.glass, border = if (theme == selected) androidx.compose.foundation.BorderStroke(2.dp, theme.primary) else null, modifier = Modifier.size(width = 82.dp, height = 70.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Translate, null, tint = theme.primary, modifier = Modifier.size(26.dp)) }
                }
                Text(theme.title, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 5.dp))
                Text(theme.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BottomBar(tab: AppTab, theme: AppTheme, onTabSelected: (AppTab) -> Unit) {
    Surface(color = theme.backgroundBottom.copy(alpha = 0.92f), tonalElevation = 0.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            BottomItem(Icons.Rounded.Home, "Home", tab == AppTab.HOME, theme) { onTabSelected(AppTab.HOME) }
            BottomItem(Icons.Rounded.Language, "Languages", tab == AppTab.LANGUAGES, theme) { onTabSelected(AppTab.LANGUAGES) }
            BottomItem(Icons.Rounded.Settings, "Settings", tab == AppTab.SETTINGS, theme) { onTabSelected(AppTab.SETTINGS) }
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean, theme: AppTheme, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (selected) theme.primary.copy(alpha = 0.14f) else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp)) {
        IconButton(onClick = onClick) { Icon(icon, label, tint = if (selected) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) theme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)), tonalElevation = 0.dp) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}
