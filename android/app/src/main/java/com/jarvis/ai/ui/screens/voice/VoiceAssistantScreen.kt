package com.jarvis.ai.ui.screens.voice

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jarvis.ai.ui.screens.dashboard.AICoreAnimation
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.viewmodel.VoiceViewModel
import com.jarvis.ai.viewmodel.ChatViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(nav: NavController, voiceVm: VoiceViewModel = hiltViewModel(), chatVm: ChatViewModel = hiltViewModel()) {
    val permission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val isListening by voiceVm.isListening.collectAsState()
    val isSpeaking by voiceVm.isSpeaking.collectAsState()
    val transcript by voiceVm.lastTranscript.collectAsState()
    var conversation by remember { mutableStateOf(listOf<Pair<String,String>>()) } // role to text
    var currentLang by remember { mutableStateOf("auto") }
    var wakeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(title = { Text("Voice Assistant", color = Color.White, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBackground))
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Language", color = Color.White.copy(0.7f), fontSize = 12.sp)
                        SegmentedLanguage(currentLang) { currentLang = it }
                    }
                    Spacer(Modifier.height(16.dp))
                    AICoreAnimation(isListening = isListening, isThinking = false, isSpeaking = isSpeaking)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when { isListening -> "Listening... speak now"; isSpeaking -> "Speaking..."; else -> "Tap mic to speak" },
                        color = if (isListening) Color(0xFF00FF88) else Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                    if (transcript.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.08f)), modifier = Modifier.fillMaxWidth()) {
                            Text(transcript, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (!permission.status.isGranted) {
                        Button(onClick = { permission.launchPermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.MicOff, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Grant Microphone Permission")
                        }
                        Text("Microphone required for voice assistant", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            FloatingActionButton(
                                onClick = {
                                    if (isListening) voiceVm.stopListening()
                                    else voiceVm.startListening(currentLang) { text ->
                                        conversation = conversation + ("user" to text)
                                        // Send to chat engine (mock response if no provider)
                                        val reply = generateLocalReply(text)
                                        conversation = conversation + ("assistant" to reply)
                                        voiceVm.speak(reply)
                                    }
                                },
                                containerColor = if (isListening) Color(0xFFFF3B30) else JarvisBlue,
                                modifier = Modifier.size(72.dp), shape = CircleShape
                            ) {
                                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            IconButton(onClick = { voiceVm.stopSpeaking() }, modifier = Modifier.size(48.dp).background(JarvisSurface, CircleShape)) {
                                Icon(Icons.Default.VolumeOff, null, tint = Color.White)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                            Switch(checked = wakeEnabled, onCheckedChange = { wakeEnabled = it })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Wake word: সবসময় প্রস্তুত", color = Color.White, fontSize = 12.sp)
                                Text("Foreground service with notification", color = Color.White.copy(0.4f), fontSize = 10.sp)
                            }
                        }
                        if (wakeEnabled) {
                            Text("Background listening uses foreground service (Android compliant). Requires notification permission.", color = Color.White.copy(0.4f), fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (conversation.isNotEmpty()) {
                Text("Conversation", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(conversation) { (role, text) ->
                        val isUser = role == "user"
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isUser) JarvisBlue else JarvisSurface), modifier = Modifier.widthIn(max = 280.dp)) {
                                Text(text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
                TextButton(onClick = { conversation = emptyList() }) { Text("Clear", color = JarvisBlue, fontSize = 12.sp) }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Voice history will appear here", color = Color.White.copy(0.3f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SegmentedLanguage(selected: String, onSelect: (String)->Unit) {
    Row(modifier = Modifier.background(Color.White.copy(0.08f), RoundedCornerShape(20.dp)).padding(4.dp)) {
        listOf("auto" to "Auto", "en" to "EN", "bn" to "BN").forEach { (id, label) ->
            Box(modifier = Modifier.background(if (selected == id) JarvisBlue else Color.Transparent, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp).let { m -> m }) {
                Text(label, color = if (selected == id) Color.White else Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.let { m -> m })
            }
        }
    }
}

fun generateLocalReply(input: String): String {
    val lower = input.lowercase()
    return when {
        lower.contains("who created you") || lower.contains("who made you") -> "I was created by Md Foysal."
        lower.contains("তোমাকে কে তৈরি করেছে") || lower.contains("তুমি কে") -> "আমি JARVIS AI, আমাকে তৈরি করেছেন Md Foysal।"
        lower.contains("hello") || lower.contains("hi") -> "Hello! I'm JARVIS, ready to help. What can I do for you?"
        lower.contains("open") && lower.contains("setting") -> "I'll help you open the supported settings page."
        else -> "Got it: \"$input\". I'm JARVIS AI by Md Foysal — I can chat, help with code, translate, and control your device with permission."
    }
}
