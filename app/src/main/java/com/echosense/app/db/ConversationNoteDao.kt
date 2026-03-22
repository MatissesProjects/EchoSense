package com.echosense.app.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationNoteDao {
    @Query("SELECT * FROM conversation_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<ConversationNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ConversationNote): Long

    @Query("DELETE FROM conversation_notes")
    suspend fun deleteAllNotes()

    @Delete
    suspend fun deleteNote(note: ConversationNote)
}
