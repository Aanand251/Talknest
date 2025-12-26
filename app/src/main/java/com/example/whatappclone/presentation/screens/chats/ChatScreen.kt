package com.example.whatappclone.presentation.screens.chats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.whatappclone.data.model.MessageStatus
import com.example.whatappclone.data.model.MessageType
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.AuthRepository
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.presentation.viewmodel.ChatViewModel
import com.example.whatappclone.ui.theme.*
import com.example.whatappclone.util.DateTimeUtil
import com.example.whatappclone.util.DownloadUtil
import com.example.whatappclone.util.PresenceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String,
    otherUserId: String,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val currentUserId = authViewModel.getCurrentUserId()
    var messageText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var otherUser by remember { mutableStateOf<User?>(null) }
    var isOnline by remember { mutableStateOf(false) }
    var lastSeen by remember { mutableStateOf(0L) }
    
    val authRepository = remember { AuthRepository() }
    val presenceManager = remember { PresenceManager(context) }
    
    // 🎯 Permission launcher for call permissions
    var permissionsGranted by remember { mutableStateOf(false) }
    var pendingCallType by remember { mutableStateOf<com.example.whatappclone.data.model.CallType?>(null) }
    
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        
        if (allGranted) {
            android.util.Log.d("ChatScreen", "Call permissions granted!")
            // Proceed with pending call
            pendingCallType?.let { callType ->
                scope.launch {
                    try {
                        android.util.Log.d("ChatScreen", "Initiating ${callType.name} call to: $otherUserId")
                        val callRepository = com.example.whatappclone.data.repository.ImprovedCallRepository(context)
                        
                        val result = callRepository.initiateCall(
                            receiverId = otherUserId,
                            receiverName = otherUser?.name ?: "Unknown",
                            receiverImage = otherUser?.profileImageUrl ?: "",
                            callType = callType
                        )
                        
                        result.onSuccess { (callId, isReceiverOnline) ->
                            android.util.Log.d("ChatScreen", "Call initiated! CallId: $callId, Receiver online: $isReceiverOnline")
                            // 🎯 Navigate to call screen regardless of receiver online status for testing
                            navController.navigate(com.example.whatappclone.presentation.navigation.Screen.Call.createRoute(callId))
                            
                            if (!isReceiverOnline) {
                                android.util.Log.w("ChatScreen", "Receiver is offline - call will ring until they come online")
                            }
                        }
                        result.onFailure { e ->
                            android.util.Log.e("ChatScreen", "Failed to initiate call", e)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatScreen", "Exception initiating call", e)
                    }
                }
                pendingCallType = null
            }
        } else {
            android.util.Log.e("ChatScreen", "Call permissions DENIED!")
            pendingCallType = null
        }
    }
    
    // Helper function to request permissions and initiate call
    fun initiateCallWithPermissions(callType: com.example.whatappclone.data.model.CallType) {
        val requiredPermissions = if (callType == com.example.whatappclone.data.model.CallType.VIDEO) {
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            )
        } else {
            arrayOf(android.Manifest.permission.RECORD_AUDIO)
        }
        
        // Check if permissions are already granted
        val allPermissionsGranted = requiredPermissions.all { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (allPermissionsGranted) {
            android.util.Log.d("ChatScreen", "Permissions already granted, initiating call")
            // Permissions already granted, initiate call directly
            scope.launch {
                try {
                    val callRepository = com.example.whatappclone.data.repository.ImprovedCallRepository(context)
                    
                    val result = callRepository.initiateCall(
                        receiverId = otherUserId,
                        receiverName = otherUser?.name ?: "Unknown",
                        receiverImage = otherUser?.profileImageUrl ?: "",
                        callType = callType
                    )
                    
                    result.onSuccess { (callId, isReceiverOnline) ->
                        android.util.Log.d("ChatScreen", "Call initiated! CallId: $callId, Receiver online: $isReceiverOnline")
                        // 🎯 Navigate to call screen regardless of receiver online status for testing
                        navController.navigate(com.example.whatappclone.presentation.navigation.Screen.Call.createRoute(callId))
                        
                        if (!isReceiverOnline) {
                            android.util.Log.w("ChatScreen", "Receiver is offline - call will ring until they come online")
                        }
                    }
                    result.onFailure { e ->
                        android.util.Log.e("ChatScreen", "Failed to initiate call", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "Exception initiating call", e)
                }
            }
        } else {
            android.util.Log.d("ChatScreen", "Requesting permissions for ${callType.name} call")
            // Request permissions
            pendingCallType = callType
            callPermissionLauncher.launch(requiredPermissions)
        }
    }
    
    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                chatViewModel.sendMediaMessage(chatId, otherUserId, it, MessageType.IMAGE)
            }
        }
    }
    
    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                chatViewModel.sendMediaMessage(chatId, otherUserId, it, MessageType.VIDEO)
            }
        }
    }
    
    // Document picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                chatViewModel.sendMediaMessage(chatId, otherUserId, it, MessageType.DOCUMENT)
            }
        }
    }
    
    LaunchedEffect(chatId) {
        chatViewModel.loadMessages(chatId)
    }
    
    LaunchedEffect(otherUserId) {
        // Load other user profile
        authRepository.getUserProfile(otherUserId).onSuccess { user ->
            otherUser = user
        }
        
        // Observe presence
        presenceManager.observePresence(otherUserId) { online, lastSeenTime ->
            isOnline = online
            lastSeen = lastSeenTime
        }
    }
    
    // 💎 Glass Background with Ocean gradient
    GradientBackground(gradient = GlassColors.OceanGradient) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = otherUser?.name ?: "Chat",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isOnline) "Online" else "Last seen ${DateTimeUtil.getLastSeenTime(lastSeen)}",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = if (isOnline) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                actions = {
                    // 🎯 Video Call Button
                    IconButton(
                        onClick = { 
                            android.util.Log.d("ChatScreen", "Video call button clicked!")
                            initiateCallWithPermissions(com.example.whatappclone.data.model.CallType.VIDEO)
                        }
                    ) {
                        Icon(Icons.Default.Videocam, "Video Call", tint = Color.White)
                    }
                    
                    // 🎯 Voice Call Button
                    IconButton(
                        onClick = { 
                            android.util.Log.d("ChatScreen", "Voice call button clicked!")
                            initiateCallWithPermissions(com.example.whatappclone.data.model.CallType.AUDIO)
                        }
                    ) {
                        Icon(Icons.Default.Call, "Voice Call", tint = Color.White)
                    }
                    
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (showAttachmentMenu) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    imagePickerLauncher.launch("image/*")
                                    showAttachmentMenu = false
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = PrimaryPurple
                                )
                            ) {
                                Icon(Icons.Default.Image, "Image", tint = Color.White)
                            }
                            Text("Image", fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    videoPickerLauncher.launch("video/*")
                                    showAttachmentMenu = false
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = PrimaryIndigo
                                )
                            ) {
                                Icon(Icons.Default.Videocam, "Video", tint = Color.White)
                            }
                            Text("Video", fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    documentPickerLauncher.launch("*/*")
                                    showAttachmentMenu = false
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = PrimaryBlue
                                )
                            ) {
                                Icon(Icons.Default.Description, "Document", tint = Color.White)
                            }
                            Text("Document", fontSize = 12.sp)
                        }
                    }
                }
                
                // 💎 Glass Message Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1E3A5F).copy(alpha = 0.7f)) // Darker blue glass for visibility
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                            Icon(Icons.Default.AttachFile, "Attach", tint = Color.White)
                        }
                        
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            placeholder = { 
                                Text("Type a message", color = Color.White.copy(alpha = 0.7f)) 
                            },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF2A4A6F).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF2A4A6F).copy(alpha = 0.3f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 💎 Pulsating Send Button
                        PulsatingEffect(durationMillis = 1500) {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() && currentUserId != null) {
                                        scope.launch {
                                            chatViewModel.sendTextMessage(chatId, otherUserId, messageText)
                                            messageText = ""
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            ) {
                                Icon(Icons.Default.Send, "Send", tint = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(
                    message = message,
                    isCurrentUser = message.senderId == currentUserId,
                    onDownload = { url ->
                        scope.launch {
                            DownloadUtil.downloadMedia(context, url, message.type)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: com.example.whatappclone.data.model.Message,
    isCurrentUser: Boolean,
    onDownload: (String) -> Unit
) {
    val context = LocalContext.current
    var showDownloadOption by remember { mutableStateOf(false) }
    
    // 💎 Animation for message appearance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        visible = true
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        ScaleInAnimation(visible = visible, durationMillis = 300) {
            // 💎 Smaller, more visible message bubbles
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isCurrentUser) {
                            // Sent messages - Brighter green with solid background
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00C853), // Bright green
                                    Color(0xFF00E676)  // Lighter green
                                )
                            )
                        } else {
                            // Received messages - Darker blue-gray for better contrast
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF2C3E50), // Dark blue-gray
                                    Color(0xFF34495E)  // Slightly lighter
                                )
                            )
                        }
                    )
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            if (message.type != MessageType.TEXT && !message.mediaUrl.isNullOrEmpty()) {
                                showDownloadOption = true
                            }
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(
                                text = message.text,
                                color = if (isCurrentUser) Color.Black else Color.White,
                                fontSize = 15.sp
                            )
                        }
                        MessageType.IMAGE -> {
                            message.mediaUrl?.let { url ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = "Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (message.text.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.text,
                                        color = if (isCurrentUser) Color.Black else Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                        MessageType.VIDEO -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    "Video",
                                    tint = if (isCurrentUser) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Video",
                                    color = if (isCurrentUser) Color.Black else Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        MessageType.DOCUMENT -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    "Document",
                                    tint = if (isCurrentUser) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.text.ifEmpty { "Document" },
                                    color = if (isCurrentUser) Color.Black else Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = message.text,
                                color = if (isCurrentUser) Color.Black else Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateTimeUtil.getMessageTime(message.timestamp),
                            fontSize = 10.sp,
                            color = if (isCurrentUser) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
                        )
                        
                        if (isCurrentUser) {
                            Icon(
                                imageVector = when (message.status) {
                                    MessageStatus.SENT -> Icons.Default.Check
                                    MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                    MessageStatus.SEEN -> Icons.Default.DoneAll
                                    else -> Icons.Default.Schedule
                                },
                                contentDescription = "Status",
                                tint = if (message.status == MessageStatus.SEEN) {
                                    Color(0xFF00BCD4) // Bright cyan for seen
                                } else {
                                    Color.Black.copy(alpha = 0.7f)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDownloadOption) {
        AlertDialog(
            onDismissRequest = { showDownloadOption = false },
            title = { Text("Download", color = Color.White) },
            text = { Text("Do you want to download this ${message.type.name.lowercase()}?", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        message.mediaUrl?.let { onDownload(it) }
                        showDownloadOption = false
                    }
                ) {
                    Text("Download", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadOption = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1F2E).copy(alpha = 0.95f)
        )
    }
}
