package com.echosense.app

import android.content.Context
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class SummarizationManager(
    private val context: Context,
    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", // Placeholder for Nano integration via AICore
        apiKey = "TODO_USER_API_KEY"    // The user will need to provide this or we use AICore system service
    )
) {

    private val db = EchoSenseDatabase.getDatabase(context)
    suspend fun getRecentNotesSummary(): String {
        val allNotes = db.conversationNoteDao().getAllNotes().first()
        if (allNotes.isEmpty()) return "No conversation history to summarize."

        val recentNotes = allNotes.take(50).reversed()
        val conversationText = recentNotes.joinToString("\n") { 
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
            "[$time]: ${it.text}"
        }

        return try {
            val response = generativeModel.generateContent(content {
                text("You are an intelligent hearing assistant. Summarize the following conversation notes into a few bullet points highlighting key names, dates, or tasks mentioned. If it's just noise or fragments, say 'No clear conversation detected'.\n\n$conversationText")
            })
            
            "--- AI CONTEXT SUMMARY ---\n" +
            (response.text ?: "AI could not generate a summary.") +
            "\n\n--- Full Transcript ---\n" +
            conversationText
        } catch (e: Exception) {
            "--- CONVERSATION LOG ---\n" +
            "AI Summarization unavailable (check connection/API key).\n\n" +
            conversationText
        }
    }

    suspend fun clearHistory() {
        db.conversationNoteDao().deleteAllNotes()
    }
}
