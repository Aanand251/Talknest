package com.example.whatappclone.presentation.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.WhatsAppTeal
import com.example.whatappclone.util.Resource
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    
    LaunchedEffect(key1 = authState, key2 = userProfile) {
        delay(2000) // Show splash for 2 seconds
        
        when {
            authState is Resource.Success && (authState as Resource.Success).data != null && userProfile != null -> {
                // User is logged in and profile exists
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            authState is Resource.Success && (authState as Resource.Success).data != null && userProfile == null -> {
                // User is logged in but profile doesn't exist
                navController.navigate(Screen.ProfileSetup.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            else -> {
                // User is not logged in - navigate to Email Auth
                navController.navigate(Screen.EmailAuth.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppTeal),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TalkNest",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Text(
            text = "from\nCHOUDHARY",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
