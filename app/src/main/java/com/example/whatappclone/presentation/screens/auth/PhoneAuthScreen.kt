package com.example.whatappclone.presentation.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whatappclone.presentation.navigation.Screen
import com.example.whatappclone.presentation.viewmodel.AuthViewModel
import com.example.whatappclone.ui.theme.WhatsAppGreen
import com.example.whatappclone.ui.theme.WhatsAppTeal
import com.example.whatappclone.util.Resource
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+1") }
    var verificationId by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    
    val auth = FirebaseAuth.getInstance()
    
    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                authViewModel.signInWithCredential(credential)
            }
            
            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            override fun onCodeSent(
                verificationIdReceived: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = verificationIdReceived
                showOtpField = true
                isLoading = false
                Toast.makeText(context, "OTP sent successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    LaunchedEffect(authState, userProfile) {
        when (authState) {
            is Resource.Success -> {
                if (userProfile == null) {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PhoneAuth.route) { inclusive = true }
                    }
                }
            }
            is Resource.Error -> {
                isLoading = false
                Toast.makeText(context, (authState as Resource.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    fun sendVerificationCode() {
        if (phoneNumber.length < 10) {
            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        val fullPhoneNumber = countryCode + phoneNumber
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    fun verifyOtp() {
        if (otpCode.length != 6) {
            Toast.makeText(context, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoading = true
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        authViewModel.signInWithCredential(credential)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            Text(
                text = "Enter your phone number",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppTeal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "WhatsApp will send an SMS message to verify your phone number.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Country Code and Phone Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { countryCode = it },
                    modifier = Modifier.width(80.dp),
                    label = { Text("Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !showOtpField
                )
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !showOtpField
                )
            }
            
            if (showOtpField) {
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isLoading
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (showOtpField) {
                        verifyOtp()
                    } else {
                        sendVerificationCode()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = if (showOtpField) "VERIFY OTP" else "SEND CODE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (showOtpField) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    showOtpField = false
                    otpCode = ""
                }) {
                    Text("Change Phone Number", color = WhatsAppGreen)
                }
            }
        }
    }
}
