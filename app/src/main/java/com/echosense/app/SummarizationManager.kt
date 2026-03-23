package com.echosense.app

import android.content.Context
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class SummarizationManager(private val context: Context) {

    private val db = EchoSenseDatabase.getDatabase(context)

    suspend fun getRecentNotesSummary(): String {
        val allNotes = db.conversationNoteDao().getAllNotes().first()
        if (allNotes.isEmpty()) return "No conversation history to summarize."

        // Limit to last 20 notes for now to keep it manageable
        val recentNotes = allNotes.take(20).reversed()
        val conversationText = recentNotes.joinToString("\n") { 
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
            "[$time]: ${it.text}"
        }

        return "--- CONVERSATION SUMMARY ---\n" +
               "Notes captured: ${recentNotes.size}\n" +
               "Last active: ${SimpleDateFormat("HH:mm:ss").format(Date(recentNotes.last().timestamp))}\n\n" +
               "Full Transcript:\n$conversationText"
               // Placeholder for Gemini Nano integration:
               // val aiSummary = generativeModel.generateContent("Summarize this: $conversationText")
               // return aiSummary.text
    }

    suspend fun clearHistory() {
        db.conversationNoteDao().deleteAllNotes()
    }
}
