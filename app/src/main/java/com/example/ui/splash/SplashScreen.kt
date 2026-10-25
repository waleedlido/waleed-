package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in animation
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        // Hold for the remainder of the 2 seconds total duration
        delay(1400)
        // Fade out
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040B06))
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_image),
            contentDescription = "شاشة الافتتاح - الرسائل المحذوفة Waleed Al-khatib",
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value),
            contentScale = ContentScale.Crop
        )
    }
}
