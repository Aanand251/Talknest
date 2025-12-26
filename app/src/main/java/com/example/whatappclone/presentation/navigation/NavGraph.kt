package com.example.whatappclone.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.whatappclone.presentation.screens.auth.EmailAuthScreen
import com.example.whatappclone.presentation.screens.auth.PhoneAuthScreen
import com.example.whatappclone.presentation.screens.auth.ProfileSetupScreen
import com.example.whatappclone.presentation.screens.chats.ChatScreen
import com.example.whatappclone.presentation.screens.contacts.FriendsListScreen
import com.example.whatappclone.presentation.screens.home.HomeScreen
import com.example.whatappclone.presentation.screens.splash.SplashScreen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.presentation.viewmodel.ChatViewModel
import com.example.whatappclone.presentation.viewmodel.StatusViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    val authViewModel: AuthViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val statusViewModel: StatusViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.EmailAuth.route) {
            EmailAuthScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.PhoneAuth.route) {
            PhoneAuthScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel,
                chatViewModel = chatViewModel,
                statusViewModel = statusViewModel
            )
        }
        
        composable(Screen.Settings.route) {
            com.example.whatappclone.presentation.screens.settings.SettingsScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.AddContact.route) {
            com.example.whatappclone.presentation.screens.contacts.AddContactScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.ContactRequests.route) {
            com.example.whatappclone.presentation.screens.contacts.ContactRequestsScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.ProfileQR.route) {
            com.example.whatappclone.presentation.screens.qr.ProfileQRScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.QRScanner.route) {
            com.example.whatappclone.presentation.screens.qr.QRScannerScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            ChatScreen(
                navController = navController,
                chatId = chatId,
                otherUserId = otherUserId,
                chatViewModel = chatViewModel,
                authViewModel = authViewModel
            )
        }
        
        composable(route = Screen.FriendsList.route) {
            FriendsListScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("isIncoming") { 
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val isIncoming = backStackEntry.arguments?.getBoolean("isIncoming") ?: false
            com.example.whatappclone.presentation.screens.call.CallScreen(
                navController = navController,
                callId = callId,
                isIncoming = isIncoming
            )
        }
    }
}
