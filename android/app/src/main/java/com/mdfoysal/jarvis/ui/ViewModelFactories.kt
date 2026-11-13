package com.mdfoysal.jarvis.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mdfoysal.jarvis.AppContainer
import com.mdfoysal.jarvis.feature.auth.AuthViewModel
import com.mdfoysal.jarvis.feature.chat.ChatViewModel
import com.mdfoysal.jarvis.feature.providers.ProvidersViewModel
import com.mdfoysal.jarvis.feature.settings.SettingsViewModel

/** Factories that wire the ViewModels to the manual dependency container. */
object JarvisViewModelFactories {

    fun auth(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { AuthViewModel(container.authRepository) }
    }

    fun providers(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ProvidersViewModel(
                providerRepository = container.providerRepository,
                settingsRepository = container.settingsRepository,
                sessionManager = container.sessionManager
            )
        }
    }

    fun chat(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ChatViewModel(
                chatRepository = container.chatRepository,
                providerRepository = container.providerRepository,
                authRepository = container.authRepository,
                settingsRepository = container.settingsRepository,
                sessionManager = container.sessionManager
            )
        }
    }

    fun settings(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                settingsRepository = container.settingsRepository,
                authRepository = container.authRepository,
                providerRepository = container.providerRepository,
                chatRepository = container.chatRepository,
                sessionManager = container.sessionManager
            )
        }
    }
}
