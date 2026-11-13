package com.jarvis.ai.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nav: NavController, vm: ChatViewModel = hiltViewModel()) {
    var input by remember { mutableStateOf("") }
    val messages by vm.messages.collectAsState()
    val convos by vm.conversations.collectAsState()
    val thinking by vm.isThinking.collectAsState()
    val error by vm.error.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showConvoDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.init("demo-user-id") }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.lastIndex) } }

    Scaffold(
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(
                title = { Text("JARVIS Chat", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                actions = {
                    IconButton(onClick = { vm.newConversation() }) { Icon(Icons.Default.Add, null, tint = JarvisBlue) }
                    IconButton(onClick = { showConvoDrawer = true }) { Icon(Icons.Default.History, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisBackground)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(JarvisSurface).padding(12.dp)) {
                if (error != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error ?: "", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.clearError() }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it }, placeholder = { Text("Ask Jarvis anything...", fontSize = 13.sp, color = Color.White.copy(0.4f)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue, unfocusedBorderColor = Color.White.copy(0.15f), focusedContainerColor = JarvisBackground, unfocusedContainerColor = JarvisBackground)
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = { if (input.isNotBlank() && !thinking) { vm.sendMessage(input); input = "" } },
                        containerColor = JarvisBlue, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(24.dp)
                    ) {
                        if (thinking) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize()) {
            if (messages.isEmpty() && !thinking) {
                Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SmartToy, null, tint = JarvisBlue.copy(0.5f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("How can I help you today?", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text("Ask coding, math, translation, or anything.", color = Color.White.copy(0.4f), fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    // Personality demo chips
                    listOf("Who created you?", "তুমি কে?", "Explain Android Compose").forEach { q ->
                        AssistChip(onClick = { input = q }, label = { Text(q, fontSize = 12.sp) }, modifier = Modifier.padding(4.dp))
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages) { m ->
                        val isUser = m.role == "user"
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                            if (!isUser) Box(modifier = Modifier.size(28.dp).background(JarvisBlue, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text("J", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(if (isUser) 48.dp else 8.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp, 16.dp, if (isUser) 4.dp else 16.dp, if (isUser) 16.dp else 4.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isUser) JarvisBlue else JarvisSurface),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(m.content, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                                    if (m.modelId != null) Text("${m.providerType} • ${m.modelId}", color = Color.White.copy(0.5f), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            if (isUser) Spacer(Modifier.width(8.dp))
                        }
                    }
                    if (thinking) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(28.dp).background(JarvisBlue.copy(0.5f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text("J", color = Color.White, fontSize = 12.sp) }
                                Spacer(Modifier.width(8.dp))
                                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface)) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = JarvisBlue, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("JARVIS is thinking...", color = Color.White.copy(0.6f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConvoDrawer) {
        ModalBottomSheet(onDismissRequest = { showConvoDrawer = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("Conversations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                if (convos.isEmpty()) Text("No conversations yet", color = Color.Gray, fontSize = 13.sp)
                else convos.forEach { c ->
                    ListItem(headlineContent = { Text(c.title, fontSize = 13.sp) }, supportingContent = { Text(c.id.take(8), fontSize = 10.sp) },
                        trailingContent = { IconButton(onClick = { vm.deleteConversation(c.id) }) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) } },
                        modifier = Modifier.clickable { vm.selectConversation(c.id); showConvoDrawer = false })
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.newConversation(); showConvoDrawer = false }, modifier = Modifier.fillMaxWidth()) { Text("New Conversation") }
            }
        }
    }
}
