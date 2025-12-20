package com.example.whatappclone.util

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.GeneralSecurityException

/**
 * EncryptionHelper - Handles end-to-end encryption for messages using Google Tink
 * 
 * Features:
 * - AES-GCM encryption (256-bit keys)
 * - Secure key storage using Android Keystore
 * - Base64 encoding for storage compatibility
 */
class EncryptionHelper(private val context: Context) {

    companion object {
        private const val KEYSET_NAME = "talknest_master_keyset"
        private const val PREFERENCE_FILE = "talknest_secure_prefs"
        private const val MASTER_KEY_URI = "android-keystore://talknest_master_key"
        
        @Volatile
        private var instance: EncryptionHelper? = null
        
        fun getInstance(context: Context): EncryptionHelper {
            return instance ?: synchronized(this) {
                instance ?: EncryptionHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val aead: Aead = run {
        try {
            android.util.Log.d("EncryptionHelper", "Initializing encryption...")
            
            // Initialize Tink
            AeadConfig.register()
            
            // Try to get keyset handle with proper error handling
            val prefs = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            val keysetHandle = if (prefs.contains(KEYSET_NAME)) {
                try {
                    // Try to load existing keyset
                    android.util.Log.d("EncryptionHelper", "Trying to load existing keyset...")
                    AndroidKeysetManager.Builder()
                        .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                        .withMasterKeyUri(MASTER_KEY_URI)
                        .build()
                        .keysetHandle
                        .also { android.util.Log.d("EncryptionHelper", "Loaded existing keyset") }
                } catch (e: Exception) {
                    android.util.Log.w("EncryptionHelper", "Failed to load existing keyset, creating new one", e)
                    // Clear corrupted keyset and create new one
                    prefs.edit().clear().apply()
                    
                    AndroidKeysetManager.Builder()
                        .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                        .withMasterKeyUri(MASTER_KEY_URI)
                        .build()
                        .keysetHandle
                        .also { android.util.Log.d("EncryptionHelper", "Created new keyset after error") }
                }
            } else {
                // Create new keyset
                android.util.Log.d("EncryptionHelper", "Creating initial keyset...")
                AndroidKeysetManager.Builder()
                    .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                    .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                    .withMasterKeyUri(MASTER_KEY_URI)
                    .build()
                    .keysetHandle
                    .also { android.util.Log.d("EncryptionHelper", "Created initial keyset") }
            }
            
            keysetHandle.getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            android.util.Log.e("EncryptionHelper", "Failed to initialize encryption", e)
            throw RuntimeException("Failed to initialize encryption", e)
        }
    }

    /**
     * Encrypt a message
     * @param plaintext The message to encrypt
     * @param associatedData Additional context data (e.g., chatId, senderId)
     * @return Base64 encoded encrypted message
     */
    fun encrypt(plaintext: String, associatedData: String = ""): String {
        return try {
            val ciphertext = aead.encrypt(
                plaintext.toByteArray(Charsets.UTF_8),
                associatedData.toByteArray(Charsets.UTF_8)
            )
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }

    /**
     * Decrypt a message
     * @param encryptedText Base64 encoded encrypted message
     * @param associatedData Additional context data (must match encryption)
     * @return Decrypted plaintext message
     */
    fun decrypt(encryptedText: String, associatedData: String = ""): String {
        return try {
            val ciphertext = Base64.decode(encryptedText, Base64.NO_WRAP)
            val plaintext = aead.decrypt(
                ciphertext,
                associatedData.toByteArray(Charsets.UTF_8)
            )
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }

    /**
     * Encrypt media URL (for secure media sharing)
     */
    fun encryptMediaUrl(url: String, chatId: String): String {
        return encrypt(url, "media_$chatId")
    }

    /**
     * Decrypt media URL
     */
    fun decryptMediaUrl(encryptedUrl: String, chatId: String): String {
        return decrypt(encryptedUrl, "media_$chatId")
    }

    /**
     * Generate a unique encryption key identifier for a chat
     */
    fun generateChatKeyId(chatId: String): String {
        return "chat_key_$chatId"
    }

    /**
     * Encrypt with chat-specific context
     */
    fun encryptForChat(message: String, chatId: String, senderId: String): String {
        val associatedData = "$chatId:$senderId"
        return encrypt(message, associatedData)
    }

    /**
     * Decrypt with chat-specific context
     */
    fun decryptForChat(encryptedMessage: String, chatId: String, senderId: String): String {
        val associatedData = "$chatId:$senderId"
        return decrypt(encryptedMessage, associatedData)
    }
}
