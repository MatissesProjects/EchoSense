package com.echosense.app

import android.content.Context
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import com.echosense.app.db.ConversationNoteDao
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class SummarizationManager(
    private val context: Context?,
    private val generativeModel: GenerativeModel? = null,
    private val db: EchoSenseDatabase? = null,
    private val daoOverride: ConversationNoteDao? = null,
    private val summarizer: (suspend (String) -> String)? = null
) {
    private val noteDao: ConversationNoteDao = daoOverride 
        ?: db?.conversationNoteDao() 
        ?: EchoSenseDatabase.getDatabase(context!!).conversationNoteDao()

    private val model: GenerativeModel by lazy {
        generativeModel ?: GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "TODO_USER_API_KEY"
        )
    }

    suspend fun getRecentNotesSummary(): String {
        val allNotes = noteDao.getAllNotes().first()
        if (allNotes.isEmpty()) return "No conversation history to summarize."

        val recentNotes = allNotes.take(50).reversed()
        val conversationText = recentNotes.joinToString("\n") { 
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
            "[$time]: ${it.text}"
        }

        return try {
            val summary = if (summarizer != null) {
                summarizer.invoke(conversationText)
            } else {
                val response = model.generateContent(content {
                    text("You are an intelligent hearing assistant. Summarize the following conversation notes into a few bullet points highlighting key names, dates, or tasks mentioned. If it's just noise or fragments, say 'No clear conversation detected'.\n\n$conversationText")
                })
                response.text ?: "AI could not generate a summary."
            }
            
            "--- AI CONTEXT SUMMARY ---\n" +
            summary +
            "\n\n--- Full Transcript ---\n" +
            conversationText
        } catch (e: Exception) {
            "--- CONVERSATION LOG ---\n" +
            "AI Summarization unavailable (check connection/API key).\n\n" +
            conversationText
        }
    }

    suspend fun clearHistory() {
        noteDao.deleteAllNotes()
    }
}
