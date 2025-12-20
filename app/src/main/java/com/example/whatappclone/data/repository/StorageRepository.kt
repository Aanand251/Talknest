package com.example.whatappclone.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {
    
    // Initialize with correct bucket URL from google-services.json
    private val storage = FirebaseStorage.getInstance("gs://whatappclone-6ef53.appspot.com")
    
    init {
        // Log storage bucket for debugging
        Log.d("StorageRepository", "Storage Bucket: ${storage.reference.bucket}")
        Log.d("StorageRepository", "Storage URL: gs://whatappclone-6ef53.appspot.com")
    }
    
    suspend fun uploadImage(uri: Uri, path: String = "images"): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("$path/$fileName")
            
            Log.d("StorageRepository", "Uploading image to: $path/$fileName")
            Log.d("StorageRepository", "Full storage path: gs://whatappclone-6ef53.appspot.com/$path/$fileName")
            
            ref.putFile(uri).await()
            Log.d("StorageRepository", "Upload successful, getting download URL...")
            
            val downloadUrl = ref.downloadUrl.await()
            Log.d("StorageRepository", "Download URL: $downloadUrl")
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload image failed", e)
            Log.e("StorageRepository", "Error details: ${e.message}")
            Log.e("StorageRepository", "Error cause: ${e.cause}")
            Result.failure(e)
        }
    }
    
    suspend fun uploadVideo(uri: Uri, path: String = "videos"): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.mp4"
            val ref = storage.reference.child("$path/$fileName")
            
            Log.d("StorageRepository", "Uploading video to: $path/$fileName")
            
            ref.putFile(uri).await()
            Log.d("StorageRepository", "Upload successful, getting download URL...")
            
            val downloadUrl = ref.downloadUrl.await()
            Log.d("StorageRepository", "Download URL: $downloadUrl")
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload video failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadAudio(uri: Uri, path: String = "audio"): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.m4a"
            val ref = storage.reference.child("$path/$fileName")
            
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadDocument(uri: Uri, fileName: String, path: String = "documents"): Result<String> {
        return try {
            val uniqueFileName = "${UUID.randomUUID()}_$fileName"
            val ref = storage.reference.child("$path/$uniqueFileName")
            
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadProfileImage(uri: Uri, userId: String): Result<String> {
        return uploadImage(uri, "profile_images/$userId")
    }
    
    suspend fun uploadStatusMedia(uri: Uri, userId: String, isVideo: Boolean): Result<String> {
        Log.d("StorageRepository", "Uploading status media for user: $userId, isVideo: $isVideo")
        return if (isVideo) {
            uploadVideo(uri, "status_videos/$userId")
        } else {
            uploadImage(uri, "status_images/$userId")
        }
    }
}
