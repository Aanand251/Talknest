# WhatsApp Clone - Android App

A fully-featured WhatsApp clone built with **Kotlin**, **Jetpack Compose**, and **Firebase**.

## 🚀 Features

### ✅ Implemented
- ✅ **Phone Authentication** - OTP-based Firebase phone authentication
- ✅ **User Profile Management** - Create and view user profiles with photos
- ✅ **Real-time Chat System** - 1-to-1 messaging with Firestore
- ✅ **Chat List** - View all conversations with last message preview
- ✅ **Status/Stories** - 24-hour status updates with images/videos
- ✅ **WhatsApp Theme** - Authentic WhatsApp UI with Material3
- ✅ **Dark Mode Support** - Complete dark theme implementation
- ✅ **Offline Storage** - Room database for local caching

### 🚧 To Be Completed
- 🔲 **Chat Screen UI** - Message bubbles, send/receive messages
- 🔲 **Media Sharing** - Images, videos, audio, documents
- 🔲 **Voice Notes** - Recording and playback
- 🔲 **Message Features** - Read receipts, typing indicator, online status
- 🔲 **Message Actions** - Delete, copy, forward messages
- 🔲 **Push Notifications** - FCM integration
- 🔲 **Group Chats** - Multi-user conversations
- 🔲 **Voice/Video Calls** - WebRTC integration (optional)

## 🏗️ Architecture

- **MVVM Pattern** - ViewModel + Repository
- **Clean Architecture** - Separation of concerns
- **Jetpack Compose** - Modern declarative UI
- **Firebase Backend** - Auth, Firestore, Storage, FCM
- **Room Database** - Local data persistence
- **Kotlin Coroutines** - Asynchronous operations
- **StateFlow** - Reactive state management

## 📦 Tech Stack

### Android
- Kotlin
- Jetpack Compose
- Material3 Design
- Navigation Component
- Room Database
- DataStore Preferences
- WorkManager

### Firebase
- Firebase Authentication (Phone Auth)
- Cloud Firestore (Database)
- Firebase Storage (Media files)
- Firebase Cloud Messaging (Notifications)
- Firebase Analytics

### Libraries
- **Coil** - Image loading
- **ExoPlayer** - Media playback
- **Accompanist** - Permissions, Pager, SystemUI
- **Gson** - JSON serialization

## 🛠️ Setup Instructions

### Prerequisites
1. **Android Studio** - Latest version (Hedgehog or newer)
2. **Firebase Project** - Create at [Firebase Console](https://console.firebase.google.com/)
3. **Minimum SDK** - API 24 (Android 7.0)
4. **Target SDK** - API 35 (Android 15)

### Firebase Configuration

#### Step 1: Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project" and follow the wizard
3. Enable Google Analytics (optional)

#### Step 2: Register Android App
1. In Firebase project, click "Add App" → Android
2. Enter package name: `com.example.whatappclone`
3. Download `google-services.json`
4. Place it in `app/` directory (replace existing if any)

#### Step 3: Enable Firebase Services

**Authentication:**
1. Go to Authentication → Sign-in method
2. Enable "Phone" provider
3. Add your test phone numbers if needed

**Firestore Database:**
1. Go to Firestore Database → Create database
2. Start in **test mode** (for development)
3. Choose a location (nearest to you)

**Storage:**
1. Go to Storage → Get started
2. Start in **test mode** (for development)

**Cloud Messaging:**
1. Go to Cloud Messaging
2. Note down the Server Key (for later)

#### Step 4: Security Rules (Important!)

**Firestore Rules:**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Chats collection
    match /chats/{chatId} {
      allow read, write: if request.auth != null;
      
      match /messages/{messageId} {
        allow read, write: if request.auth != null;
      }
    }
    
    // Status collection
    match /status/{statusId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

**Storage Rules:**
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Project Setup

1. **Clone/Open Project**
   ```bash
   # Open the project in Android Studio
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync
   - Wait for dependencies to download

3. **Add google-services.json**
   - Ensure `google-services.json` is in `app/` directory
   - File must match your Firebase project

4. **Build Project**
   ```
   Build → Make Project
   ```

5. **Run on Device/Emulator**
   - Connect Android device or start emulator
   - Click Run ▶️

## 📱 Testing

### Phone Authentication Testing
Firebase allows test phone numbers without SMS:

1. Go to Firebase Console → Authentication → Sign-in method
2. Click Phone → Phone numbers for testing
3. Add test numbers:
   - Phone: `+1 555-555-5555`
   - Code: `123456`

### Test User Flow
1. Launch app → Enter test phone number
2. Enter OTP code `123456`
3. Setup profile (name, photo, about)
4. Access home screen with Chats/Status/Calls tabs

## 📂 Project Structure

```
app/src/main/java/com/example/whatappclone/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entity/       # Room entities
│   │   └── AppDatabase.kt
│   ├── model/            # Data models
│   │   ├── User.kt
│   │   ├── Message.kt
│   │   ├── Chat.kt
│   │   └── Status.kt
│   └── repository/       # Data repositories
│       ├── AuthRepository.kt
│       ├── ChatRepository.kt
│       ├── StorageRepository.kt
│       └── StatusRepository.kt
├── presentation/
│   ├── navigation/       # Navigation setup
│   ├── screens/          # UI screens
│   │   ├── splash/
│   │   ├── auth/
│   │   ├── home/
│   │   ├── chats/
│   │   └── status/
│   └── viewmodel/        # ViewModels
│       ├── AuthViewModel.kt
│       ├── ChatViewModel.kt
│       └── StatusViewModel.kt
├── ui/theme/             # App theming
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
├── util/                 # Utilities
│   ├── Constants.kt
│   ├── DateTimeUtil.kt
│   └── Resource.kt
└── MainActivity.kt
```

## 🎨 UI Screenshots

*(Screenshots will be added as features are completed)*

## 🔒 Security Considerations

1. **Never commit** `google-services.json` to public repositories
2. Use **Firebase Security Rules** in production
3. Implement **rate limiting** for authentication
4. Validate **user input** on both client and server
5. Use **HTTPS only** for all network calls

## 🐛 Known Issues

- Chat screen UI is under development
- Media sharing not yet implemented
- Push notifications pending
- Group chats to be added

## 🤝 Contributing

This is a learning/demonstration project. Feel free to:
- Report bugs
- Suggest features
- Submit pull requests

## 📝 Next Steps

1. **Complete Chat Screen** - Message bubbles, send/receive UI
2. **Media Handling** - Image/video/audio/document sharing
3. **Voice Notes** - Recording and playback functionality
4. **Notifications** - FCM push notifications
5. **Polish UI** - Animations, transitions, error handling

## 📄 License

This project is for educational purposes. WhatsApp® is a registered trademark of Meta Platforms, Inc.

## 👨‍💻 Author

**CHOUDHARY**
- Created: December 2025
- Version: 1.0.0

## 🙏 Acknowledgments

- Firebase for backend services
- Jetpack Compose for modern UI
- Material Design 3 guidelines
- WhatsApp for design inspiration

---

**Note:** This is a clone project for learning purposes and is not affiliated with or endorsed by WhatsApp or Meta Platforms, Inc.
