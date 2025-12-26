package com.example.whatappclone.presentation.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.SettingsRepository
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.userProfile.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // 💎 Glass Background with Sunset gradient
    GradientBackground(gradient = GlassColors.SunsetGradient) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Settings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 💎 Glass Profile Section
                currentUser?.let { user ->
                    ScaleInAnimation(visible = true, durationMillis = 600) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            gradient = GlassColors.CrystalGradient
                        ) {
                            ProfileSection(
                                user = user,
                                onEditClick = { showEditDialog = true }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Settings Categories
                SettingsSection(
                    title = "Account",
                    items = listOf(
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Edit Profile",
                        subtitle = "Change your name and about",
                        onClick = { showEditDialog = true }
                    ),
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "Email",
                        subtitle = currentUser?.email ?: "Not set",
                        onClick = { }
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            var showLastSeenDialog by remember { mutableStateOf(false) }
            var showReadReceiptsDialog by remember { mutableStateOf(false) }
            var showNotificationsDialog by remember { mutableStateOf(false) }
            var showThemeDialog by remember { mutableStateOf(false) }
            var showProfilePicturePicker by remember { mutableStateOf(false) }
            
            // Initialize with user's current settings
            var lastSeenPref by remember(currentUser) { mutableStateOf(currentUser?.lastSeenPrivacy ?: "Everyone") }
            var readReceiptsEnabled by remember(currentUser) { mutableStateOf(currentUser?.readReceiptsEnabled ?: true) }
            var notificationsEnabled by remember(currentUser) { mutableStateOf(currentUser?.popupNotificationsEnabled ?: true) }
            var currentTheme by remember { mutableStateOf("Modern Gradient") }
            
            // Initialize SettingsRepository
            val settingsRepository = remember { com.example.whatappclone.data.repository.SettingsRepository() }
            val scope = rememberCoroutineScope()
            
            // Profile Picture Picker
            val profilePictureLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    // Upload profile picture
                    authViewModel.uploadProfilePicture(it)
                }
            }
            
            SettingsSection(
                title = "Privacy & Security",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Encryption",
                        subtitle = "End-to-end encrypted",
                        onClick = { }
                    ),
                    SettingsItem(
                        icon = Icons.Default.Visibility,
                        title = "Last Seen",
                        subtitle = lastSeenPref,
                        onClick = { showLastSeenDialog = true }
                    ),
                    SettingsItem(
                        icon = Icons.Default.Check,
                        title = "Read Receipts",
                        subtitle = if (readReceiptsEnabled) "Enabled" else "Disabled",
                        onClick = { showReadReceiptsDialog = true }
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsSection(
                title = "Notifications",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Message Notifications",
                        subtitle = if (notificationsEnabled) "Enabled" else "Disabled",
                        onClick = { showNotificationsDialog = true }
                    ),
                    SettingsItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Notification Sound",
                        subtitle = "Default",
                        onClick = { }
                    )
                )
            )
            
            // Last Seen Dialog
            if (showLastSeenDialog) {
                AlertDialog(
                    onDismissRequest = { showLastSeenDialog = false },
                    title = { Text("Last Seen") },
                    text = {
                        Column {
                            listOf("Everyone", "My Contacts", "Nobody").forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            lastSeenPref = option
                                            showLastSeenDialog = false
                                            // Save to Firestore
                                            currentUser?.userId?.let { userId ->
                                                scope.launch(Dispatchers.IO) {
                                                    settingsRepository.updateLastSeenPrivacy(userId, option)
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = lastSeenPref == option,
                                        onClick = {
                                            lastSeenPref = option
                                            showLastSeenDialog = false
                                            // Save to Firestore
                                            currentUser?.userId?.let { userId ->
                                                scope.launch(Dispatchers.IO) {
                                                    settingsRepository.updateLastSeenPrivacy(userId, option)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(option)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLastSeenDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
            
            // Read Receipts Dialog
            if (showReadReceiptsDialog) {
                AlertDialog(
                    onDismissRequest = { showReadReceiptsDialog = false },
                    title = { Text("Read Receipts") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable read receipts")
                                Switch(
                                    checked = readReceiptsEnabled,
                                    onCheckedChange = { readReceiptsEnabled = it }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "If disabled, you won't see read receipts from others",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showReadReceiptsDialog = false
                            // Save to Firestore
                            currentUser?.userId?.let { userId ->
                                scope.launch(Dispatchers.IO) {
                                    settingsRepository.updateReadReceipts(userId, readReceiptsEnabled)
                                }
                            }
                        }) {
                            Text("Save")
                        }
                    }
                )
            }
            
            // Notifications Dialog
            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = { Text("Notifications") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable notifications")
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { notificationsEnabled = it }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showNotificationsDialog = false }) {
                            Text("Save")
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsSection(
                title = "Profile",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Change Profile Picture",
                        subtitle = "Upload new photo",
                        onClick = { profilePictureLauncher.launch("image/*") }
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsSection(
                title = "App Settings",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = currentTheme,
                        onClick = { showThemeDialog = true }
                    ),
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Storage",
                        subtitle = "Manage app storage",
                        onClick = { }
                    )
                )
            )
            
            // Theme Dialog
            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text("Choose Theme") },
                    text = {
                        Column {
                            listOf(
                                "Modern Gradient",
                                "Dark Mode",
                                "Light Mode",
                                "Ocean Blue",
                                "Forest Green"
                            ).forEach { theme ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentTheme = theme
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentTheme == theme,
                                        onClick = {
                                            currentTheme = theme
                                            showThemeDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(theme)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(SecondaryPink, SecondaryOrange)
                    ),
                    width = 2.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = SecondaryPink
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Logout",
                        color = SecondaryPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Version
            Text(
                text = "TalkNest v1.0.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            }
        }
    }
    
    // Edit Profile Dialog
    if (showEditDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, about ->
                // TODO: Update profile
                showEditDialog = false
            }
        )
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = Color.White) },
            text = { Text("Are you sure you want to logout?", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.signOut()
                        showLogoutDialog = false
                        navController.navigate(Screen.EmailAuth.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = SecondaryPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1F2E).copy(alpha = 0.95f)
        )
    }
    
    // Edit Profile Dialog
    if (showEditDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, about ->
                // TODO: Update profile
                showEditDialog = false
            }
        )
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = Color.White) },
            text = { Text("Are you sure you want to logout?", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.signOut()
                        showLogoutDialog = false
                        navController.navigate(Screen.EmailAuth.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = SecondaryPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1F2E).copy(alpha = 0.95f)
        )
    }
}

@Composable
fun ProfileSection(user: User, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💎 Glass Profile Picture with pulsating effect
        PulsatingEffect(durationMillis = 2000) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(
                        width = 3.dp,
                        color = NeonGreen,
                        shape = CircleShape
                    )
            ) {
                AsyncImage(
                    model = user.profileImageUrl.ifEmpty { "https://via.placeholder.com/150" },
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // User Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.about,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        // 💎 Glass Edit Button
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.8f))
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = Color.White
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, items: List<SettingsItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )
        
        // 💎 Glass Card for settings items
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            gradient = GlassColors.OceanGradient
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItemRow(item)
                    if (index < items.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💎 Glass icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = item.subtitle,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var about by remember { mutableStateOf(user.about) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Edit Profile",
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it },
                    label = { Text("About") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, about) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)
