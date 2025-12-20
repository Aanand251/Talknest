package com.example.whatappclone.presentation.screens.qr

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.whatappclone.data.repository.ContactRepository
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.*
import com.example.whatappclone.util.QRCodeHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProfile by authViewModel.userProfile.collectAsState()
    val contactRepository = remember { ContactRepository() }
    
    var isScanning by remember { mutableStateOf(true) }
    var scannedData by remember { mutableStateOf<String?>(null) }
    var isSendingRequest by remember { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Scan QR Code",
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                if (isScanning) {
                    // QR Scanner View
                    AndroidView(
                        factory = { context ->
                            CompoundBarcodeView(context).apply {
                                decodeContinuous(object : BarcodeCallback {
                                    override fun barcodeResult(result: BarcodeResult?) {
                                        result?.text?.let { qrData ->
                                            scannedData = qrData
                                            isScanning = false
                                            pause()
                                        }
                                    }
                                })
                                resume()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Scanning Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = "Point camera at QR code",
                                modifier = Modifier.padding(16.dp),
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                    }
                } else {
                    // Scanned Result
                    scannedData?.let { qrData ->
                        val parsedData = QRCodeHelper.parseQRCode(qrData)
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundLight)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (parsedData != null) {
                                val (userId, email, name) = parsedData
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        Text(
                                            text = "✓",
                                            fontSize = 60.sp,
                                            color = AccentEmerald
                                        )
                                        
                                        Text(
                                            text = "QR Code Scanned!",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                        
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                        
                                        Text(
                                            text = name,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        Text(
                                            text = email,
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Send Request Button
                                        Button(
                                            onClick = {
                                                userProfile?.let { user ->
                                                    scope.launch {
                                                        isSendingRequest = true
                                                        val result = contactRepository.sendFriendRequest(
                                                            currentUser = user,
                                                            receiverEmail = email,
                                                            receiverId = userId,
                                                            message = "Hi! I scanned your QR code."
                                                        )
                                                        isSendingRequest = false
                                                        
                                                        if (result.isSuccess) {
                                                            Toast.makeText(
                                                                context,
                                                                "Friend request sent!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            navController.popBackStack()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Failed: ${result.exceptionOrNull()?.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isSendingRequest,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent
                                            ),
                                            contentPadding = PaddingValues()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(
                                                                PrimaryPurple,
                                                                PrimaryBlue
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSendingRequest) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color.White
                                                    )
                                                } else {
                                                    Text(
                                                        "Send Friend Request",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Scan Again Button
                                        TextButton(
                                            onClick = {
                                                scannedData = null
                                                isScanning = true
                                            }
                                        ) {
                                            Text(
                                                "Scan Another QR Code",
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Invalid QR Code
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "❌",
                                            fontSize = 60.sp
                                        )
                                        
                                        Text(
                                            text = "Invalid QR Code",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryPink
                                        )
                                        
                                        Text(
                                            text = "This is not a TalkNest profile QR code",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        Button(
                                            onClick = {
                                                scannedData = null
                                                isScanning = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PrimaryPurple
                                            )
                                        ) {
                                            Text("Try Again")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Permission Denied
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📷",
                        fontSize = 60.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Camera Permission Required",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Please grant camera permission to scan QR codes",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple
                        )
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

