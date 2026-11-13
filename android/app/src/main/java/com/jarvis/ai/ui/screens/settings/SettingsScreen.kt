package com.jarvis.ai.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jarvis.ai.navigation.Routes
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    var voiceEnabled by remember { mutableStateOf(true) }
    var wakeEnabled by remember { mutableStateOf(false) }
    var ttsRate by remember { mutableStateOf(1f) }
    var language by remember { mutableStateOf("Auto") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(title = { Text("Settings", color = Color.White) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBackground))
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).background(JarvisBlue, CircleShape), contentAlignment = Alignment.Center) { Text("MF", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Md Foysal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("md.foysal@example.com", color = Color.White.copy(0.5f), fontSize = 11.sp) }
                    Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(18.dp))
                }
            }

            Text("Voice & Speech", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSwitch("Voice Responses", "Speak answers aloud", voiceEnabled) { voiceEnabled = it }
                    Divider(color = Color.White.copy(0.06f))
                    SettingSwitch("Wake Word", "সবসময় প্রস্তুত (foreground service)", wakeEnabled) { wakeEnabled = it }
                    Divider(color = Color.White.copy(0.06f))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Speech Rate", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Slider(value = ttsRate, onValueChange = { ttsRate = it }, valueRange = 0.5f..2f, modifier = Modifier.weight(1f))
                        Text(String.format("%.1fx", ttsRate), color = JarvisBlue, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Text("General", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingItem(Icons.Default.Language, "Language", language) { language = if (language == "Auto") "English" else if (language == "English") "বাংলা" else "Auto" }
                    Divider(color = Color.White.copy(0.06f))
                    SettingItem(Icons.Default.Memory, "AI Providers", "Manage API keys") { nav.navigate(Routes.PROVIDER_CONFIG) }
                    Divider(color = Color.White.copy(0.06f))
                    SettingItem(Icons.Default.DeleteSweep, "Clear Conversations", "Delete all history") { /* call repo */ }
                }
            }

            Text("Support", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingItem(Icons.Default.Info, "About JARVIS AI", "v1.0.0 by Md Foysal") {}
                    SettingItem(Icons.Default.PrivacyTip, "Privacy Policy", "Encrypted local storage") {}
                    SettingItem(Icons.Default.Help, "Help & Feedback", "Contact support") {}
                }
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { nav.navigate(Routes.LOGIN) { popUpTo(0) } }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Logout")
            }
            Button(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1A1A)), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Delete Account", color = Color(0xFFFF6B6B))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently delete your data, conversations and API configs. This cannot be undone.") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; nav.navigate(Routes.LOGIN) { popUpTo(0) } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean)->Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp); Text(subtitle, color = Color.White.copy(0.5f), fontSize = 10.sp) }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: ()->Unit) {
    ListItem(
        headlineContent = { Text(title, color = Color.White, fontSize = 13.sp) },
        supportingContent = { Text(subtitle, color = Color.White.copy(0.5f), fontSize = 11.sp) },
        leadingContent = { Icon(icon, null, tint = JarvisBlue, modifier = Modifier.size(20.dp)) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
