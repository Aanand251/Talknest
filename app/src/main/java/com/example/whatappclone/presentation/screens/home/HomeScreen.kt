package com.example.whatappclone.presentation.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.screens.chats.ChatsListScreen
import com.example.whatappclone.presentation.screens.status.StatusListScreen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.presentation.viewmodel.ChatViewModel
import com.example.whatappclone.presentation.viewmodel.StatusViewModel
import com.example.whatappclone.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    statusViewModel: StatusViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var showStatusOptions by remember { mutableStateOf(false) }
    
    // Image/Video picker for status
    val statusImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            statusViewModel.createStatus(it, false, "")
        }
    }
    
    val statusVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            statusViewModel.createStatus(it, true, "")
        }
    }
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        // Load user data
        val userId = authViewModel.getCurrentUserId()
        userId?.let {
            authViewModel.loadUserProfile(it)
            chatViewModel.loadChats(it)
            statusViewModel.loadStatuses()
        }
        
        // Listen for incoming calls and start CallService
        val callRepository = com.example.whatappclone.data.repository.ImprovedCallRepository(context)
        callRepository.listenForIncomingCalls().collect { call ->
            call?.let {
                // Start CallService to show notification with ringtone
                com.example.whatappclone.service.CallService.startIncomingCall(context, it)
            }
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search chats...", color = Color.White.copy(0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    } else {
                        Text(
                            "TalkNest",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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
                ),
                actions = {
                    IconButton(onClick = { 
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("My QR Code") },
                                onClick = {
                                    navController.navigate(Screen.ProfileQR.route)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.QrCode, "QR Code") }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan QR") },
                                onClick = {
                                    navController.navigate(Screen.QRScanner.route)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, "Scan") }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Contact") },
                                onClick = {
                                    navController.navigate(Screen.AddContact.route)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, "Add Contact") }
                            )
                            DropdownMenuItem(
                                text = { Text("Friend Requests") },
                                onClick = {
                                    navController.navigate(Screen.ContactRequests.route)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.People, "Requests") }
                            )
                            DropdownMenuItem(
                                text = { Text("My Friends") },
                                onClick = {
                                    navController.navigate(Screen.FriendsList.route)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Group, "Friends") }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    navController.navigate(Screen.Settings.route)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, "Settings") }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate based on current tab
                    when (pagerState.currentPage) {
                        0 -> {
                            // Navigate to user selection for new chat
                            chatViewModel.loadAllUsers()
                            // Show user selection dialog or screen
                        }
                        1 -> {
                            // Show status posting options
                            showStatusOptions = true
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = if (pagerState.currentPage == 0) Icons.AutoMirrored.Filled.Message else Icons.Default.CameraAlt,
                    contentDescription = "Add",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = WhatsAppTeal,
                contentColor = Color.White
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = { Text("CHATS") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = { Text("STATUS") }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(2) }
                    },
                    text = { Text("CALLS") }
                )
            }
            
            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ChatsListScreen(
                        navController = navController,
                        chatViewModel = chatViewModel,
                        authViewModel = authViewModel
                    )
                    1 -> StatusListScreen(
                        statusViewModel = statusViewModel,
                        authViewModel = authViewModel
                    )
                    2 -> CallsListScreen()
                }
            }
        }
        
        // Status posting dialog
        if (showStatusOptions) {
            AlertDialog(
                onDismissRequest = { showStatusOptions = false },
                title = { Text("Post Status") },
                text = {
                    Column {
                        Text("Choose media type:")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                statusImagePickerLauncher.launch("image/*")
                                showStatusOptions = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, "Image")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Photo")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                statusVideoPickerLauncher.launch("video/*")
                                showStatusOptions = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Videocam, "Video")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatusOptions = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CallsListScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Calls feature coming soon!")
    }
}
