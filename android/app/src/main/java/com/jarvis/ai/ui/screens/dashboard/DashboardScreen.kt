package com.jarvis.ai.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jarvis.ai.navigation.Routes
import com.jarvis.ai.ui.theme.JarvisAccent
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(nav: NavController) {
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(
                title = { Column { Text("JARVIS AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("by Md Foysal • Online", color = JarvisBlue, fontSize = 10.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBackground),
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                    IconButton(onClick = { nav.navigate(Routes.PROVIDER_CONFIG) }) { Icon(Icons.Default.Memory, null, tint = JarvisBlue) }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = JarvisSurface) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = { nav.navigate(Routes.CHAT) }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(selected = false, onClick = { nav.navigate(Routes.VOICE) }, icon = { Icon(Icons.Default.Mic, null) }, label = { Text("Voice") })
                NavigationBarItem(selected = false, onClick = { nav.navigate(Routes.DEVICE) }, icon = { Icon(Icons.Default.PhoneAndroid, null) }, label = { Text("Device") })
            }
        }
    ) { pad ->
        LazyColumn(modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                // Holographic AI Core
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        AICoreAnimation(isListening = isListening, isThinking = isThinking, isSpeaking = isSpeaking)
                        Spacer(Modifier.height(16.dp))
                        StatusChips(isListening, isThinking, isSpeaking)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = { isListening = !isListening; isThinking = false; isSpeaking = false }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (isListening) JarvisBlue else Color.White.copy(0.1f))) {
                                Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (isListening) "Listening..." else "Tap to Speak")
                            }
                            IconButton(onClick = { isSpeaking = !isSpeaking }, modifier = Modifier.background(JarvisSurface, CircleShape)) { Icon(Icons.Default.VolumeUp, null, tint = if (isSpeaking) JarvisBlue else Color.White) }
                            IconButton(onClick = { isThinking = !isThinking }, modifier = Modifier.background(JarvisSurface, CircleShape)) { Icon(Icons.Default.Stop, null, tint = Color.White) }
                        }
                    }
                }
            }
            item {
                Text("Quick Actions", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickCard("New Chat", Icons.Default.AddComment, JarvisBlue) { nav.navigate(Routes.CHAT) }
                    QuickCard("Voice Mode", Icons.Default.RecordVoiceOver, JarvisAccent) { nav.navigate(Routes.VOICE) }
                    QuickCard("Device", Icons.Default.SettingsRemote, Color(0xFFFF6B35)) { nav.navigate(Routes.DEVICE) }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, null, tint = JarvisBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Try saying", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        SuggestionChip("তুমি কে?")
                        SuggestionChip("Open my settings")
                        SuggestionChip("Explain quantum computing")
                        SuggestionChip("Write a Python function")
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Recent Conversations", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.CHAT) }) { Text("View all", color = JarvisBlue, fontSize = 11.sp) }
                }
                // Placeholder recent
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.CHAT) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).background(JarvisBlue.copy(0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ChatBubble, null, tint = JarvisBlue, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text("Welcome to JARVIS", color = Color.White, fontSize = 13.sp); Text("2 min ago • Gemini", color = Color.White.copy(0.5f), fontSize = 10.sp) }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AICoreAnimation(isListening: Boolean, isThinking: Boolean, isSpeaking: Boolean) {
    val infinite = rememberInfiniteTransition(label = "core")
    val rotation by infinite.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "rot")
    val pulse by infinite.animateFloat(initialValue = 0.95f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "pulse")
    val color = when { isListening -> Color(0xFF00FF88); isThinking -> Color(0xFFFFD600); isSpeaking -> JarvisBlue; else -> JarvisBlue }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        // Outer glow
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.copy(0.25f), Color.Transparent)), CircleShape).blur(12.dp))
        // Rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(color = color.copy(0.15f), radius = r, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = color.copy(0.08f), radius = r * 0.75f, style = Stroke(width = 1.dp.toPx()))
            // rotating dots
            for (i in 0 until 8) {
                val angle = Math.toRadians((rotation + i * 45).toDouble())
                val x = center.x + kotlin.math.cos(angle).toFloat() * r * 0.88f
                val y = center.y + kotlin.math.sin(angle).toFloat() * r * 0.88f
                drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(x, y))
            }
        }
        // Inner core
        Box(
            modifier = Modifier.size((90 * pulse).dp).background(Brush.radialGradient(listOf(color, color.copy(0.6f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Voice bars when speaking/listening
            if (isSpeaking || isListening) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { idx ->
                        val h by infinite.animateFloat(initialValue = 6f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(300 + idx * 80, easing = EaseInOut), RepeatMode.Reverse), label = "bar$idx")
                        Box(modifier = Modifier.width(3.dp).height(h.dp).background(Color.White, RoundedCornerShape(2.dp)))
                    }
                }
            } else {
                Text("◉", color = Color.White, fontSize = 28.sp)
            }
        }
    }
}

@Composable
fun StatusChips(listening: Boolean, thinking: Boolean, speaking: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip("Listening", listening, Color(0xFF00FF88))
        StatusChip("Thinking", thinking, Color(0xFFFFD600))
        StatusChip("Speaking", speaking, JarvisBlue)
    }
}

@Composable
fun StatusChip(label: String, active: Boolean, color: Color) {
    Box(modifier = Modifier.background(if (active) color.copy(0.15f) else Color.White.copy(0.06f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(if (active) color else Color.White.copy(0.3f), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (active) color else Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RowScope.QuickCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(44.dp).background(color.copy(0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) }
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SuggestionChip(text: String) {
    Box(modifier = Modifier.padding(vertical = 3.dp).background(Color.White.copy(0.06f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(text, color = Color.White.copy(0.8f), fontSize = 12.sp)
    }
}
