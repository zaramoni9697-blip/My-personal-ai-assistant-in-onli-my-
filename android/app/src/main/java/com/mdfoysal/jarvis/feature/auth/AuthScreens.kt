package com.mdfoysal.jarvis.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mdfoysal.jarvis.ui.components.GlassCard
import com.mdfoysal.jarvis.ui.components.JarvisCore
import com.mdfoysal.jarvis.ui.components.SectionHeader
import com.mdfoysal.jarvis.ui.components.StatusPill
import com.mdfoysal.jarvis.ui.theme.JarvisBackgroundBrush
import com.mdfoysal.jarvis.ui.theme.JarvisDanger
import com.mdfoysal.jarvis.ui.theme.JarvisHoloBlue
import com.mdfoysal.jarvis.ui.theme.JarvisTextSecondary

/** Shared background with the animated JARVIS core. */
@Composable
fun JarvisAuthBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackgroundBrush)
    ) {
        content()
    }
}

/** Wordmark used on the auth screens. */
@Composable
fun JarvisWordmark(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        JarvisCore(state = CoreState.IDLE, size = 132.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "JARVIS AI",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Personal AI Assistant · Created by Md Foysal",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = when {
            !isPassword -> VisualTransformation.None
            passwordVisible -> VisualTransformation.None
            else -> PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = if (onToggleVisibility != null) {
            {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisHoloBlue,
            unfocusedBorderColor = JarvisHoloBlue.copy(alpha = 0.35f),
            focusedLabelColor = JarvisHoloBlue,
            cursorColor = JarvisHoloBlue
        )
    )
}

@Composable
private fun AuthMessage(error: String?, message: String?) {
    if (error != null) {
        StatusPill(text = error, color = JarvisDanger, showDot = false)
    } else if (message != null) {
        StatusPill(text = message, color = JarvisHoloBlue, showDot = false)
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = JarvisHoloBlue,
            contentColor = MaterialTheme.colorScheme.background
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Login screen. */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgot: () -> Unit
) {
    JarvisAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JarvisWordmark()
            Spacer(Modifier.height(28.dp))

            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(title = "Welcome back", subtitle = "Sign in to your JARVIS workspace")
                    Spacer(Modifier.height(18.dp))
                    AuthTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = state.passwordVisible,
                        onToggleVisibility = onTogglePassword
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onNavigateToForgot) {
                            Text("Forgot password?", color = JarvisHoloBlue)
                        }
                    }
                    AuthMessage(state.error, state.message)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(text = "Sign in", loading = state.isLoading, onClick = onSignIn)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onNavigateToSignUp) {
                            Text("No account? Create one", color = JarvisTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** Sign-up screen. */
@Composable
fun SignUpScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    JarvisAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JarvisWordmark()
            Spacer(Modifier.height(24.dp))
            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title = "Create your account",
                        subtitle = "Your account, keys and conversations stay private to you."
                    )
                    Spacer(Modifier.height(18.dp))
                    AuthTextField(
                        value = state.displayName,
                        onValueChange = onDisplayNameChange,
                        label = "Display name"
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = state.passwordVisible,
                        onToggleVisibility = onTogglePassword,
                        supportingText = "At least 8 characters"
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthTextField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Confirm password",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        passwordVisible = state.passwordVisible
                    )
                    Spacer(Modifier.height(10.dp))
                    AuthMessage(state.error, state.message)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(text = "Create account", loading = state.isLoading, onClick = onSignUp)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onNavigateToLogin) {
                            Text("Already have an account? Sign in", color = JarvisTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** Password reset screen. */
@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    JarvisAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title = "Reset password",
                        subtitle = "We will send reset instructions to your account email."
                    )
                    Spacer(Modifier.height(18.dp))
                    AuthTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(Modifier.height(10.dp))
                    AuthMessage(state.error, state.message)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(text = "Send reset instructions", loading = state.isLoading, onClick = onSubmit)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onNavigateBack) {
                            Text("Back to sign in", color = JarvisTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
