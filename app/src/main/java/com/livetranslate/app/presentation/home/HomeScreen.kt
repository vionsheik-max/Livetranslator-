package com.livetranslate.app.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.livetranslate.app.domain.model.AppLanguage
import com.livetranslate.app.domain.model.TranslationMode
import com.livetranslate.app.presentation.theme.DeepOcean
import com.livetranslate.app.presentation.theme.ElectricBlue
import com.livetranslate.app.presentation.theme.Midnight
import com.livetranslate.app.presentation.theme.OverlayBorder
import com.livetranslate.app.presentation.theme.OverlayGlass
import com.livetranslate.app.presentation.theme.OverlayShadow
import com.livetranslate.app.presentation.theme.SoftMint

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    overlayPermissionGranted: Boolean,
    onTargetLanguageSelected: (AppLanguage) -> Unit,
    onLiveTranslationToggled: (Boolean) -> Unit,
    onModeSelected: (TranslationMode) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Midnight, DeepOcean),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.18f),
                            SoftMint.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HeaderCard(description = uiState.description)
            ImplementationCard(
                overlayPermissionGranted = overlayPermissionGranted,
                onRequestOverlayPermission = onRequestOverlayPermission,
            )
            LanguageCard(
                selectedLanguage = uiState.targetLanguage,
                languages = uiState.availableLanguages,
                onTargetLanguageSelected = onTargetLanguageSelected,
            )
            LiveTranslationCard(
                liveTranslationEnabled = uiState.liveTranslationEnabled,
                overlayPermissionGranted = overlayPermissionGranted,
                onLiveTranslationToggled = onLiveTranslationToggled,
                onRequestOverlayPermission = onRequestOverlayPermission,
            )
            ModeCard(
                selectedMode = uiState.selectedMode,
                modes = uiState.availableModes,
                onModeSelected = onModeSelected,
            )
        }
    }
}

@Composable
private fun ImplementationCard(
    overlayPermissionGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading(
                title = "SESSION STATUS",
                subtitle = "Milestone 2 adds the real floating overlay service and position persistence.",
            )
            Text(
                text = if (overlayPermissionGranted) {
                    "Overlay access is granted. The floating Live Translate control can stay available above other apps."
                } else {
                    "Overlay access is still required before the floating control can appear above other apps."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "MediaProjection capture, OCR, language detection, and in-place translation rendering are not wired yet in this build.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!overlayPermissionGranted) {
                OutlinedButton(onClick = onRequestOverlayPermission) {
                    Text("Grant overlay access")
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    description: String,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "LIVE TRANSLATE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Built for live on-screen OCR, language detection, and in-place translation overlays.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageCard(
    selectedLanguage: AppLanguage,
    languages: List<AppLanguage>,
    onTargetLanguageSelected: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading(
                title = "TARGET LANGUAGE",
                subtitle = "Select the language that translated text should appear in.",
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = selectedLanguage.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Language") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.displayName) },
                            onClick = {
                                onTargetLanguageSelected(language)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveTranslationCard(
    liveTranslationEnabled: Boolean,
    overlayPermissionGranted: Boolean,
    onLiveTranslationToggled: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    GlassCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionHeading(
                        title = "LIVE TRANSLATION",
                        subtitle = "Enable the overlay session now and connect the full capture pipeline next.",
                    )
                }
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Switch(
                    checked = liveTranslationEnabled,
                    onCheckedChange = onLiveTranslationToggled,
                )
            }

            Text(
                text = if (overlayPermissionGranted) {
                    "The floating control can be started and adjusted independently from the main screen."
                } else {
                    "Grant overlay access first so the floating control can appear above other apps."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!overlayPermissionGranted) {
                OutlinedButton(onClick = onRequestOverlayPermission) {
                    Text("Grant overlay access")
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    selectedMode: TranslationMode,
    modes: List<TranslationMode>,
    onModeSelected: (TranslationMode) -> Unit,
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading(
                title = "MODE",
                subtitle = "Tune the live translation pipeline for the current task.",
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                modes.forEach { mode ->
                    val selected = mode == selectedMode
                    FilterChip(
                        selected = selected,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Column {
                                Text(
                                    text = mode.title,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = mode.summary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                        modifier = Modifier.sizeIn(minHeight = 56.dp),
                    )
                }
            }
            OutlinedButton(
                onClick = { onModeSelected(selectedMode) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text("Current mode: ${selectedMode.title}")
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = OverlayGlass,
        tonalElevation = 0.dp,
        shadowElevation = 18.dp,
        border = BorderStroke(1.dp, OverlayBorder),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f),
                            OverlayShadow.copy(alpha = 0.22f),
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            content()
        }
    }
}
