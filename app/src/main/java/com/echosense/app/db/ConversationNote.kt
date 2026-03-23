package com.echosense.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_notes")
data class ConversationNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val speakerLabel: String = "Unknown", // Added for diarization
    val timestamp: Long = System.currentTimeMillis(),
    val audioProfileId: Int? = null,
    val summary: String? = null
)
