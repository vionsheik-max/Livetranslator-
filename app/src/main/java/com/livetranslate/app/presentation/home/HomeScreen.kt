package com.livetranslate.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livetranslate.app.capture.CaptureSessionState
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import com.livetranslate.app.presentation.theme.DeepOcean
import com.livetranslate.app.presentation.theme.ElectricBlue
import com.livetranslate.app.presentation.theme.Midnight
import com.livetranslate.app.presentation.theme.OverlayBorder
import com.livetranslate.app.presentation.theme.OverlayGlass
import com.livetranslate.app.presentation.theme.SoftMint

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    overlayPermissionGranted: Boolean,
    captureSessionState: CaptureSessionState,
    onTargetLanguageSelected: (AppLanguage) -> Unit,
    onLiveTranslationToggled: (Boolean) -> Unit,
    onVoiceInterpreterToggled: (Boolean) -> Unit,
    onModeSelected: (TranslationMode) -> Unit,
    onDownloadLanguagePack: (AppLanguage) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Midnight, DeepOcean)))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppHeader()
            StatusCard(
                overlayReady = overlayPermissionGranted,
                captureState = captureSessionState,
                onOverlayPermission = onRequestOverlayPermission,
                onCapturePermission = onRequestScreenCapturePermission,
            )
            FeatureRow(
                icon = Icons.Rounded.Translate,
                title = "Screen translation",
                subtitle = "Translate visible text in place",
                checked = uiState.liveTranslationEnabled,
                accent = SoftMint,
                onCheckedChange = onLiveTranslationToggled,
            )
            FeatureRow(
                icon = Icons.Rounded.Mic,
                title = "Voice captions",
                subtitle = "Translate spoken dialogue into captions",
                checked = uiState.voiceInterpreterEnabled,
                accent = ElectricBlue,
                onCheckedChange = onVoiceInterpreterToggled,
            )
            LanguageCard(
                selected = uiState.targetLanguage,
                languages = uiState.availableLanguages,
                packState = uiState.languagePackState,
                onSelected = onTargetLanguageSelected,
                onDownload = onDownloadLanguagePack,
            )
            ModeRow(
                selectedMode = uiState.selectedMode,
                modes = uiState.availableModes,
                onModeSelected = onModeSelected,
            )
            PrivacyNote()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = CircleShape, color = SoftMint.copy(alpha = 0.16f), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Translate, null, tint = SoftMint, modifier = Modifier.size(25.dp))
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text("Live Translator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Your screen, understood.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusCard(
    overlayReady: Boolean,
    captureState: CaptureSessionState,
    onOverlayPermission: () -> Unit,
    onCapturePermission: () -> Unit,
) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.GraphicEq, null, tint = if (overlayReady) SoftMint else ElectricBlue, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(if (overlayReady) "Ready when you are" else "One quick setup", fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (captureState) {
                        CaptureSessionState.PermissionRequired -> "Allow screen capture to translate another app."
                        CaptureSessionState.Starting -> "Starting screen capture…"
                        is CaptureSessionState.Ready -> "Live capture is ready."
                        is CaptureSessionState.Error -> "Capture needs attention."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!overlayReady) {
            OutlinedButton(onClick = onOverlayPermission, modifier = Modifier.fillMaxWidth()) { Text("Allow overlay") }
        }
        if (captureState is CaptureSessionState.PermissionRequired || captureState is CaptureSessionState.Error) {
            Button(onClick = onCapturePermission, modifier = Modifier.fillMaxWidth()) { Text("Allow screen capture") }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp)) }
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun LanguageCard(
    selected: AppLanguage,
    languages: List<AppLanguage>,
    packState: LanguagePackState,
    onSelected: (AppLanguage) -> Unit,
    onDownload: (AppLanguage) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Language, null, tint = SoftMint, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Translate into", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected.displayName, fontWeight = FontWeight.SemiBold)
            }
            Box {
                androidx.compose.material3.IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.ExpandMore, "Choose language")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.displayName) },
                            onClick = { onSelected(language); menuOpen = false },
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudDownload, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text("Optional offline pack", modifier = Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.bodySmall)
            when (packState) {
                LanguagePackState.Downloading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                is LanguagePackState.Ready -> Icon(Icons.Rounded.Check, "Downloaded", tint = SoftMint, modifier = Modifier.size(20.dp))
                else -> OutlinedButton(onClick = { onDownload(selected) }) { Text("Download") }
            }
        }
        if (packState is LanguagePackState.Failed) {
            Text(packState.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ModeRow(
    selectedMode: TranslationMode,
    modes: List<TranslationMode>,
    onModeSelected: (TranslationMode) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        modes.take(3).forEach { mode ->
            FilterChip(selected = mode == selectedMode, onClick = { onModeSelected(mode) }, label = { Text(mode.title) }, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
        Text("Capture and microphone stay off until you turn them on.", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GlassPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
        color = OverlayGlass.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OverlayBorder.copy(alpha = 0.46f)),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
