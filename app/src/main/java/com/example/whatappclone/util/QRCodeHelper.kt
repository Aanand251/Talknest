package com.example.whatappclone.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel


object QRCodeHelper {
    
    /**
     * Generate QR code bitmap from user data
     * @param userId User's Firebase UID
     * @param email User's email
     * @param name User's display name
     * @param size QR code size in pixels
     * @return Bitmap of QR code
     */
    fun generateProfileQRCode(
        userId: String,
        email: String,
        name: String,
        size: Int = 512
    ): Bitmap {
        // Create QR data string with user info
        val qrData = "TALKNEST:$userId|$email|$name"
        
        val hints = hashMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        hints[EncodeHintType.MARGIN] = 1
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, size, size, hints)
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Parse QR code data to extract user information
     * @param qrData Scanned QR code string
     * @return Triple of (userId, email, name) or null if invalid
     */
    fun parseQRCode(qrData: String): Triple<String, String, String>? {
        if (!qrData.startsWith("TALKNEST:")) return null
        
        val data = qrData.removePrefix("TALKNEST:")
        val parts = data.split("|")
        
        if (parts.size != 3) return null
        
        return Triple(parts[0], parts[1], parts[2])
    }
}
