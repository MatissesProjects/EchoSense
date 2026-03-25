package com.echosense.app

import com.echosense.app.db.ConversationNote
import com.echosense.app.fakes.FakeConversationNoteDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SummarizationManagerTest {

    private lateinit var fakeDao: FakeConversationNoteDao
    private lateinit var summarizationManager: SummarizationManager

    @Before
    fun setup() {
        fakeDao = FakeConversationNoteDao()
    }

    @Test
    fun testEmptySummary() = runBlocking {
        summarizationManager = SummarizationManager(
            context = null,
            daoOverride = fakeDao
        )
        
        val summary = summarizationManager.getRecentNotesSummary()
        
        assertTrue(summary.contains("No conversation history"))
    }

    @Test
    fun testSummaryWithNotesSuccess() = runBlocking {
        fakeDao.insertNote(ConversationNote(text = "Hello", timestamp = 1000))
        
        summarizationManager = SummarizationManager(
            context = null,
            daoOverride = fakeDao,
            summarizer = { "This is a fake summary" }
        )
        
        val summary = summarizationManager.getRecentNotesSummary()
        
        assertTrue(summary.contains("--- AI CONTEXT SUMMARY ---"))
        assertTrue(summary.contains("This is a fake summary"))
        assertTrue(summary.contains("Hello"))
    }

    @Test
    fun testSummaryWithNotesFailure() = runBlocking {
        fakeDao.insertNote(ConversationNote(text = "Hello", timestamp = 1000))
        
        summarizationManager = SummarizationManager(
            context = null,
            daoOverride = fakeDao,
            summarizer = { throw RuntimeException("Fake API Error") }
        )
        
        val summary = summarizationManager.getRecentNotesSummary()
        
        assertTrue(summary.contains("--- CONVERSATION LOG ---"))
        assertTrue(summary.contains("AI Summarization unavailable"))
        assertTrue(summary.contains("Hello"))
    }
}
