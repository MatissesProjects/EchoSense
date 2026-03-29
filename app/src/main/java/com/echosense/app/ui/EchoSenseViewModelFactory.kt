package com.echosense.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.echosense.app.AudioSettingsManager
import com.echosense.app.db.ConversationNoteDao

class EchoSenseViewModelFactory(
    private val settingsManager: AudioSettingsManager? = null,
    private val noteDao: ConversationNoteDao? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(settingsManager!!) as T
            }
            modelClass.isAssignableFrom(NotesHistoryViewModel::class.java) -> {
                NotesHistoryViewModel(noteDao!!) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
