package com.mdfoysal.jarvis

import android.content.Context
import com.mdfoysal.jarvis.core.chat.ChatEngine
import com.mdfoysal.jarvis.data.local.JarvisDatabase
import com.mdfoysal.jarvis.data.local.SettingsStore
import com.mdfoysal.jarvis.data.remote.JarvisBackendClient
import com.mdfoysal.jarvis.data.remote.OkHttpTransport
import com.mdfoysal.jarvis.data.repository.AuthRepository
import com.mdfoysal.jarvis.data.repository.ChatRepository
import com.mdfoysal.jarvis.data.repository.ProviderRepository
import com.mdfoysal.jarvis.data.repository.ProviderSpecService
import com.mdfoysal.jarvis.data.repository.SettingsRepository
import com.mdfoysal.jarvis.data.security.CredentialVault
import com.mdfoysal.jarvis.data.security.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Manual dependency container.
 *
 * The project deliberately avoids a DI framework: the graph is small, explicit and easy to
 * follow, which keeps the build free of annotation processors beyond Room's.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: JarvisDatabase by lazy { JarvisDatabase.build(appContext) }
    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager(appContext) }
    val credentialVault: CredentialVault by lazy { CredentialVault(appContext) }
    val transport: OkHttpTransport by lazy { OkHttpTransport() }
    val chatEngine: ChatEngine by lazy { ChatEngine(transport) }
    val providerSpecService: ProviderSpecService by lazy { ProviderSpecService(appContext) }

    /** Cached backend URL so the HTTP client can read it synchronously. */
    private val serverUrl = settingsStore.settings
        .map { it.serverUrl }
        .stateIn(appScope, SharingStarted.Eagerly, "")

    val backendClient: JarvisBackendClient by lazy {
        JarvisBackendClient(transport) { serverUrl.value }
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            userDao = database.userDao(),
            sessionManager = sessionManager,
            settingsStore = settingsStore,
            credentialVault = credentialVault,
            providerConfigDao = database.providerConfigDao(),
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            memoryDao = database.memoryDao(),
            backendClient = backendClient
        )
    }

    val providerRepository: ProviderRepository by lazy {
        ProviderRepository(
            dao = database.providerConfigDao(),
            vault = credentialVault,
            settingsStore = settingsStore,
            specService = providerSpecService,
            chatEngine = chatEngine
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            memoryDao = database.memoryDao(),
            providerRepository = providerRepository,
            chatEngine = chatEngine,
            settingsStore = settingsStore
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(settingsStore) }
}
