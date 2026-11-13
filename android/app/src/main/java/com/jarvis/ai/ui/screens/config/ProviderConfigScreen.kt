package com.jarvis.ai.ui.screens.config

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jarvis.ai.data.local.datastore.PreferencesManager
import com.jarvis.ai.navigation.Routes
import com.jarvis.ai.network.provider.ProviderType
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.viewmodel.ProviderViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigScreen(nav: NavController, vm: ProviderViewModel = hiltViewModel()) {
    // In real app, get userId from PreferencesManager
    var showAddDialog by remember { mutableStateOf(false) }
    val providers by vm.providers.collectAsState()
    val msg by vm.uiState.collectAsState()
    val loading by vm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    // Mock userId for demo; real flow injects from auth
    LaunchedEffect(Unit) { vm.setUserId("demo-user-id") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AI Provider Setup", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBackground),
                actions = {
                    TextButton(onClick = { nav.navigate(Routes.DASHBOARD) }) { Text("Skip →", color = JarvisBlue) }
                })
        },
        containerColor = JarvisBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = JarvisBlue) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            Card(colors = CardDefaults.cardColors(containerColor = JarvisSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = JarvisBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Your API keys are encrypted locally", color = Color.White, fontSize = 12.sp)
                    }
                    Text("Select your preferred AI provider. You can add multiple and switch anytime.", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (providers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(JarvisSurface, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                        Text("No providers configured", color = Color.White.copy(0.5f), fontSize = 13.sp)
                        Text("Tap + to add your first provider", color = Color.White.copy(0.3f), fontSize = 11.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(providers) { p ->
                        Card(colors = CardDefaults.cardColors(containerColor = JarvisSurface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(JarvisBlue.copy(0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text(p.providerType.take(2).uppercase(), color = JarvisBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${p.modelId} • ${if (p.isActive) "Active" else "Inactive"}", color = if (p.isActive) JarvisBlue else Color.White.copy(0.4f), fontSize = 11.sp)
                                }
                                if (p.isActive) Icon(Icons.Default.CheckCircle, null, tint = JarvisBlue, modifier = Modifier.size(20.dp))
                                IconButton(onClick = { vm.testProvider(p) }) { Icon(Icons.Default.NetworkCheck, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp)) }
                                IconButton(onClick = { vm.delete(p.id) }) { Icon(Icons.Default.Delete, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp)) }
                            }
                            if (!p.isActive) {
                                TextButton(onClick = { vm.setActive(p.id) }, modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)) { Text("Set as Active", color = JarvisBlue, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { nav.navigate(Routes.DASHBOARD) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue), shape = RoundedCornerShape(12.dp), enabled = providers.isNotEmpty()) {
                Text("CONTINUE TO JARVIS", fontWeight = FontWeight.Bold)
            }
            if (msg != null) {
                Snackbar(modifier = Modifier.padding(top = 8.dp)) { Text(msg ?: "") }
                LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); vm.clearMessage() }
            }
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = JarvisBlue)
        }
    }

    if (showAddDialog) {
        AddProviderDialog(onDismiss = { showAddDialog = false }, onSave = { type, key, model, url, name ->
            vm.addProvider(type, key, model, url, name); showAddDialog = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderDialog(onDismiss: () -> Unit, onSave: (ProviderType, String, String, String, String) -> Unit) {
    var selected by remember { mutableStateOf(ProviderType.GEMINI) }
    var expanded by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var modelId by remember { mutableStateOf(selected.defaultModel) }
    var baseUrl by remember { mutableStateOf(selected.defaultBaseUrl) }
    var displayName by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Provider") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = selected.displayName, onValueChange = {}, readOnly = true, label = { Text("Provider") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ProviderType.values().forEach { t ->
                        DropdownMenuItem(text = { Text(t.displayName) }, onClick = { selected = t; modelId = t.defaultModel; baseUrl = t.defaultBaseUrl; expanded = false })
                    }
                }
            }
            OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key *") },
                visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = modelId, onValueChange = { modelId = it }, label = { Text("Model ID *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }, confirmButton = {
        Button(onClick = { onSave(selected, apiKey, modelId, baseUrl, displayName) }, enabled = apiKey.isNotBlank() && modelId.isNotBlank()) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
