package com.jarvis.ai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.ai.ui.screens.auth.ForgotPasswordScreen
import com.jarvis.ai.ui.screens.auth.LoginScreen
import com.jarvis.ai.ui.screens.auth.SignupScreen
import com.jarvis.ai.ui.screens.chat.ChatScreen
import com.jarvis.ai.ui.screens.config.ProviderConfigScreen
import com.jarvis.ai.ui.screens.dashboard.DashboardScreen
import com.jarvis.ai.ui.screens.device.DeviceControlScreen
import com.jarvis.ai.ui.screens.settings.SettingsScreen
import com.jarvis.ai.ui.screens.splash.SplashScreen
import com.jarvis.ai.ui.screens.voice.VoiceAssistantScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FORGOT = "forgot"
    const val PROVIDER_CONFIG = "provider_config"
    const val DASHBOARD = "dashboard"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val DEVICE = "device"
    const val SETTINGS = "settings"
}

@Composable
fun JarvisNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(nav) }
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.SIGNUP) { SignupScreen(nav) }
        composable(Routes.FORGOT) { ForgotPasswordScreen(nav) }
        composable(Routes.PROVIDER_CONFIG) { ProviderConfigScreen(nav) }
        composable(Routes.DASHBOARD) { DashboardScreen(nav) }
        composable(Routes.CHAT) { ChatScreen(nav) }
        composable(Routes.VOICE) { VoiceAssistantScreen(nav) }
        composable(Routes.DEVICE) { DeviceControlScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
    }
}
