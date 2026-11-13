package com.jarvis.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.data.local.db.UserEntity
import com.jarvis.ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val user: UserEntity, val message: String? = null) : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val res = repo.login(email, password)
            _state.value = res.fold({ AuthState.Success(it) }, { AuthState.Error(it.message ?: "Login failed") })
        }
    }

    fun signup(email: String, password: String, name: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val res = repo.signUp(email, password, name)
            _state.value = res.fold({ AuthState.Success(it) }, { AuthState.Error(it.message ?: "Signup failed") })
        }
    }

    fun forgot(email: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val res = repo.forgotPassword(email)
            _state.value = res.fold({ AuthState.Success(UserEntity(email=email, displayName=""), it) }, { AuthState.Error(it.message ?: "Failed") })
        }
    }

    fun reset() { _state.value = AuthState.Idle }
}
