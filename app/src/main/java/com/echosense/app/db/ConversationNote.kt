package com.echosense.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_notes")
data class ConversationNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioProfileId: Int? = null, // Which profile was active during this note
    val summary: String? = null // For Phase 3 AICore integration
)
