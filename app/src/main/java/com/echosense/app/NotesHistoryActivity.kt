package com.echosense.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.echosense.app.ui.EchoSenseViewModelFactory
import com.echosense.app.ui.EchoSenseTheme
import com.echosense.app.ui.NotesHistoryViewModel
import com.echosense.app.ui.screens.NotesHistoryScreen

class NotesHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = EchoSenseDatabase.getDatabase(this)
        val factory = EchoSenseViewModelFactory(noteDao = db.conversationNoteDao())
        val viewModel = ViewModelProvider(this, factory)[NotesHistoryViewModel::class.java]

        setContent {
            EchoSenseTheme {
                NotesHistoryScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}
