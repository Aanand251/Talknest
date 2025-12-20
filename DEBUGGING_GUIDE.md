# 🔍 Debugging Guide - Call Listener Issue

## Test Kaise Karein:

### 1. **App Kholo aur Logcat dekho**
```bash
adb logcat | grep -E "ChatScreen|CallRepository|CallViewModel|CallService"
```

### 2. **Call Button Dabao**
Video ya Voice call button pe tap karo, fir dekho logcat me:

**Expected Logs:**
```
D/ChatScreen: Video call button clicked!
D/ChatScreen: Initiating video call to: <userId>
D/ImprovedCallRepository: Checking user online status...
D/ImprovedCallRepository: User <userId> is online: true/false
D/ChatScreen: Call initiated! CallId: <callId>, Receiver online: true/false
D/ChatScreen: Navigating to call screen...
```

### 3. **Agar Logs Nahi Aa Rahe:**

Problem ho sakti hai:
- ❌ Button click listener work nahi kar rahi
- ❌ Scope launch nahi ho raha
- ❌ Context null hai

### 4. **Agar "Receiver offline" Dikhe:**

Firebase Realtime Database me user online status set nahi hai:

**Fix:**
1. Firebase Console kholo
2. Realtime Database section me jao
3. Check karo: `users/{userId}/online = true`
4. Agar nahi hai to manually set karo

### 5. **Call Screen Pe Kuch Nahi Dikhe:**

CallViewModel me issue hai:

**Check:**
```
D/CallViewModel: loadCallAsCaller started
D/CallViewModel: Listening for call updates...
D/CallViewModel: Call status: RINGING
```

### 6. **Ringtone Nahi Baj Rahi:**

CallRingtoneManager ka issue:

**Check:**
```
D/CallRingtoneManager: Playing outgoing ringtone
D/CallRingtoneManager: MediaPlayer prepared
D/CallRingtoneManager: Vibration started
```

---

## 🔥 Quick Fixes:

### Fix 1: User Online Status
```kotlin
// MainActivity me already add kar diya:
OnlineStatusManager.setUserOnline()
```

### Fix 2: Call Screen LaunchedEffect
```kotlin
// Already fixed - pehle call load hoti hai, fir check
LaunchedEffect(callId) {
    scope.launch {
        val call = repository.getCall(callId)
        if (call != null) {
            viewModel.loadCallAsCaller(callId, isVideoCall)
        }
    }
}
```

### Fix 3: Incoming Call Listener
```kotlin
// HomeScreen me CallService use ho rahi hai
callRepository.listenForIncomingCalls().collect { call ->
    call?.let {
        CallService.startIncomingCall(context, it)
    }
}
```

---

## 📱 Testing Steps:

1. **App Install Karo** (Already done ✅)
2. **Dono Devices Pe Login Karo**
3. **Logcat Start Karo:**
   ```bash
   adb logcat -s ChatScreen:D ImprovedCallRepository:D CallViewModel:D CallService:D CallRingtoneManager:D
   ```
4. **Video Call Button Dabao**
5. **Dekho Logs Me Kya Aa Raha Hai**

---

## 🎯 Expected Behavior:

### Caller Side:
1. Button click → Log aayega
2. Call initiate → Firebase me call document banega
3. Navigate to CallScreen
4. Ringback tone bajegi (caller ko)
5. Status: RINGING
6. 30s timeout start hoga

### Receiver Side:
1. CallService start hogi
2. Full-screen notification aayegi
3. Ringtone + Vibration
4. Answer/Reject buttons
5. Answer pe → CallScreen khulega
6. Status: ANSWERED → CONNECTED

---

## 🚨 Agar Abhi Bhi Kaam Nahi Kare:

**Logcat output share karo** taaki main exact issue dekh sakoon:
- Kya button click log aa raha hai?
- Kya call initiate log aa raha hai?
- Kya navigation log aa raha hai?
- Koi error message?

---

**Ab test karo aur batao kya logs aa rahe hain!** 🔍
