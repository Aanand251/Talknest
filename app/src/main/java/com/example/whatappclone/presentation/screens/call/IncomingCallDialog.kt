package com.example.whatappclone.presentation.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.whatappclone.data.model.Call
import com.example.whatappclone.data.model.CallType
import com.example.whatappclone.ui.theme.*

@Composable
fun IncomingCallDialog(
    call: Call,
    onAnswer: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Prevent dismiss on outside click */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.95f),
                            PrimaryViolet.copy(alpha = 0.95f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // Caller avatar
                AsyncImage(
                    model = call.callerImage.ifEmpty { 
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=${call.callerName}"
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
                    text = call.callerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Call type
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (call.callType == CallType.VIDEO) 
                            Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (call.callType == CallType.VIDEO) 
                            "Incoming Video Call..." else "Incoming Voice Call...",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Answer and Reject buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = onReject,
                            modifier = Modifier.size(72.dp),
                            containerColor = Color.Red,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Reject",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reject",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    
                    // Answer button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = onAnswer,
                            modifier = Modifier.size(72.dp),
                            containerColor = WhatsAppGreen,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (call.callType == CallType.VIDEO)
                                    Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Answer",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Answer",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
