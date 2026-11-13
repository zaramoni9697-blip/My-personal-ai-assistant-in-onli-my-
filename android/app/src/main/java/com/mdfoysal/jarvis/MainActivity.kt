package com.mdfoysal.jarvis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mdfoysal.jarvis.feature.auth.AuthViewModel
import com.mdfoysal.jarvis.feature.auth.ForgotPasswordScreen
import com.mdfoysal.jarvis.feature.auth.LoginScreen
import com.mdfoysal.jarvis.feature.auth.SignUpScreen
import com.mdfoysal.jarvis.feature.chat.ChatScreen
import com.mdfoysal.jarvis.feature.chat.ChatViewModel
import com.mdfoysal.jarvis.feature.providers.ProviderEditScreen
import com.mdfoysal.jarvis.feature.providers.ProviderListScreen
import com.mdfoysal.jarvis.feature.providers.ProvidersViewModel
import com.mdfoysal.jarvis.feature.settings.PhoneControlScreen
import com.mdfoysal.jarvis.feature.settings.SettingsScreen
import com.mdfoysal.jarvis.feature.settings.SettingsViewModel
import com.mdfoysal.jarvis.feature.voice.WakeWordService
import com.mdfoysal.jarvis.ui.JarvisViewModelFactories
import com.mdfoysal.jarvis.ui.components.JarvisCore
import com.mdfoysal.jarvis.ui.components.CoreState
import com.mdfoysal.jarvis.ui.theme.JarvisBackgroundBrush
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisTextSecondary
import com.mdfoysal.jarvis.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }

        val container = (application as JarvisApp).container
        setContent {
            JarvisTheme {
                JarvisRoot(container = container, wakeIntent = intent)
            }
        }
    }
}

/** Session state used to decide which graph to show. */
private sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val userId: String) : SessionState
}

@Composable
private fun JarvisRoot(container: AppContainer, wakeIntent: Intent?) {
    val sessionState by container.authRepository.session
        .map { session ->
            if (session == null) SessionState.SignedOut else SessionState.SignedIn(session.userId)
        }
        .collectAsStateWithLifecycle(initialValue = SessionState.Loading)

    when (val current = sessionState) {
        is SessionState.Loading -> JarvisSplashScreen()
        is SessionState.SignedOut -> AuthGraph(container)
        is SessionState.SignedIn -> MainGraph(
            container = container,
            userId = current.userId,
            wakeTriggered = wakeIntent?.getBooleanExtra(WakeWordService.EXTRA_WAKE_TRIGGERED, false) == true
        )
    }
}

@Composable
private fun JarvisSplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash-scale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            JarvisCore(
                state = CoreState.IDLE,
                size = 180.dp,
                modifier = Modifier.scale(scale)
            )
            Spacer(Modifier.height(20.dp))
            Text("JARVIS AI", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Personal AI Assistant · Created by Md Foysal",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthGraph(container: AppContainer) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = JarvisViewModelFactories.auth(container))
    val state by authViewModel.state.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = state,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onTogglePassword = authViewModel::togglePasswordVisibility,
                onSignIn = authViewModel::signIn,
                onNavigateToSignUp = { navController.navigate(Routes.SIGNUP) },
                onNavigateToForgot = { navController.navigate(Routes.FORGOT) }
            )
        }
        composable(Routes.SIGNUP) {
            SignUpScreen(
                state = state,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                onDisplayNameChange = authViewModel::onDisplayNameChange,
                onTogglePassword = authViewModel::togglePasswordVisibility,
                onSignUp = authViewModel::signUp,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                state = state,
                onEmailChange = authViewModel::onEmailChange,
                onSubmit = authViewModel::requestPasswordReset,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainGraph(container: AppContainer, userId: String, wakeTriggered: Boolean) {
    val navController = rememberNavController()
    val providersViewModel: ProvidersViewModel = viewModel(factory = JarvisViewModelFactories.providers(container))
    val chatViewModel: ChatViewModel = viewModel(factory = JarvisViewModelFactories.chat(container))
    val settingsViewModel: SettingsViewModel = viewModel(factory = JarvisViewModelFactories.settings(container))

    val providerState by providersViewModel.state.collectAsStateWithLifecycle()
    val providerForm by providersViewModel.form.collectAsStateWithLifecycle()
    val chatState by chatViewModel.state.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        val hasProviders = container.providerRepository.hasConfiguration(userId)
        if (!hasProviders) {
            navController.navigate(Routes.PROVIDERS) { launchSingleTop = true }
        }
    }

    LaunchedEffect(wakeTriggered) {
        if (wakeTriggered) {
            chatViewModel.startListening()
        }
    }

    NavHost(navController = navController, startDestination = Routes.CHAT) {
        composable(Routes.CHAT) {
            ChatScreen(
                state = chatState,
                onInputChange = chatViewModel::onInputChange,
                onSend = chatViewModel::send,
                onStartListening = chatViewModel::startListening,
                onStopListening = chatViewModel::stopListening,
                onStopSpeaking = chatViewModel::stopSpeaking,
                onConfirmCommand = chatViewModel::confirmCommand,
                onDismissCommand = chatViewModel::dismissCommand,
                onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onClearConversation = chatViewModel::clearConversation
            )
        }

        composable(Routes.PROVIDERS) {
            ProviderListScreen(
                state = providerState,
                onAddProvider = { providerId ->
                    navController.navigate(Routes.providerEdit(providerId, null))
                },
                onEditProvider = { providerId, configId ->
                    navController.navigate(Routes.providerEdit(providerId, configId))
                },
                onSetActive = providersViewModel::setActive,
                onDeleteProvider = providersViewModel::delete,
                onContinue = {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.CHAT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.PROVIDER_EDIT,
            arguments = listOf(
                navArgument("providerId") { type = NavType.StringType },
                navArgument("configId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId").orEmpty()
            val configId = entry.arguments?.getString("configId")?.takeIf { it.isNotBlank() }
            LaunchedEffect(providerId, configId) {
                providersViewModel.loadForEditing(providerId, configId)
            }
            ProviderEditScreen(
                specs = providerState.specs,
                form = providerForm,
                onProviderSelected = providersViewModel::onProviderSelected,
                onNicknameChange = providersViewModel::onNicknameChange,
                onBaseUrlChange = providersViewModel::onBaseUrlChange,
                onChatPathChange = providersViewModel::onChatPathChange,
                onTestPathChange = providersViewModel::onTestPathChange,
                onModelChange = providersViewModel::onModelChange,
                onApiKeyChange = providersViewModel::onApiKeyChange,
                onToggleApiKeyVisibility = providersViewModel::toggleApiKeyVisibility,
                onAuthTypeChange = providersViewModel::onAuthTypeChange,
                onRequestStyleChange = providersViewModel::onRequestStyleChange,
                onTemperatureChange = providersViewModel::onTemperatureChange,
                onMaxTokensChange = providersViewModel::onMaxTokensChange,
                onResponseTextPathChange = providersViewModel::onResponseTextPathChange,
                onToggleStreaming = providersViewModel::toggleStreaming,
                onTestConnection = providersViewModel::testConnection,
                onSave = providersViewModel::save,
                onDelete = if (configId != null) {
                    {
                        providerState.configs.firstOrNull { it.id == configId }?.let {
                            providersViewModel.delete(it)
                        }
                        navController.popBackStack()
                    }
                } else null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = settingsState,
                onLanguageChange = settingsViewModel::setLanguage,
                onTtsEnabledChange = settingsViewModel::setTtsEnabled,
                onTtsRateChange = settingsViewModel::setTtsRate,
                onVoiceModeChange = settingsViewModel::setVoiceMode,
                onWakeWordEnabledChange = settingsViewModel::setWakeWordEnabled,
                onWakeWordPhraseChange = settingsViewModel::setWakeWordPhrase,
                onMemoryEnabledChange = settingsViewModel::setMemoryEnabled,
                onAddMemory = settingsViewModel::addMemory,
                onDeleteMemory = settingsViewModel::deleteMemory,
                onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                onOpenPhoneControl = { navController.navigate(Routes.PHONE) },
                onLogout = { settingsViewModel.logout { } },
                onDeleteAccount = { settingsViewModel.deleteAccount { } },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PHONE) {
            PhoneControlScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

/** Navigation routes of the JARVIS app. */
private object Routes {
    const val LOGIN = "auth/login"
    const val SIGNUP = "auth/signup"
    const val FORGOT = "auth/forgot"
    const val CHAT = "main/chat"
    const val PROVIDERS = "main/providers"
    const val SETTINGS = "main/settings"
    const val PHONE = "main/phone"

    const val PROVIDER_EDIT = "main/providers/{providerId}?configId={configId}"

    fun providerEdit(providerId: String, configId: String?): String =
        "main/providers/$providerId?configId=${configId.orEmpty()}"
}
