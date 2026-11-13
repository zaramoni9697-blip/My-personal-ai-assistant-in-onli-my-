package com.mdfoysal.jarvis.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mdfoysal.jarvis.data.local.entity.MemoryEntity
import com.mdfoysal.jarvis.ui.components.GlassCard
import com.mdfoysal.jarvis.ui.components.SectionHeader
import com.mdfoysal.jarvis.ui.components.StatusPill
import com.mdfoysal.jarvis.ui.theme.JarvisBackgroundBrush
import com.mdfoysal.jarvis.ui.theme.JarvisDanger
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisTextSecondary
import com.mdfoysal.jarvis.ui.theme.JarvisTeal

/** The settings screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLanguageChange: (String) -> Unit,
    onTtsEnabledChange: (Boolean) -> Unit,
    onTtsRateChange: (Float) -> Unit,
    onVoiceModeChange: (Boolean) -> Unit,
    onWakeWordEnabledChange: (Boolean) -> Unit,
    onWakeWordPhraseChange: (String) -> Unit,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onAddMemory: (String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onOpenProviders: () -> Unit,
    onOpenPhoneControl: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back", color = JarvisHoloBlue) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(JarvisBackgroundBrush)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ---- profile ----------------------------------------------------
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Profile", subtitle = state.email)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.displayName.ifBlank { "JARVIS user" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- voice ------------------------------------------------------
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Voice & language")
                    Spacer(Modifier.height(12.dp))
                    Text("Reply language", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("auto" to "Automatic", "en" to "English", "bn" to "বাংলা").forEach { (value, label) ->
                            ChoiceChip(
                                label = label,
                                selected = state.settings.language == value,
                                onClick = { onLanguageChange(value) }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SwitchRow(
                        title = "Speak replies aloud",
                        subtitle = "Text-to-speech for every answer",
                        checked = state.settings.ttsEnabled,
                        onCheckedChange = onTtsEnabledChange
                    )
                    if (state.settings.ttsEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text("Speech speed", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = state.settings.ttsRate,
                            onValueChange = onTtsRateChange,
                            valueRange = 0.5f..2f,
                            colors = SliderDefaults.colors(
                                thumbColor = JarvisHoloBlue,
                                activeTrackColor = JarvisHoloBlue
                            )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Continuous voice mode and wake-word listening are not available yet. Use the microphone button for individual requests.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- providers --------------------------------------------------
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "AI providers")
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenProviders,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisHoloBlue)
                    ) { Text("Manage providers") }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenPhoneControl,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisHoloBlue)
                    ) { Text("Phone control & permissions") }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- memory -----------------------------------------------------
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Memory",
                        subtitle = "Everything JARVIS remembers is stored on this device and you can delete it."
                    )
                    Spacer(Modifier.height(10.dp))
                    SwitchRow(
                        title = "Use memory in conversations",
                        subtitle = "Send the notes below as context",
                        checked = state.settings.memoryEnabled,
                        onCheckedChange = onMemoryEnabledChange
                    )
                    Spacer(Modifier.height(10.dp))
                    var memoryInput by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = memoryInput,
                            onValueChange = { memoryInput = it },
                            label = { Text("Remember that…") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisHoloBlue,
                                unfocusedBorderColor = JarvisHoloBlue.copy(alpha = 0.35f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                onAddMemory(memoryInput)
                                memoryInput = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add memory", tint = JarvisHoloBlue)
                        }
                    }
                    state.memories.forEach { memory ->
                        MemoryRow(memory = memory, onDelete = { onDeleteMemory(memory.id) })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- account ----------------------------------------------------
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Account")
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisHoloBlue)
                    ) { Text("Log out") }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisDanger)
                    ) { Text("Delete account and all local data") }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ---- about ------------------------------------------------------
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "About")
                    Spacer(Modifier.height(8.dp))
                    Text("JARVIS AI", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Personal AI Assistant · Created by Md Foysal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextSecondary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = JarvisTextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisHoloBlue,
                checkedTrackColor = JarvisHoloBlue.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) JarvisHoloBlue else JarvisTextSecondary,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) JarvisHoloBlue.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun MemoryRow(memory: MemoryEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = memory.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = JarvisDanger)
        }
    }
}
