package com.example.whatappclone.presentation.screens.call

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.whatappclone.data.model.CallStatus
import com.example.whatappclone.data.model.CallType
import com.example.whatappclone.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    navController: NavController,
    callId: String,
    isIncoming: Boolean = false,
    viewModel: CallViewModel = viewModel()
) {
    // Request microphone and camera permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )
    
    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
    
    val callState by viewModel.callState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val callDuration by viewModel.callDuration.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Determine if this is caller or receiver - use isIncoming parameter
    val isCaller = !isIncoming
    val isVideoCall = callState?.callType == CallType.VIDEO
    
    // Show permission dialog if not granted
    if (!permissionsState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("Permissions Required") },
            text = { Text("Microphone and Camera permissions are required for calling.") },
            confirmButton = {
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Cancel")
                }
            }
        )
        return
    }
    
    LaunchedEffect(callId) {
        // First load the call to get its details
        scope.launch {
            val repository = com.example.whatappclone.data.repository.ImprovedCallRepository(context)
            val call = repository.getCall(callId)
            
            if (call != null) {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                
                if (call.callerId == currentUserId) {
                    // Caller - person who initiated the call
                    viewModel.loadCallAsCaller(callId, call.callType == CallType.VIDEO)
                } else {
                    // Receiver - person receiving the call
                    viewModel.loadCallAsReceiver(callId, call.callType == CallType.VIDEO)
                }
            }
        }
    }
    
    LaunchedEffect(callState?.callStatus) {
        if (callState?.callStatus == CallStatus.ENDED || 
            callState?.callStatus == CallStatus.REJECTED) {
            delay(1000)
            navController.popBackStack()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryPurple, PrimaryViolet, PrimaryIndigo)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Call info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                // User avatar
                AsyncImage(
                    model = callState?.receiverImage?.ifEmpty { 
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=${callState?.receiverName}"
                    },
                    contentDescription = "Caller",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Caller name
                Text(
                    text = callState?.receiverName ?: "Unknown",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Call status
                Text(
                    text = when (callState?.callStatus) {
                        CallStatus.RINGING -> "Ringing..."
                        CallStatus.CONNECTING -> "Connecting..."
                        CallStatus.CONNECTED -> formatDuration(callDuration)
                        CallStatus.ENDED -> "Call Ended"
                        CallStatus.REJECTED -> "Call Rejected"
                        CallStatus.NO_ANSWER -> "No Answer"
                        else -> "Calling..."
                    },
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Call type indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (callState?.callType == CallType.VIDEO) 
                            Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (callState?.callType == CallType.VIDEO) "Video Call" else "Voice Call",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Middle section - Video preview (for video calls)
            if (callState?.callType == CallType.VIDEO && isVideoEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video Preview",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            // Bottom section - Call controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Main controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute button
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        onClick = { viewModel.toggleMute() }
                    )
                    
                    // Speaker button
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (isSpeakerOn) "Speaker On" else "Speaker Off",
                        isActive = isSpeakerOn,
                        onClick = { viewModel.toggleSpeaker() }
                    )
                    
                    // Video button (only for video calls)
                    if (callState?.callType == CallType.VIDEO) {
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = if (isVideoEnabled) "Video On" else "Video Off",
                            isActive = !isVideoEnabled,
                            onClick = { viewModel.toggleVideo() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // End call button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewModel.endCall()
                            delay(500)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = Color.Red,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = if (isActive) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f),
            shape = CircleShape
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
