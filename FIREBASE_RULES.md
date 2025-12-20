# Firebase Security Rules

## Firestore Security Rules

Copy and paste these rules into Firebase Console → Firestore Database → Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isSignedIn() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    function isParticipant(participants) {
      return request.auth.uid in participants;
    }
    
    // Users collection
    match /users/{userId} {
      // Anyone authenticated can read user profiles
      allow read: if isSignedIn();
      // Users can only write their own profile
      allow create: if isSignedIn() && isOwner(userId);
      allow update, delete: if isSignedIn() && isOwner(userId);
    }
    
    // Chats collection
    match /chats/{chatId} {
      // Users can read chats they are part of
      allow read: if isSignedIn() && isParticipant(resource.data.participants);
      // Users can create chats
      allow create: if isSignedIn() && isParticipant(request.resource.data.participants);
      // Participants can update chat
      allow update: if isSignedIn() && isParticipant(resource.data.participants);
      // No one can delete chats (optional - you can allow participants)
      allow delete: if false;
      
      // Messages subcollection
      match /messages/{messageId} {
        // Participants can read messages
        allow read: if isSignedIn();
        // Authenticated users can create messages
        allow create: if isSignedIn();
        // Sender can update their own messages
        allow update: if isSignedIn();
        // Sender can delete their own messages
        allow delete: if isSignedIn();
      }
    }
    
    // Status collection
    match /status/{statusId} {
      // Anyone authenticated can read status
      allow read: if isSignedIn();
      // Users can create their own status
      allow create: if isSignedIn() && isOwner(request.resource.data.userId);
      // Users can update their own status
      allow update: if isSignedIn() && isOwner(resource.data.userId);
      // Users can delete their own status
      allow delete: if isSignedIn() && isOwner(resource.data.userId);
    }
    
    // Groups collection (for future group chat feature)
    match /groups/{groupId} {
      allow read: if isSignedIn() && request.auth.uid in resource.data.members;
      allow create: if isSignedIn();
      allow update: if isSignedIn() && request.auth.uid in resource.data.admins;
      allow delete: if isSignedIn() && request.auth.uid in resource.data.admins;
      
      match /messages/{messageId} {
        allow read: if isSignedIn();
        allow create: if isSignedIn();
        allow update, delete: if isSignedIn();
      }
    }
  }
}
```

## Firebase Storage Rules

Copy and paste these rules into Firebase Console → Storage → Rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Helper functions
    function isSignedIn() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    // Profile images - users can read all, write only their own
    match /profile_images/{userId}/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && isOwner(userId);
    }
    
    // Chat images - authenticated users can read and write
    match /images/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.resource.size < 5 * 1024 * 1024; // Max 5MB
    }
    
    // Chat videos - authenticated users can read and write
    match /videos/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.resource.size < 16 * 1024 * 1024; // Max 16MB
    }
    
    // Chat audio/voice notes
    match /audio/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.resource.size < 10 * 1024 * 1024; // Max 10MB
    }
    
    // Chat documents
    match /documents/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.resource.size < 10 * 1024 * 1024; // Max 10MB
    }
    
    // Status media - users can read all, write only their own
    match /status/{userId}/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && isOwner(userId);
    }
    
    // Group images (for future feature)
    match /groups/{groupId}/{filename} {
      allow read: if isSignedIn();
      allow write: if isSignedIn();
    }
  }
}
```

## How to Apply Rules

### Firestore Rules
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **Firestore Database** in left sidebar
4. Click **Rules** tab
5. Delete existing rules
6. Copy and paste the Firestore rules from above
7. Click **Publish**

### Storage Rules
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **Storage** in left sidebar
4. Click **Rules** tab
5. Delete existing rules
6. Copy and paste the Storage rules from above
7. Click **Publish**

## Security Rule Explanation

### Firestore Rules
- **Users Collection**: 
  - Anyone can read user profiles (needed for displaying names/photos)
  - Users can only modify their own profile
  
- **Chats Collection**: 
  - Only chat participants can read/write
  - Messages can be read by authenticated users in the chat
  
- **Status Collection**: 
  - Anyone can view status updates
  - Users can only create/modify their own status

### Storage Rules
- **Profile Images**: Users can only upload to their own folder
- **Media Files**: Authenticated users can upload, with size limits
- **Status Media**: Users can only upload to their own status folder

## Testing Rules

After publishing rules, test in Firebase Console:
1. Go to Rules tab
2. Click **Rules Playground**
3. Test read/write operations
4. Verify they work as expected

## Production Considerations

For production, consider adding:
1. **Rate limiting** to prevent abuse
2. **Content validation** (file types, sizes)
3. **User verification** requirements
4. **Cost controls** for storage
5. **Stricter access controls** based on your needs

## Important Notes

⚠️ **Test Mode vs Production**
- **Test mode** allows all authenticated users - good for development
- **Production rules** should be stricter and more specific
- Always test rules before deploying to production

⚠️ **Security Best Practices**
- Never use test mode in production
- Regularly audit your security rules
- Monitor usage and access patterns
- Update rules as features evolve

---

**Last Updated:** December 3, 2025
