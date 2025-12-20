package com.example.whatappclone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color.White,
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    tertiary = WhatsAppBlue,
    background = BackgroundDark,
    surface = ChatBubbleReceivedDark,
    onSurface = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    error = ErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppTeal,
    onPrimary = Color.White,
    secondary = WhatsAppGreen,
    onSecondary = Color.White,
    tertiary = WhatsAppBlue,
    background = BackgroundLight,
    surface = Color.White,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    error = ErrorColor
)

@Composable
fun WhatappCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = if (darkTheme) {
                BackgroundDark.toArgb()
            } else {
                WhatsAppTeal.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}