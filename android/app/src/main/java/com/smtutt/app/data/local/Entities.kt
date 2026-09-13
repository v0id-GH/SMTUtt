package com.smtutt.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_schedules")
data class CachedScheduleEntity(
    @PrimaryKey
    val targetKey: String, // e.g., "group_7738" or "teacher_101505"
    val targetId: String,
    val title: String,
    val isTeacher: Boolean,
    val jsonData: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String, // ID of group or teacher
    val name: String,
    val isTeacher: Boolean,
    val isDefault: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
