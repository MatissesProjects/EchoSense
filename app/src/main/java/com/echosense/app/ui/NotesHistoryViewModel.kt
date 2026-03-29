package com.echosense.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.ConversationNoteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesHistoryViewModel(private val noteDao: ConversationNoteDao) : ViewModel() {

    val notes: StateFlow<List<ConversationNote>> = noteDao.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteNote(note: ConversationNote) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }
}
