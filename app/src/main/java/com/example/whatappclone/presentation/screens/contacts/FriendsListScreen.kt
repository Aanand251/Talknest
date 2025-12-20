package com.example.whatappclone.presentation.screens.contacts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.ContactRepository
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUserId = authViewModel.getCurrentUserId()
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val contactRepository = remember { ContactRepository() }
    
    LaunchedEffect(currentUserId) {
        currentUserId?.let { userId ->
            scope.launch {
                val result = contactRepository.getContacts(userId)
                result.onSuccess { contactList ->
                    friends = contactList
                    isLoading = false
                }.onFailure {
                    isLoading = false
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Friends",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color.White
                        )
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                friends.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No friends yet",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Add friends to start chatting",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(friends) { friend ->
                            FriendItem(
                                friend = friend,
                                currentUserId = currentUserId,
                                onClick = { chatId ->
                                    navController.navigate(Screen.Chat.createRoute(chatId, friend.userId))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: User,
    currentUserId: String?,
    onClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chatRepository = remember { com.example.whatappclone.data.repository.ChatRepository(context) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (currentUserId != null) {
                    val chatId = chatRepository.getChatId(currentUserId, friend.userId)
                    onClick(chatId)
                }
            },
        color = Color.White,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (friend.profileImageUrl.isNullOrEmpty()) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    tint = Color.Gray
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(friend.profileImageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!friend.email.isNullOrEmpty()) {
                    Text(
                        text = friend.email,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else if (!friend.phoneNumber.isNullOrEmpty()) {
                    Text(
                        text = friend.phoneNumber,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
