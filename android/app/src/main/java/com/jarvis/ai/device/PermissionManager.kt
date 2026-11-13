package com.jarvis.ai.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(@ApplicationContext private val context: Context) {

    data class AppPermission(val permission: String, val title: String, val description: String, val required: Boolean)

    val allPermissions = listOf(
        AppPermission(Manifest.permission.RECORD_AUDIO, "Microphone", "For voice commands and wake word detection", true),
        AppPermission(Manifest.permission.CAMERA, "Camera", "For image understanding and flashlight control", false),
        AppPermission(Manifest.permission.CALL_PHONE, "Phone", "To make calls via assistant", false),
        AppPermission(Manifest.permission.SEND_SMS, "SMS", "To send messages via assistant", false),
        AppPermission(Manifest.permission.READ_CONTACTS, "Contacts", "To find contacts for calling/messaging", false),
        AppPermission(Manifest.permission.POST_NOTIFICATIONS, "Notifications", "For foreground service and reminders", true),
    )

    fun isGranted(permission: String): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun checkAll(): Map<String,Boolean> = allPermissions.associate { it.permission to isGranted(it.permission) }
}
