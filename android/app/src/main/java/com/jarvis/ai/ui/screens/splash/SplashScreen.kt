package com.jarvis.ai.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jarvis.ai.navigation.Routes
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(nav: NavController) {
    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (startAnim) 1f else 0.8f, animationSpec = tween(1200, easing = EaseOutBack), label = "scale")
    val alpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, animationSpec = tween(800), label = "alpha")
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "pulse")

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1800)
        nav.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF050A1A), JarvisBackground, Color(0xFF0A2A4A)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale).alpha(alpha)) {
            // Holographic core
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Box(modifier = Modifier.fillMaxSize().scale(pulse).background(Brush.radialGradient(listOf(JarvisBlue.copy(alpha = 0.3f), Color.Transparent)), CircleShape).blur(8.dp))
                Box(modifier = Modifier.size(110.dp).background(Brush.radialGradient(listOf(JarvisBlue, Color(0xFF003366))), CircleShape))
                Text("J", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            Text("JARVIS AI", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 4.sp)
            Text("by Md Foysal", fontSize = 12.sp, color = JarvisBlue, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text("Advanced Personal Assistant", fontSize = 13.sp, color = Color.White.copy(0.6f))
        }
        Text("v1.0.0 • Secure • Intelligent", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), fontSize = 10.sp, color = Color.White.copy(0.3f))
    }
}
