package com.echosense.app.fakes

import com.echosense.app.db.ConversationNote
import com.echosense.app.db.ConversationNoteDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConversationNoteDao : ConversationNoteDao {
    private val notes = MutableStateFlow<List<ConversationNote>>(emptyList())

    override fun getAllNotes(): Flow<List<ConversationNote>> = notes

    override suspend fun insertNote(note: ConversationNote): Long {
        notes.value = notes.value + note
        return note.id.toLong()
    }

    override suspend fun deleteAllNotes() {
        notes.value = emptyList()
    }

    override suspend fun deleteNote(note: ConversationNote) {
        notes.value = notes.value.filter { it.id != note.id }
    }
}
