package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MessageVaultApp
import com.example.R
import com.example.ml.TranslationManager
import com.example.ui.theme.DarkEmerald
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.util.PreferenceManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    translationManager: TranslationManager,
    preferenceManager: PreferenceManager,
    onSplashFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.96f) }
    var progress by remember { mutableFloatStateOf(0.1f) }
    var statusText by remember { mutableStateOf("جاري تهيئة الرسائل المحذوفة...") }

    // Pulsing green ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    LaunchedEffect(Unit) {
        // Initial fade-in and subtle zoom
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        scaleAnim.animateTo(
            targetValue = 1.01f,
            animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
        )
    }

    LaunchedEffect(Unit) {
        // Step 1: Database readiness
        statusText = "جاري تهيئة قاعدة البيانات المشفرة..."
        progress = 0.3f
        try {
            MessageVaultApp.get().database.openHelper.readableDatabase
        } catch (e: Exception) {
            // gracefully ignored
        }
        delay(400)

        // Step 2: System preferences and Notification Access
        statusText = "فحص حالة الصلاحيات والخدمة..."
        progress = 0.65f
        preferenceManager.isNotificationAccessGranted()
        delay(400)

        // Step 3: Check ML Kit translation models
        statusText = "تهيئة محرك الترجمة والأمان..."
        progress = 0.9f
        try {
            translationManager.checkDownloadedModels()
        } catch (e: Exception) {
            // non-fatal
        }
        delay(350)

        // Step 4: Finalize
        progress = 1.0f
        statusText = "الخزينة جاهزة للعمل ✓"
        delay(250)

        // Smooth fade out
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // Ambient Radial Glow behind the artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonGreen.copy(alpha = glowAlpha * 0.35f),
                            DarkEmerald.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        // Full Screen Splash Image preserving artwork, Waleed Al-khatib branding, and proportions
        Image(
            painter = painterResource(id = R.drawable.splash_image),
            contentDescription = "شاشة الافتتاح - الرسائل المحذوفة Waleed Al-khatib",
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value),
            contentScale = ContentScale.Crop
        )

        // Bottom Loading Bar & Status Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 28.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon Green Horizontal Progress Indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonGreen,
                trackColor = DarkEmerald.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = statusText,
                color = TextPrimaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
