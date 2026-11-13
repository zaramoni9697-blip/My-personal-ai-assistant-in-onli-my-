package com.mdfoysal.jarvis.feature.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mdfoysal.jarvis.core.providers.AuthType
import com.mdfoysal.jarvis.core.providers.ProviderConfig
import com.mdfoysal.jarvis.core.providers.ProviderSpec
import com.mdfoysal.jarvis.core.providers.RequestStyle
import com.mdfoysal.jarvis.ui.components.GlassCard
import com.mdfoysal.jarvis.ui.components.SectionHeader
import com.mdfoysal.jarvis.ui.components.StatusPill
import com.mdfoysal.jarvis.ui.theme.JarvisBackgroundBrush
import com.mdfoysal.jarvis.ui.theme.JarvisDanger
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisTextSecondary
import com.mdfoysal.jarvis.ui.theme.JarvisTeal

/** Provider list + add button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    state: ProvidersUiState,
    onAddProvider: (String) -> Unit,
    onEditProvider: (String, String) -> Unit,
    onSetActive: (ProviderConfig) -> Unit,
    onDeleteProvider: (ProviderConfig) -> Unit,
    onContinue: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("AI Provider Setup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(JarvisBackgroundBrush)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SectionHeader(
                title = "Choose who JARVIS talks to",
                subtitle = "Your API keys are encrypted with the Android Keystore and never leave this device."
            )
            Spacer(Modifier.height(18.dp))

            if (state.configs.isEmpty()) {
                GlassCard {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "No provider configured yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Add Gemini, OpenAI, Claude, DeepSeek, Groq, OpenRouter, Mistral or your own custom API to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSecondary
                        )
                    }
                }
            } else {
                state.configs.forEach { config ->
                    ProviderConfigCard(
                        config = config,
                        isActive = config.id == state.activeConfigId,
                        onEdit = { onEditProvider(config.providerId, config.id) },
                        onSetActive = { onSetActive(config) },
                        onDelete = { onDeleteProvider(config) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(6.dp))
            SectionHeader(title = "Add a provider")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.specs.forEach { spec ->
                    ProviderChip(spec = spec, onClick = { onAddProvider(spec.id) })
                }
            }

            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisHoloBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open JARVIS dashboard", color = MaterialTheme.colorScheme.background)
            }
        }
    }
}

@Composable
private fun ProviderChip(spec: ProviderSpec, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(JarvisHoloBlue.copy(alpha = 0.10f))
            .border(1.dp, JarvisHoloBlue.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = JarvisHoloBlue, modifier = Modifier.size(16.dp))
        Text(spec.label, style = MaterialTheme.typography.labelLarge, color = JarvisHoloBlue)
    }
}

@Composable
private fun ProviderConfigCard(
    config: ProviderConfig,
    isActive: Boolean,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(accent = if (isActive) JarvisTeal else JarvisHoloBlue) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${config.providerId} · ${config.model}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextSecondary
                    )
                }
                if (isActive) {
                    StatusPill(text = "Active", color = JarvisTeal, showDot = false)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = config.baseUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisHoloBlue)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                if (!isActive) {
                    OutlinedButton(
                        onClick = onSetActive,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisTeal)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set active")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisDanger)
                }
            }
        }
    }
}

/** Add / edit a provider configuration. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    specs: List<ProviderSpec>,
    form: ProviderFormState,
    onProviderSelected: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onChatPathChange: (String) -> Unit,
    onTestPathChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onAuthTypeChange: (AuthType) -> Unit,
    onRequestStyleChange: (RequestStyle) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onResponseTextPathChange: (String) -> Unit,
    onToggleStreaming: () -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onNavigateBack: () -> Unit
) {
    val spec = specs.firstOrNull { it.id == form.providerId }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (form.configId == null) "Add provider" else "Edit provider") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back", color = JarvisHoloBlue) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
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
            SectionHeader(
                title = spec?.label ?: form.providerId,
                subtitle = spec?.description.orEmpty()
            )
            Spacer(Modifier.height(16.dp))

            if (form.configId == null) {
                SectionHeader(title = "Provider")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specs.forEach { item ->
                        val selected = item.id == form.providerId
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) JarvisHoloBlue else JarvisTextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) JarvisHoloBlue.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) JarvisHoloBlue.copy(alpha = 0.5f) else JarvisTextSecondary.copy(alpha = 0.2f),
                                    RoundedCornerShape(50)
                                )
                                .clickable { onProviderSelected(item.id) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Credentials")
                    Spacer(Modifier.height(12.dp))
                    JarField(
                        value = form.apiKey,
                        onValueChange = onApiKeyChange,
                        label = "API key",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = form.apiKeyVisible,
                        onToggleVisibility = onToggleApiKeyVisibility
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuthType.entries.forEach { type ->
                            val selected = type == form.authType
                            Text(
                                text = type.name.lowercase().replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) JarvisHoloBlue else JarvisTextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) JarvisHoloBlue.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) JarvisHoloBlue.copy(alpha = 0.5f) else JarvisTextSecondary.copy(alpha = 0.2f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { onAuthTypeChange(type) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Model & endpoint")
                    Spacer(Modifier.height(12.dp))
                    JarField(form.nickname, onNicknameChange, "Configuration name")
                    Spacer(Modifier.height(12.dp))
                    JarField(form.model, onModelChange, "Model ID")
                    Spacer(Modifier.height(12.dp))
                    JarField(form.baseUrl, onBaseUrlChange, "API base URL")
                    Spacer(Modifier.height(12.dp))
                    JarField(form.chatPath, onChatPathChange, "Endpoint path")
                    Spacer(Modifier.height(12.dp))
                    JarField(form.testPath, onTestPathChange, "Test endpoint path")
                    if (form.providerId == "custom") {
                        Spacer(Modifier.height(12.dp))
                        JarField(
                            form.responseTextPath,
                            onResponseTextPathChange,
                            "Reply text path (optional, e.g. choices.0.message.content)"
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Wire format",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisTextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RequestStyle.entries.forEach { style ->
                                val selected = style == form.requestStyle
                                Text(
                                    text = style.name.lowercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) JarvisHoloBlue else JarvisTextSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) JarvisHoloBlue.copy(alpha = 0.16f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                        .clickable { onRequestStyleChange(style) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(title = "Generation")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Streaming replies", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = form.streaming,
                            onCheckedChange = { onToggleStreaming() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisHoloBlue,
                                checkedTrackColor = JarvisHoloBlue.copy(alpha = 0.35f)
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    JarField(form.temperature, onTemperatureChange, "Temperature (optional)", KeyboardType.Decimal)
                    Spacer(Modifier.height(12.dp))
                    JarField(form.maxTokens, onMaxTokensChange, "Max tokens (optional)", KeyboardType.Number)
                }
            }

            Spacer(Modifier.height(16.dp))
            form.error?.let {
                StatusPill(text = it, color = JarvisDanger, showDot = false)
                Spacer(Modifier.height(10.dp))
            }
            form.message?.let {
                StatusPill(text = it, color = JarvisTeal, showDot = false)
                Spacer(Modifier.height(10.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !form.isTesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisHoloBlue)
                ) {
                    if (form.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test connection")
                    }
                }
                Button(
                    onClick = onSave,
                    enabled = !form.isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisHoloBlue)
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.background)
                }
            }

            onDelete?.let {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisDanger)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete configuration")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun JarField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = when {
            !isPassword -> VisualTransformation.None
            passwordVisible -> VisualTransformation.None
            else -> PasswordVisualTransformation()
        },
        trailingIcon = if (onToggleVisibility != null) {
            {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide API key" else "Show API key"
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisHoloBlue,
            unfocusedBorderColor = JarvisHoloBlue.copy(alpha = 0.35f),
            focusedLabelColor = JarvisHoloBlue,
            cursorColor = JarvisHoloBlue
        )
    )
}
