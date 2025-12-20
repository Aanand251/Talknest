package com.example.whatappclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        presenceManager = PresenceManager(this)
        
        // Set user online for calling
        OnlineStatusManager.setUserOnline()
        
        setContent {
            WhatappCloneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
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