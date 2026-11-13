package com.jarvis.ai.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jarvis.ai.device.PermissionManager
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.viewmodel.DeviceViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlScreen(nav: NavController, vm: DeviceViewModel = hiltViewModel()) {
    val status by vm.status.collectAsState()
    var showApps by remember { mutableStateOf(false) }
    var alarmHour by remember { mutableStateOf("7") }
    var alarmMin by remember { mutableStateOf("00") }
    var flashOn by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(title = { Text("Device Control", color = Color.White) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBackground))
        }
    ) { pad ->
        LazyColumn(modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                if (status != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = JarvisBlue.copy(0.15f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = JarvisBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(status!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.clear() }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
            }
            item {
                Text("Permissions", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PermissionRow(Icons.Default.Mic, "Microphone", "Voice commands", true)
                        PermissionRow(Icons.Default.Phone, "Phone", "Make calls via dialer", false)
                        PermissionRow(Icons.Default.Notifications, "Notifications", "Read notifications (if granted)", false)
                        Text("All actions request confirmation and use Android intents. No hidden permissions.", color = Color.White.copy(0.4f), fontSize = 10.sp)
                    }
                }
            }
            item {
                Text("Quick Controls", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ControlTile(Icons.Default.Lightbulb, "Flashlight", if (flashOn) "ON" else "OFF") { flashOn = !flashOn; vm.toggleFlash(flashOn) }
                    ControlTile(Icons.Default.VolumeUp, "Volume", "Adjust") { vm.openSettings("sound") }
                    ControlTile(Icons.Default.BatteryFull, "Battery", vm.batteryInfo().take(12)) { }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, null, tint = JarvisBlue)
                            Spacer(Modifier.width(8.dp))
                            Text("Set Alarm", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = alarmHour, onValueChange = { alarmHour = it }, label = { Text("Hour") }, modifier = Modifier.weight(1f), singleLine = true)
                            Text(":", color = Color.White, fontSize = 18.sp)
                            OutlinedTextField(value = alarmMin, onValueChange = { alarmMin = it }, label = { Text("Min") }, modifier = Modifier.weight(1f), singleLine = true)
                            Button(onClick = { val h = alarmHour.toIntOrNull() ?: 7; val m = alarmMin.toIntOrNull() ?: 0; vm.setAlarm(h,m) }, colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue)) { Text("Set") }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Device Info", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(vm.deviceInfo(), color = Color.White.copy(0.7f), fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(vm.batteryInfo(), color = JarvisBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth().clickable { showApps = !showApps }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, null, tint = JarvisBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Installed Apps", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Icon(if (showApps) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.White.copy(0.5f))
                    }
                }
                if (showApps) {
                    val apps = vm.installedApps().take(20)
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface.copy(0.6f)), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            apps.forEach { (pkg, label) ->
                                ListItem(
                                    headlineContent = { Text(label, fontSize = 12.sp, color = Color.White) },
                                    supportingContent = { Text(pkg, fontSize = 10.sp, color = Color.White.copy(0.4f)) },
                                    trailingContent = { TextButton(onClick = { vm.openApp(pkg) }) { Text("Open", color = JarvisBlue, fontSize = 11.sp) } }
                                )
                                Divider(color = Color.White.copy(0.06f))
                            }
                            if (apps.isEmpty()) Text("No launchable apps found", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { vm.openSettings("general") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Settings") }
                    OutlinedButton(onClick = { vm.openSettings("wifi") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Wifi, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Wi-Fi") }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(icon: ImageVector, title: String, desc: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(32.dp).background(if (granted) JarvisBlue.copy(0.15f) else Color.White.copy(0.08f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (granted) JarvisBlue else Color.White.copy(0.4f), modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text(desc, color = Color.White.copy(0.5f), fontSize = 10.sp) }
        Icon(if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (granted) JarvisBlue else Color.White.copy(0.2f), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun RowScope.ControlTile(icon: ImageVector, title: String, subtitle: String, onClick: ()->Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = JarvisBlue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(0.5f), fontSize = 10.sp)
        }
    }
}
