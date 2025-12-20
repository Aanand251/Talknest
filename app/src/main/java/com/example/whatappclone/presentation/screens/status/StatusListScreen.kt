package com.example.whatappclone.presentation.screens.status

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.whatappclone.data.model.Status
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.presentation.viewmodel.StatusViewModel
import com.example.whatappclone.ui.theme.WhatsAppGreen
import com.example.whatappclone.util.DateTimeUtil

@Composable
fun StatusListScreen(
    statusViewModel: StatusViewModel,
    authViewModel: AuthViewModel
) {
    val statuses by statusViewModel.statuses.collectAsState()
    val currentUserId = authViewModel.getCurrentUserId()
    
    // Group statuses by user
    val groupedStatuses = statuses.groupBy { it.userId }
    
    LaunchedEffect(Unit) {
        statusViewModel.loadStatuses()
    }
    
    if (statuses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No status updates",
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // My Status (if exists)
            val myStatuses = groupedStatuses[currentUserId]
            if (!myStatuses.isNullOrEmpty()) {
                item {
                    Text(
                        text = "My status",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    StatusItem(status = myStatuses.first(), isMyStatus = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            // Recent updates
            val recentStatuses = groupedStatuses.filter { it.key != currentUserId }
            if (recentStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent updates",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(recentStatuses.entries.toList()) { (_, userStatuses) ->
                    StatusItem(status = userStatuses.first(), isMyStatus = false)
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    status: Status,
    isMyStatus: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image with border
        Box {
            if (status.userProfileImage.isEmpty()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, WhatsAppGreen, CircleShape),
                    tint = Color.Gray
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(status.userProfileImage),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, WhatsAppGreen, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMyStatus) "My status" else status.userName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateTimeUtil.getTimeAgo(status.timestamp),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
