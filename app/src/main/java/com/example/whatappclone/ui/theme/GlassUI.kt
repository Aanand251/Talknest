package com.example.whatappclone.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 🎨 Glass UI - Crystallic, Glassy, Shiny Effects
 */

// ✨ Beautiful Gradient Colors
object GlassColors {
    // Crystal Glass gradients
    val CrystalGradient = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2)
    )
    
    val AuroraGradient = listOf(
        Color(0xFF4158D0),
        Color(0xFFC850C0),
        Color(0xFFFFCC70)
    )
    
    val SunsetGradient = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFFE66D),
        Color(0xFF4ECDC4)
    )
    
    val OceanGradient = listOf(
        Color(0xFF667EEA),
        Color(0xFF4ECDC4),
        Color(0xFF44A08D)
    )
    
    val PurpleDreamGradient = listOf(
        Color(0xFFA8EDEA),
        Color(0xFFFED6E3)
    )
    
    val NeonGradient = listOf(
        Color(0xFF00F5A0),
        Color(0xFF00D9F5)
    )
    
    // Dark mode glass colors
    val DarkGlassGradient = listOf(
        Color(0x40FFFFFF),
        Color(0x20FFFFFF)
    )
    
    val WhatsAppGlassGradient = listOf(
        Color(0x8025D366),
        Color(0x6000BFA5)
    )
}

/**
 * 💎 Glass Surface - Beautiful glassmorphism effect
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    gradient: List<Color> = GlassColors.CrystalGradient,
    alpha: Float = 0.15f,
    blur: Dp = 16.dp,
    borderAlpha: Float = 0.3f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = gradient.map { it.copy(alpha = alpha) }
                )
            )
            .blur(blur / 4)
    ) {
        // Border highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color.White.copy(alpha = 0f)
                        )
                    )
                )
        )
        
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * ✨ Shimmer Effect - Beautiful loading animation
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.3f to Color.White.copy(alpha = 0.3f),
                    0.5f to Color.White.copy(alpha = 0.5f),
                    0.7f to Color.White.copy(alpha = 0.3f),
                    1f to Color.Transparent
                )
            )
    ) {
        content()
    }
}

/**
 * 🎭 Scale In Animation - Beautiful entry animation
 */
@Composable
fun ScaleInAnimation(
    visible: Boolean = true,
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = tween(durationMillis, delayMillis),
            initialScale = 0.8f
        ) + fadeIn(
            animationSpec = tween(durationMillis, delayMillis)
        ),
        exit = scaleOut(
            animationSpec = tween(durationMillis / 2),
            targetScale = 0.8f
        ) + fadeOut(
            animationSpec = tween(durationMillis / 2)
        )
    ) {
        content()
    }
}

/**
 * 🌊 Slide In Animation - Beautiful slide entry
 */
@Composable
fun SlideInAnimation(
    visible: Boolean = true,
    durationMillis: Int = 400,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis, delayMillis, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 3 }
        ) + fadeIn(
            animationSpec = tween(durationMillis, delayMillis)
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis / 2),
            targetOffsetY = { -it / 3 }
        ) + fadeOut(
            animationSpec = tween(durationMillis / 2)
        )
    ) {
        content()
    }
}

/**
 * 💫 Pulsating Effect - Eye-catching attention animation
 */
@Composable
fun PulsatingEffect(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1000,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(modifier = modifier.scale(scale)) {
        content()
    }
}

/**
 * 🌈 Gradient Background - Beautiful app background
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    gradient: List<Color> = GlassColors.AuroraGradient,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradient
                )
            )
    ) {
        content()
    }
}

/**
 * 💎 Glass Card - Beautiful card with glass effect
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradient: List<Color> = GlassColors.WhatsAppGlassGradient,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        onClick = { onClick?.invoke() },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = gradient
                    )
                )
                .padding(16.dp)
        ) {
            content()
        }
    }
}
