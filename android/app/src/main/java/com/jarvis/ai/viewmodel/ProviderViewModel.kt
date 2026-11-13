package com.jarvis.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.data.local.db.ProviderEntity
import com.jarvis.ai.data.local.datastore.PreferencesManager
import com.jarvis.ai.data.repository.ProviderRepository
import com.jarvis.ai.network.provider.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val repo: ProviderRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    val providers: StateFlow<List<ProviderEntity>> = _userId.filterNotNull().flatMapLatest { repo.observeProviders(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<String?>(null)
    val uiState: StateFlow<String?> = _uiState
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun setUserId(id: String) { _userId.value = id }

    fun addProvider(type: ProviderType, apiKey: String, model: String, baseUrl: String, displayName: String = "") {
        val uid = _userId.value ?: return
        viewModelScope.launch {
            _loading.value = true
            val res = repo.saveProvider(uid, type, displayName, apiKey, model, baseUrl)
            _uiState.value = res.fold({ "Provider added: ${type.displayName}" }, { it.message })
            _loading.value = false
        }
    }

    fun testProvider(entity: ProviderEntity) {
        viewModelScope.launch {
            _loading.value = true
            val r = repo.testConnection(entity)
            _uiState.value = r.fold({ it }, { "Test failed: ${it.message}" })
            _loading.value = false
        }
    }

    fun setActive(id: String) {
        val uid = _userId.value ?: return
        viewModelScope.launch { repo.setActive(uid, id); _uiState.value = "Active provider updated" }
    }

    fun delete(id: String) { viewModelScope.launch { repo.deleteProvider(id); _uiState.value = "Deleted" } }

    fun clearMessage() { _uiState.value = null }
}
