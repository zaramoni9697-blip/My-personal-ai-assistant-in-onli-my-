package com.mdfoysal.jarvis.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdfoysal.jarvis.data.repository.AuthRepository
import com.mdfoysal.jarvis.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the authentication screens. */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val signedIn: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onConfirmPasswordChange(value: String) =
        _state.update { it.copy(confirmPassword = value, error = null) }

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, error = null) }

    fun togglePasswordVisibility() =
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }

    fun signIn() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.signIn(current.email, current.password)) {
                is AuthResult.Success -> _state.update {
                    it.copy(isLoading = false, signedIn = true, password = "", message = result.message)
                }
                is AuthResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun signUp() {
        val current = _state.value
        if (current.isLoading) return
        if (current.password != current.confirmPassword) {
            _state.update { it.copy(error = "The two passwords do not match") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.signUp(current.email, current.password, current.displayName)) {
                is AuthResult.Success -> _state.update {
                    it.copy(isLoading = false, signedIn = true, password = "", confirmPassword = "")
                }
                is AuthResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun requestPasswordReset() {
        val current = _state.value
        if (current.email.isBlank()) {
            _state.update { it.copy(error = "Enter the email address of your account first") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.requestPasswordReset(current.email)) {
                is AuthResult.Success -> _state.update {
                    it.copy(isLoading = false, message = result.message ?: "Reset instructions sent")
                }
                is AuthResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
