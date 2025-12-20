package com.example.whatappclone.presentation.screens.auth

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.StorageRepository
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.WhatsAppGreen
import com.example.whatappclone.ui.theme.WhatsAppTeal
import com.example.whatappclone.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var name by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("Hey there! I am using TalkNest") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }
    
    val profileCreationState by authViewModel.profileCreationState.collectAsState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImageUri = uri
    }
    
    LaunchedEffect(profileCreationState) {
        when (profileCreationState) {
            is Resource.Success -> {
                Toast.makeText(context, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                }
            }
            is Resource.Error -> {
                isLoading = false
                Toast.makeText(context, (profileCreationState as Resource.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    fun createProfile() {
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        val userId = authViewModel.getCurrentUserId() ?: return
        
        scope.launch {
            var profileImageUrl = ""
            
            // Upload profile image if selected
            profileImageUri?.let { uri ->
                val result = storageRepository.uploadProfileImage(uri, userId)
                result.onSuccess { url ->
                    profileImageUrl = url
                }.onFailure {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Create user profile with email from Firebase Auth
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val userEmail = currentUser?.email ?: ""
            
            val user = User(
                userId = userId,
                email = userEmail,
                phoneNumber = "",
                name = name,
                about = about,
                profileImageUrl = profileImageUrl
            )
            
            authViewModel.createUserProfile(user)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "Profile Info",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppTeal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Please provide your name and an optional profile photo",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Gray
                    )
                }
                
                // Camera Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Photo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 25) name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name (required)") },
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WhatsAppGreen,
                    focusedLabelColor = WhatsAppGreen
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // About Input
            OutlinedTextField(
                value = about,
                onValueChange = { if (it.length <= 139) about = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("About") },
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WhatsAppGreen,
                    focusedLabelColor = WhatsAppGreen
                ),
                supportingText = { Text("${about.length}/139") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Create Profile Button
            Button(
                onClick = { createProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "CREATE PROFILE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
