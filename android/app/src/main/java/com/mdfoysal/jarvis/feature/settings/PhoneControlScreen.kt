package com.mdfoysal.jarvis.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mdfoysal.jarvis.feature.device.DeviceController
import com.mdfoysal.jarvis.feature.device.InstalledApp
import com.mdfoysal.jarvis.ui.components.GlassCard
import com.mdfoysal.jarvis.ui.components.SectionHeader
import com.mdfoysal.jarvis.ui.components.StatusPill
import com.mdfoysal.jarvis.ui.theme.JarvisBackgroundBrush
import com.mdfoysal.jarvis.ui.theme.JarvisDanger
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisTextSecondary
import com.mdfoysal.jarvis.ui.theme.JarvisTeal

/**
 * Phone control and permissions.
 *
 * The screen explains exactly what each permission is used for and how to grant it — JARVIS
 * never silently takes a capability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneControlScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { DeviceController(context) }
    var permissionState by remember { mutableStateOf(PermissionSnapshot.grantState(context)) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionState = PermissionSnapshot.grantState(context) }

    LaunchedEffect(Unit) {
        apps = runCatching { controller.installedApps() }.getOrDefault(emptyList())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Phone control") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Back", color = JarvisHoloBlue) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(JarvisBackgroundBrush)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "What JARVIS can do",
                            subtitle = "Everything here uses public Android APIs. Actions that change " +
                                "something always ask for your confirmation first."
                        )
                        Spacer(Modifier.height(12.dp))
                        QuickAction("Turn the flashlight on") {
                            controller.setFlashlight(true).message
                        }
                        QuickAction("Turn the flashlight off") {
                            controller.setFlashlight(false).message
                        }
                        QuickAction("Raise the volume") { controller.adjustVolume(true).message }
                        QuickAction("Lower the volume") { controller.adjustVolume(false).message }
                        QuickAction("Mute the volume") { controller.muteVolume().message }
                        QuickAction("Battery status") { controller.batteryInfo().message }
                        QuickAction("Device information") { controller.deviceInfo().message }
                        QuickAction("Open system settings") { controller.openSettings().message }
                        QuickAction("Read notifications") { controller.readNotifications().message }
                    }
                }
            }

            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = "Permissions",
                            subtitle = "Tap a permission to grant it. JARVIS works without them, " +
                                "only the matching feature is unavailable."
                        )
                        Spacer(Modifier.height(12.dp))
                        PermissionRow(
                            title = "Microphone",
                            explanation = "Used only while you are listening, or while the visible " +
                                "wake-word notification is on screen.",
                            granted = permissionState.microphone,
                            onRequest = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            }
                        )
                        PermissionRow(
                            title = "Notifications",
                            explanation = "Shows the “JARVIS is listening” notification required by " +
                                "Android for background microphone access.",
                            granted = permissionState.notifications,
                            onRequest = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        )
                        PermissionRow(
                            title = "Camera",
                            explanation = "Android requires camera access before an app can switch the " +
                                "flashlight on or off.",
                            granted = permissionState.camera,
                            onRequest = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        )
                        PermissionRow(
                            title = "Phone (direct calls)",
                            explanation = "Lets JARVIS place a call directly. Without it JARVIS opens " +
                                "the dialler and you press call.",
                            granted = permissionState.phone,
                            onRequest = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                            }
                        )
                        PermissionRow(
                            title = "Messages (direct send)",
                            explanation = "Lets JARVIS send a message directly. Without it JARVIS opens " +
                                "the message composer and you press send.",
                            granted = permissionState.sms,
                            onRequest = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = JarvisHoloBlue
                            )
                        ) { Text("Notification access settings") }
                    }
                }
            }

            statusMessage?.let { message ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        StatusPill(text = message, color = JarvisTeal, showDot = false)
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Installed apps",
                    subtitle = "Tap an app to let JARVIS open it (${apps.size} available)."
                )
            }

            items(apps.take(60)) { app ->
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                statusMessage = controller.openApp(app.label).message
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisHoloBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, action: () -> String) {
    var result by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedButton(
            onClick = { result = action() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = JarvisHoloBlue)
        ) { Text(label) }
        result?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextSecondary,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    explanation: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = JarvisTextSecondary)
            Spacer(Modifier.height(4.dp))
            if (granted) {
                StatusPill(text = "Granted", color = JarvisTeal, showDot = false)
            } else {
                TextButton(onClick = onRequest) {
                    Text("Grant permission", color = JarvisHoloBlue)
                }
            }
        }
    }
}

/** Snapshot of the permission state used by the screen. */
private data class PermissionSnapshot(
    val microphone: Boolean,
    val notifications: Boolean,
    val camera: Boolean,
    val phone: Boolean,
    val sms: Boolean
) {
    companion object {
        fun grantState(context: android.content.Context): PermissionSnapshot {
            fun granted(permission: String) = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            return PermissionSnapshot(
                microphone = granted(Manifest.permission.RECORD_AUDIO),
                notifications = granted(Manifest.permission.POST_NOTIFICATIONS),
                camera = granted(Manifest.permission.CAMERA),
                phone = granted(Manifest.permission.CALL_PHONE),
                sms = granted(Manifest.permission.SEND_SMS)
            )
        }
    }
}
