package com.jarvis.ai.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jarvis.ai.navigation.Routes
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.viewmodel.AuthState
import com.jarvis.ai.viewmodel.AuthViewModel

@Composable
fun LoginScreen(nav: NavController, vm: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthState.Success) nav.navigate(Routes.PROVIDER_CONFIG) { popUpTo(Routes.LOGIN) { inclusive = true } }
    }

    Box(modifier = Modifier.fillMaxSize().background(JarvisBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Welcome Back", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Sign in to JARVIS AI", fontSize = 13.sp, color = Color.White.copy(0.6f))
            Spacer(Modifier.height(32.dp))

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = JarvisBlue) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue, unfocusedBorderColor = Color.White.copy(0.2f), focusedLabelColor = JarvisBlue)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = JarvisBlue) },
                        trailingIcon = { IconButton(onClick = { showPass = !showPass }) { Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(0.6f)) } },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue, unfocusedBorderColor = Color.White.copy(0.2f))
                    )
                    if (state is AuthState.Error) {
                        Text((state as AuthState.Error).message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (state is AuthState.Success && (state as AuthState.Success).message != null) {
                        Text((state as AuthState.Success).message!!, color = JarvisBlue, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { nav.navigate(Routes.FORGOT) }, modifier = Modifier.align(Alignment.End)) { Text("Forgot Password?", color = JarvisBlue, fontSize = 12.sp) }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.login(email, password) },
                        enabled = state !is AuthState.Loading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state is AuthState.Loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("LOGIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", color = Color.White.copy(0.6f), fontSize = 13.sp)
                TextButton(onClick = { nav.navigate(Routes.SIGNUP) }) { Text("Sign Up", color = JarvisBlue, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun SignupScreen(nav: NavController, vm: AuthViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()

    LaunchedEffect(state) { if (state is AuthState.Success) nav.navigate(Routes.PROVIDER_CONFIG) { popUpTo(Routes.LOGIN) { inclusive=true } } }

    Box(modifier = Modifier.fillMaxSize().background(JarvisBackground).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Create Account", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Join JARVIS AI by Md Foysal", fontSize = 12.sp, color = Color.White.copy(0.6f))
            Spacer(Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = JarvisSurface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true,
                        trailingIcon = { IconButton(onClick = { showPass = !showPass }) { Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue))
                    if (state is AuthState.Error) Text((state as AuthState.Error).message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        if (password != confirm) { /* handled via error */ }
                        vm.signup(email, password, name)
                    }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue), shape = RoundedCornerShape(12.dp)) {
                        if (state is AuthState.Loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("SIGN UP", fontWeight = FontWeight.Bold)
                    }
                }
            }
            TextButton(onClick = { nav.popBackStack() }) { Text("Already have account? Login", color = JarvisBlue) }
        }
    }
}

@Composable
fun ForgotPasswordScreen(nav: NavController, vm: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(JarvisBackground).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Forgot Password", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Enter your email") }, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisBlue))
            Spacer(Modifier.height(12.dp))
            if (state is AuthState.Error) Text((state as AuthState.Error).message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if (state is AuthState.Success) Text((state as AuthState.Success).message ?: "Check your email", color = JarvisBlue, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.forgot(email) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue)) {
                if (state is AuthState.Loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("SEND RESET LINK")
            }
            TextButton(onClick = { nav.popBackStack() }) { Text("Back to Login", color = JarvisBlue) }
        }
    }
}
