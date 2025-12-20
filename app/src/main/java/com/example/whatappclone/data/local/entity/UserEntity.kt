package com.example.whatappclone.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val phoneNumber: String,
    val name: String,
    val about: String,
    val profileImageUrl: String,
    val lastSeen: Long,
    val isOnline: Boolean
)
