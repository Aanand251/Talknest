package com.example.whatappclone.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.whatappclone.data.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object DownloadUtil {
    
    suspend fun downloadMedia(context: Context, url: String, type: MessageType) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = "TalkNest_${System.currentTimeMillis()}.${getExtension(type)}"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadToMediaStore(context, url, fileName, type)
                } else {
                    downloadToLegacy(context, url, fileName, type)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloaded successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun downloadToMediaStore(context: Context, url: String, fileName: String, type: MessageType) {
        val collection = when (type) {
            MessageType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MessageType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }
        }
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(type))
            put(MediaStore.MediaColumns.RELATIVE_PATH, getRelativePath(type))
        }
        
        val uri = context.contentResolver.insert(collection, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                URL(url).openStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
    
    private suspend fun downloadToLegacy(context: Context, url: String, fileName: String, type: MessageType) {
        val directory = when (type) {
            MessageType.IMAGE -> Environment.DIRECTORY_PICTURES
            MessageType.VIDEO -> Environment.DIRECTORY_MOVIES
            else -> Environment.DIRECTORY_DOWNLOADS
        }
        
        val folder = File(Environment.getExternalStoragePublicDirectory(directory), "TalkNest")
        if (!folder.exists()) folder.mkdirs()
        
        val file = File(folder, fileName)
        FileOutputStream(file).use { outputStream ->
            URL(url).openStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    
    private fun getExtension(type: MessageType): String {
        return when (type) {
            MessageType.IMAGE -> "jpg"
            MessageType.VIDEO -> "mp4"
            MessageType.DOCUMENT -> "pdf"
            else -> "bin"
        }
    }
    
    private fun getMimeType(type: MessageType): String {
        return when (type) {
            MessageType.IMAGE -> "image/jpeg"
            MessageType.VIDEO -> "video/mp4"
            MessageType.DOCUMENT -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
    
    private fun getRelativePath(type: MessageType): String {
        return when (type) {
            MessageType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/TalkNest"
            MessageType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/TalkNest"
            else -> "${Environment.DIRECTORY_DOWNLOADS}/TalkNest"
        }
    }
}
