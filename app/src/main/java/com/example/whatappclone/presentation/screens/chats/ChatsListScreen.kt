package com.example.whatappclone.presentation.screens.chats

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.whatappclone.data.model.Chat
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.ChatRepository
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.presentation.viewmodel.ChatViewModel
import com.example.whatappclone.ui.theme.*
import com.example.whatappclone.util.DateTimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatsListScreen(
    navController: NavController,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chats by chatViewModel.chats.collectAsState()
    val currentUserId = authViewModel.getCurrentUserId()
    var showUserSelectionDialog by remember { mutableStateOf(false) }
    val users by chatViewModel.users.collectAsState()
    val scope = rememberCoroutineScope()
    val chatRepository = remember { ChatRepository(context) }
    
    // 💎 Animation state
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        currentUserId?.let { chatViewModel.loadChats(it) }
        chatViewModel.loadAllUsers()
        delay(100)
        visible = true
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (chats.isEmpty()) {
            // 💎 Glass Empty state
            ScaleInAnimation(visible = visible, durationMillis = 600) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.padding(32.dp),
                        gradient = GlassColors.CrystalGradient
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No chats yet",
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start a conversation",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showUserSelectionDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Text("Start a Chat", color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            // 💎 Glass Chats List with slide-in animation
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(chats) { index, chat ->
                    var itemVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        delay((index * 50).toLong())
                        itemVisible = true
                    }
                    
                    SlideInAnimation(
                        visible = itemVisible,
                        durationMillis = 400
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            onClick = {
                                val otherUserId = chat.participants.find { it != currentUserId } ?: return@GlassCard
                                scope.launch {
                                    val chatId = chatRepository.getChatId(currentUserId ?: "", otherUserId)
                                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId))
                                }
                            },
                            gradient = GlassColors.OceanGradient
                        ) {
                            ChatListItem(
                                chat = chat,
                                currentUserId = currentUserId ?: ""
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Action Button to show user list
        FloatingActionButton(
            onClick = { showUserSelectionDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "New Chat",
                tint = Color.White
            )
        }
    }
    
    // User Selection Dialog
    if (showUserSelectionDialog) {
        UserSelectionDialog(
            users = users,
            onUserSelected = { user ->
                showUserSelectionDialog = false
                scope.launch {
                    val chatId = chatRepository.getChatId(currentUserId ?: "", user.userId)
                    navController.navigate(Screen.Chat.createRoute(chatId, user.userId))
                }
            },
            onDismiss = { showUserSelectionDialog = false }
        )
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    currentUserId: String
) {
    val otherUserId = chat.participants.find { it != currentUserId } ?: ""
    val authRepository = remember { com.example.whatappclone.data.repository.AuthRepository() }
    var otherUser by remember { mutableStateOf<User?>(null) }
    
    LaunchedEffect(otherUserId) {
        val result = authRepository.getUserProfile(otherUserId)
        result.onSuccess { user -> otherUser = user }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💎 Glass Profile Image Container
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (otherUser?.profileImageUrl.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(56.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(otherUser?.profileImageUrl),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = otherUser?.name ?: "Loading...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateTimeUtil.getMessageTime(chat.lastMessageTime),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage.ifEmpty { "No messages yet" },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 💎 Glass Unread count badge
                val unreadCount = chat.unreadCount[currentUserId] ?: 0
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonGreen.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSelectionDialog(
    users: List<User>,
    onUserSelected: (User) -> Unit,
    onDismiss: () -> Unit
) {
    // 💎 Glass Dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Select Contact",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                itemsIndexed(users) { index, user ->
                    var itemVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        delay((index * 30).toLong())
                        itemVisible = true
                    }
                    
                    SlideInAnimation(
                        visible = itemVisible,
                        durationMillis = 300
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { onUserSelected(user) },
                            gradient = GlassColors.CrystalGradient
                        ) {
                            UserListItem(user = user)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF1A1F2E).copy(alpha = 0.95f)
    )
}

@Composable
fun UserListItem(
    user: User
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💎 Glass Profile Container
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (user.profileImageUrl.isEmpty()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(user.profileImageUrl),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = user.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = user.about,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
