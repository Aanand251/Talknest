package com.example.whatappclone.presentation.screens.chats

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.whatappclone.util.DateTimeUtil
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
    
    LaunchedEffect(Unit) {
        currentUserId?.let { chatViewModel.loadChats(it) }
        chatViewModel.loadAllUsers()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (chats.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No chats yet",
                        fontSize = 20.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showUserSelectionDialog = true }) {
                        Text("Start a Chat")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(chats) { chat ->
                    ChatListItem(
                        chat = chat,
                        currentUserId = currentUserId ?: "",
                        onClick = {
                            val otherUserId = chat.participants.find { it != currentUserId } ?: return@ChatListItem
                            scope.launch {
                                val chatId = chatRepository.getChatId(currentUserId ?: "", otherUserId)
                                navController.navigate(Screen.Chat.createRoute(chatId, otherUserId))
                            }
                        }
                    )
                    HorizontalDivider()
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
    currentUserId: String,
    onClick: () -> Unit
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
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        if (otherUser?.profileImageUrl.isNullOrEmpty()) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                tint = Color.Gray
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateTimeUtil.getMessageTime(chat.lastMessageTime),
                    fontSize = 12.sp,
                    color = Color.Gray
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
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Unread count badge
                val unreadCount = chat.unreadCount[currentUserId] ?: 0
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Contact") },
        text = {
            LazyColumn {
                items(users) { user ->
                    UserListItem(
                        user = user,
                        onClick = { onUserSelected(user) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UserListItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.profileImageUrl.isEmpty()) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                tint = Color.Gray
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
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = user.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = user.about,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
