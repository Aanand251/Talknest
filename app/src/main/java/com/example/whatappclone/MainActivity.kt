package com.example.whatappclone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.whatappclone.data.repository.AuthRepository
import com.example.whatappclone.presentation.navigation.NavGraph
import com.example.whatappclone.ui.theme.WhatappCloneTheme
import com.example.whatappclone.util.PresenceManager
import com.example.whatappclone.utils.OnlineStatusManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var presenceManager: PresenceManager
    private val authRepository = AuthRepository()
    private var navController: NavHostController? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        presenceManager = PresenceManager(this)
        
        // Set user online for calling
        OnlineStatusManager.setUserOnline()
        
        setContent {
            WhatappCloneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    navController = rememberNavController()
                    NavGraph(navController = navController!!)
                    
                    // Handle incoming call intent
                    LaunchedEffect(Unit) {
                        handleIncomingCallIntent(intent)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingCallIntent(intent)
    }
    
    private fun handleIncomingCallIntent(intent: Intent?) {
        val callId = intent?.getStringExtra("CALL_ID")
        val isIncoming = intent?.getBooleanExtra("IS_INCOMING", false) ?: false
        
        if (callId != null) {
            Log.d("MainActivity", "Handling call intent - callId: $callId, isIncoming: $isIncoming")
            
            // Navigate to call screen
            navController?.navigate("call/$callId/$isIncoming") {
                // Clear any existing call screens
                popUpTo(0) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            authRepository.getCurrentUserId()?.let { userId ->
                presenceManager.setUserOnline(userId)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            authRepository.getCurrentUserId()?.let { userId ->
                presenceManager.setUserOffline(userId)
            }
        }
    }
}