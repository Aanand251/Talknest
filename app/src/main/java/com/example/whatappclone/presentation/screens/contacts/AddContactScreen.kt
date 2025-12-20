package com.example.whatappclone.presentation.screens.contacts

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.whatappclone.data.model.ContactRequest
import com.example.whatappclone.data.model.RequestStatus
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.ContactRepository
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val currentUser by authViewModel.userProfile.collectAsState()
    val contactRepository = remember { ContactRepository() }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<User?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Contact", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(16.dp)
        ) {
            // Search Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            searchResult = null
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter email address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, "Email", tint = PrimaryPurple)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                if (email.isBlank()) {
                                    errorMessage = "Please enter an email"
                                    return@launch
                                }
                                
                                isSearching = true
                                errorMessage = null
                                successMessage = null
                                
                                contactRepository.searchUserByEmail(email).fold(
                                    onSuccess = { user ->
                                        isSearching = false
                                        if (user != null) {
                                            searchResult = user
                                        } else {
                                            showInviteDialog = true
                                        }
                                    },
                                    onFailure = {
                                        isSearching = false
                                        errorMessage = "Search failed"
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .width(80.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, "Search", tint = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error/Success Messages
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, "Error", tint = ErrorColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = ErrorColor)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            successMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentEmerald.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, "Success", tint = AccentEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = AccentEmerald)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Search Result
            searchResult?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(PrimaryPurple, PrimaryBlue)
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            AsyncImage(
                                model = user.profileImageUrl.ifEmpty { "https://via.placeholder.com/150" },
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = user.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        
                        Text(
                            text = user.email,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = user.about,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Send Request Button
                        Button(
                            onClick = {
                                currentUser?.let { sender ->
                                    scope.launch {
                                        contactRepository.sendFriendRequest(
                                            currentUser = sender,
                                            receiverEmail = user.email,
                                            receiverId = user.userId
                                        ).fold(
                                            onSuccess = {
                                                successMessage = "Friend request sent!"
                                                searchResult = null
                                                email = ""
                                            },
                                            onFailure = {
                                                errorMessage = "Failed to send request"
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(PrimaryPurple, PrimaryBlue)
                                ),
                                width = 2.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PersonAdd, "Send", tint = PrimaryPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Friend Request", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Invite Dialog for unregistered users
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("User Not Found", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This email is not registered on TalkNest.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Would you like to send an invitation?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.let { user ->
                            val inviteLink = contactRepository.generateInvitationLink(
                                user.name, user.email
                            )
                            val shareText = contactRepository.shareInvitation(inviteLink)
                            
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                        }
                        showInviteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple
                    )
                ) {
                    Text("Send Invitation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ContactRequestsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.userProfile.collectAsState()
    val contactRepository = remember { ContactRepository() }
    val scope = rememberCoroutineScope()
    
    val incomingRequests by contactRepository
        .observeIncomingRequests(currentUser?.userId ?: "")
        .collectAsState(initial = emptyList())
    
    val sentRequests by contactRepository
        .observeSentRequests(currentUser?.userId ?: "")
        .collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Requests", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryPurple
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Received (${incomingRequests.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Sent (${sentRequests.filter { it.status == RequestStatus.PENDING }.size})") }
                )
            }
            
            when (selectedTab) {
                0 -> IncomingRequestsList(
                    requests = incomingRequests,
                    onAccept = { requestId ->
                        scope.launch {
                            contactRepository.acceptFriendRequest(requestId)
                        }
                    },
                    onReject = { requestId ->
                        scope.launch {
                            contactRepository.rejectFriendRequest(requestId)
                        }
                    }
                )
                1 -> SentRequestsList(
                    requests = sentRequests,
                    onCancel = { requestId ->
                        scope.launch {
                            contactRepository.cancelFriendRequest(requestId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IncomingRequestsList(
    requests: List<ContactRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No pending requests", color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                ContactRequestCard(
                    request = request,
                    onAccept = { onAccept(request.requestId) },
                    onReject = { onReject(request.requestId) }
                )
            }
        }
    }
}

@Composable
fun SentRequestsList(
    requests: List<ContactRequest>,
    onCancel: (String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No sent requests", color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                SentRequestCard(
                    request = request,
                    onCancel = { onCancel(request.requestId) }
                )
            }
        }
    }
}

@Composable
fun ContactRequestCard(
    request: ContactRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = request.senderProfileImage.ifEmpty { "https://via.placeholder.com/150" },
                contentDescription = "Profile",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryPurple, CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = request.senderEmail,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                if (request.message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.message,
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentEmerald, CircleShape)
                ) {
                    Icon(Icons.Default.Check, "Accept", tint = Color.White)
                }
                
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(40.dp)
                        .background(ErrorColor, CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Reject", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun SentRequestCard(
    request: ContactRequest,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (request.status) {
                RequestStatus.PENDING -> Color.White
                RequestStatus.ACCEPTED -> AccentEmerald.copy(alpha = 0.1f)
                RequestStatus.REJECTED -> ErrorColor.copy(alpha = 0.1f)
                RequestStatus.CANCELLED -> Color.LightGray.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.receiverEmail,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (request.status) {
                        RequestStatus.PENDING -> "Pending"
                        RequestStatus.ACCEPTED -> "✓ Accepted"
                        RequestStatus.REJECTED -> "✗ Rejected"
                        RequestStatus.CANCELLED -> "Cancelled"
                    },
                    fontSize = 14.sp,
                    color = when (request.status) {
                        RequestStatus.PENDING -> PrimaryPurple
                        RequestStatus.ACCEPTED -> AccentEmerald
                        RequestStatus.REJECTED -> ErrorColor
                        RequestStatus.CANCELLED -> TextSecondary
                    }
                )
            }
            
            if (request.status == RequestStatus.PENDING) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = ErrorColor)
                }
            }
        }
    }
}
