package com.notes.satu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val modifiedAt: Long = System.currentTimeMillis(),
    val isLocked: Boolean = false
)
