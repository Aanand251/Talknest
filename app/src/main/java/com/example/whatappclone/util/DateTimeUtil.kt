package com.example.whatappclone.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtil {
    
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes minute${if (minutes > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hour${if (hours > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days day${if (days > 1) "s" else ""} ago"
            }
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
    
    fun getMessageTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val messageCalendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        
        return when {
            calendar.get(Calendar.DATE) == messageCalendar.get(Calendar.DATE) &&
            calendar.get(Calendar.MONTH) == messageCalendar.get(Calendar.MONTH) &&
            calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) -> {
                // Today - show time only
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            calendar.get(Calendar.DATE) - 1 == messageCalendar.get(Calendar.DATE) &&
            calendar.get(Calendar.MONTH) == messageCalendar.get(Calendar.MONTH) &&
            calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) -> {
                // Yesterday
                "Yesterday"
            }
            else -> {
                // Other days
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
    
    fun getChatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
    
    fun getFullDateTime(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
    
    fun getLastSeenText(timestamp: Long, isOnline: Boolean): String {
        return if (isOnline) {
            "Online"
        } else {
            "Last seen ${getTimeAgo(timestamp)}"
        }
    }
    
    fun getLastSeenTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes min ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
