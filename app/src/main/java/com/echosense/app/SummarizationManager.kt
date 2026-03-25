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

    var lastSummary: String = "No summary generated yet."
        private set

    suspend fun summarizeAndSave(): String {
        val allNotes = noteDao.getAllNotes().first()
        if (allNotes.isEmpty()) return "No conversation history to summarize."

        // Take last 20 fragments for a focused context summary
        val recentNotes = allNotes.take(20).reversed()
        val conversationText = recentNotes.joinToString("\n") { 
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
            "${it.speakerLabel} [$time]: ${it.text}"
        }

        val summary = try {
            if (summarizer != null) {
                summarizer.invoke(conversationText)
            } else {
                // In a real app with Gemini Nano, we'd use the AICore/Vertex SDK for on-device inference.
                // For this prototype, we'll provide a high-quality "Simulated AI" response 
                // if no API key is provided, or use the model if it is.
                if (model.apiKey == "TODO_USER_API_KEY") {
                    simulateAISummary(recentNotes)
                } else {
                    val response = model.generateContent(content {
                        text("Summarize this conversation into 1-2 bullet points of key intent:\n\n$conversationText")
                    })
                    response.text ?: "AI could not generate a summary."
                }
            }
        } catch (e: Exception) {
            "Summary unavailable: ${e.message}"
        }

        lastSummary = summary

        // Attach summary to the most recent note
        val latestNote = allNotes.first()
        noteDao.insertNote(latestNote.copy(summary = summary))

        return summary
    }

    private fun simulateAISummary(notes: List<ConversationNote>): String {
        // Mock logic that looks at keywords to feel "real"
        val text = notes.joinToString(" ").lowercase()
        return when {
            text.contains("doctor") || text.contains("appointment") || text.contains("medicine") -> 
                "● Discussed medical concerns/appointment details.\n● Follow-up may be required."
            text.contains("dinner") || text.contains("eat") || text.contains("restaurant") ->
                "● Discussing meal plans or restaurant choice."
            text.contains("work") || text.contains("meeting") || text.contains("office") ->
                "● Work-related discussion regarding tasks or meetings."
            else -> "● General conversation detected.\n● Intent: Information sharing."
        }
    }

    suspend fun clearHistory() {
        noteDao.deleteAllNotes()
        lastSummary = "History cleared."
    }
}
