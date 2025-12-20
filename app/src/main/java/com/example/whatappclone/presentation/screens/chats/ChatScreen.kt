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
    
    Scaffold(
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
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    val contextForCall = LocalContext.current
                    
                    IconButton(
                        onClick = { 
                            try {
                                android.util.Log.d("ChatScreen", "Video call button clicked!")
                                android.util.Log.d("ChatScreen", "Other user ID: $otherUserId")
                                android.util.Log.d("ChatScreen", "Context: $contextForCall")
                                
                                scope.launch {
                                    try {
                                        android.util.Log.d("ChatScreen", "Inside coroutine - Initiating video call to: $otherUserId")
                                        val callRepository = com.example.whatappclone.data.repository.ImprovedCallRepository(contextForCall)
                                        android.util.Log.d("ChatScreen", "CallRepository created")
                                        
                                        val result = callRepository.initiateCall(
                                            receiverId = otherUserId,
                                            receiverName = otherUser?.name ?: "Unknown",
                                            receiverImage = otherUser?.profileImageUrl ?: "",
                                            callType = com.example.whatappclone.data.model.CallType.VIDEO
                                        )
                                        
                                        android.util.Log.d("ChatScreen", "initiateCall returned")
                                        
                                        result.onSuccess { (callId, isReceiverOnline) ->
                                            android.util.Log.d("ChatScreen", "SUCCESS! CallId: $callId, Receiver online: $isReceiverOnline")
                                            if (!isReceiverOnline) {
                                                android.util.Log.d("ChatScreen", "Receiver is offline!")
                                            } else {
                                                android.util.Log.d("ChatScreen", "Navigating to call screen...")
                                                navController.navigate(com.example.whatappclone.presentation.navigation.Screen.Call.createRoute(callId))
                                            }
                                        }
                                        result.onFailure { e ->
                                            android.util.Log.e("ChatScreen", "FAILED to initiate call", e)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ChatScreen", "Exception in coroutine", e)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatScreen", "Exception in onClick", e)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Videocam, "Video Call", tint = Color.White)
                    }
                    IconButton(
                        onClick = { 
                            try {
                                android.util.Log.d("ChatScreen", "Voice call button clicked!")
                                android.util.Log.d("ChatScreen", "Other user ID: $otherUserId")
                                
                                scope.launch {
                                    try {
                                        android.util.Log.d("ChatScreen", "Inside coroutine - Initiating voice call to: $otherUserId")
                                        val callRepository = com.example.whatappclone.data.repository.ImprovedCallRepository(contextForCall)
                                        android.util.Log.d("ChatScreen", "CallRepository created")
                                        
                                        val result = callRepository.initiateCall(
                                            receiverId = otherUserId,
                                            receiverName = otherUser?.name ?: "Unknown",
                                            receiverImage = otherUser?.profileImageUrl ?: "",
                                            callType = com.example.whatappclone.data.model.CallType.AUDIO
                                        )
                                        
                                        android.util.Log.d("ChatScreen", "initiateCall returned")
                                        
                                        result.onSuccess { (callId, isReceiverOnline) ->
                                            android.util.Log.d("ChatScreen", "SUCCESS! CallId: $callId, Receiver online: $isReceiverOnline")
                                            if (!isReceiverOnline) {
                                                android.util.Log.d("ChatScreen", "Receiver is offline!")
                                            } else {
                                                android.util.Log.d("ChatScreen", "Navigating to call screen...")
                                                navController.navigate(com.example.whatappclone.presentation.navigation.Screen.Call.createRoute(callId))
                                            }
                                        }
                                        result.onFailure { e ->
                                            android.util.Log.e("ChatScreen", "FAILED to initiate call", e)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ChatScreen", "Exception in coroutine", e)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatScreen", "Exception in onClick", e)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Call, "Voice Call", tint = Color.White)
                    }
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryPurple, PrimaryIndigo, PrimaryBlue)
                    )
                )
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
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                        Icon(Icons.Default.AttachFile, "Attach", tint = PrimaryPurple)
                    }
                    
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message") }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUserId != null) {
                                scope.launch {
                                    chatViewModel.sendTextMessage(chatId, otherUserId, messageText)
                                    messageText = ""
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = PrimaryPurple)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: com.example.whatappclone.data.model.Message,
    isCurrentUser: Boolean,
    onDownload: (String) -> Unit
) {
    val context = LocalContext.current
    var showDownloadOption by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isCurrentUser) PrimaryPurple else Color.LightGray,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        if (message.type != MessageType.TEXT && !message.mediaUrl.isNullOrEmpty()) {
                            showDownloadOption = true
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            color = if (isCurrentUser) Color.White else Color.Black
                        )
                    }
                    MessageType.IMAGE -> {
                        message.mediaUrl?.let { url ->
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = "Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (message.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = if (isCurrentUser) Color.White else Color.Black
                                )
                            }
                        }
                    }
                    MessageType.VIDEO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayCircle,
                                "Video",
                                tint = if (isCurrentUser) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Video",
                                color = if (isCurrentUser) Color.White else Color.Black
                            )
                        }
                    }
                    MessageType.DOCUMENT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                "Document",
                                tint = if (isCurrentUser) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.text.ifEmpty { "Document" },
                                color = if (isCurrentUser) Color.White else Color.Black
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.text,
                            color = if (isCurrentUser) Color.White else Color.Black
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
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray
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
                                Color.Cyan
                            } else {
                                Color.White.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    if (showDownloadOption) {
        AlertDialog(
            onDismissRequest = { showDownloadOption = false },
            title = { Text("Download") },
            text = { Text("Do you want to download this ${message.type.name.lowercase()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        message.mediaUrl?.let { onDownload(it) }
                        showDownloadOption = false
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadOption = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
