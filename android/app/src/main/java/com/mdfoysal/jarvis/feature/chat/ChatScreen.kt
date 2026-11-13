package com.mdfoysal.jarvis.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mdfoysal.jarvis.core.persona.DeviceAction
import com.mdfoysal.jarvis.data.local.entity.MessageEntity
import com.mdfoysal.jarvis.ui.components.CoreState
import com.mdfoysal.jarvis.ui.components.GlassCard
import com.mdfoysal.jarvis.ui.components.JarvisCore
import com.mdfoysal.jarvis.ui.components.PulsingDot
import com.mdfoysal.jarvis.ui.components.StatusPill
import com.mdfoysal.jarvis.ui.theme.JarvisBackgroundBrush
import com.mdfoysal.jarvis.ui.theme.JarvisDanger
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisTextSecondary
import com.mdfoysal.jarvis.ui.theme.JarvisTeal
import com.mdfoysal.jarvis.ui.theme.JarvisViolet

/** The main JARVIS dashboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onConfirmCommand: () -> Unit,
    onDismissCommand: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearConversation: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("JARVIS", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = state.activeProvider?.nickname ?: "No provider",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisTextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenProviders) {
                        Icon(Icons.Default.Tune, contentDescription = "Providers", tint = JarvisHoloBlue)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = JarvisHoloBlue)
                    }
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
        ) {
            // ---- holographic core -------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    JarvisCore(
                        state = when (state.assistantState) {
                            AssistantState.LISTENING -> CoreState.LISTENING
                            AssistantState.THINKING -> CoreState.THINKING
                            AssistantState.SPEAKING -> CoreState.SPEAKING
                            AssistantState.IDLE -> CoreState.IDLE
                        },
                        size = 172.dp,
                        amplitude = state.amplitude
                    )
                    Spacer(Modifier.height(10.dp))
                    StatusPill(
                        text = when (state.assistantState) {
                            AssistantState.LISTENING -> "Listening"
                            AssistantState.THINKING -> "Thinking"
                            AssistantState.SPEAKING -> "Speaking"
                            AssistantState.IDLE -> "Ready"
                        },
                        color = when (state.assistantState) {
                            AssistantState.LISTENING -> JarvisTeal
                            AssistantState.THINKING -> JarvisViolet
                            AssistantState.SPEAKING -> JarvisHoloBlue
                            AssistantState.IDLE -> JarvisHoloBlue
                        }
                    )
                    state.partialSpeech.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ---- conversation -----------------------------------------------
            if (state.messages.isEmpty()) {
                EmptyConversation()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }

            state.error?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatusPill(text = error, color = JarvisDanger, showDot = false)
                }
            }

            // ---- input ------------------------------------------------------
            InputBar(
                value = state.input,
                assistantState = state.assistantState,
                onValueChange = onInputChange,
                onSend = onSend,
                onMicClick = {
                    if (state.assistantState == AssistantState.LISTENING) onStopListening() else onStartListening()
                },
                onStopSpeaking = onStopSpeaking,
                onClear = onClearConversation
            )
        }
    }

    // ---- confirmation dialog for phone actions ----------------------------
    state.pendingCommand?.let { command ->
        AlertDialog(
            onDismissRequest = onDismissCommand,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Confirm this action") },
            text = {
                Column {
                    Text(describeCommand(command))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "JARVIS will only perform this after you confirm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmCommand) {
                    Text("Allow", color = JarvisHoloBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCommand) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            }
        )
    }
}

private fun describeCommand(command: com.mdfoysal.jarvis.core.persona.DeviceCommand): String =
    when (command.action) {
        DeviceAction.OPEN_APP -> "Open the app “${command.target}”?"
        DeviceAction.SET_ALARM -> "Set an alarm for ${command.time}?"
        DeviceAction.CREATE_REMINDER -> "Create a reminder: “${command.target}”?"
        DeviceAction.VOLUME_UP -> "Raise the media volume?"
        DeviceAction.VOLUME_DOWN -> "Lower the media volume?"
        DeviceAction.VOLUME_MUTE -> "Mute the media volume?"
        DeviceAction.FLASHLIGHT_ON -> "Turn the flashlight on?"
        DeviceAction.FLASHLIGHT_OFF -> "Turn the flashlight off?"
        DeviceAction.BATTERY_INFO -> "Check the battery status?"
        DeviceAction.DEVICE_INFO -> "Show the device information?"
        DeviceAction.OPEN_SETTINGS -> "Open the system settings?"
        DeviceAction.READ_NOTIFICATIONS -> "Read the current notifications?"
        DeviceAction.START_CALL -> "Open the dialler for ${command.target}?"
        DeviceAction.SEND_SMS -> "Open a message to ${command.target}?"
        DeviceAction.OPEN_WEB -> "Open ${command.target}?"
        DeviceAction.UNKNOWN -> "Unknown action"
    }

@Composable
private fun EmptyConversation() {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            PulsingDot(color = JarvisHoloBlue)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ask me anything — in English or বাংলা",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "I can chat, help with code and maths, translate, summarise, and control parts of this phone with your confirmation.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) JarvisHoloBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    1.dp,
                    if (isUser) JarvisHoloBlue.copy(alpha = 0.35f) else JarvisHoloBlue.copy(alpha = 0.12f),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.errorMessage != null) JarvisDanger else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    assistantState: AssistantState,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClear: () -> Unit
) {
    GlassCard(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask JARVIS…", color = JarvisTextSecondary) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisHoloBlue,
                    unfocusedBorderColor = JarvisHoloBlue.copy(alpha = 0.25f),
                    cursorColor = JarvisHoloBlue
                )
            )
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (assistantState == AssistantState.LISTENING) JarvisTeal.copy(alpha = 0.25f)
                        else JarvisHoloBlue.copy(alpha = 0.12f)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = if (assistantState == AssistantState.LISTENING) JarvisTeal else JarvisHoloBlue
                )
            }
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(JarvisHoloBlue.copy(alpha = 0.16f))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = JarvisHoloBlue)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (assistantState == AssistantState.SPEAKING) {
                TextButton(onClick = onStopSpeaking) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop speaking", color = JarvisDanger)
                }
            }
            TextButton(onClick = onClear) {
                Text("Clear conversation", color = JarvisTextSecondary)
            }
        }
    }
}
