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
        
        val summary = summarizationManager.summarizeAndSave()
        
        assertTrue(summary.contains("No conversation history"))
    }

    @Test
    fun testSummaryWithNotesSuccess() = runBlocking {
        fakeDao.insertNote(ConversationNote(text = "Hello", timestamp = 1000, speakerLabel = "Speaker A"))
        
        summarizationManager = SummarizationManager(
            context = null,
            daoOverride = fakeDao,
            summarizer = { "This is a fake summary" }
        )
        
        val summary = summarizationManager.summarizeAndSave()
        
        assertTrue(summary.contains("This is a fake summary"))
    }

    @Test
    fun testSummaryWithNotesFailure() = runBlocking {
        fakeDao.insertNote(ConversationNote(text = "Hello", timestamp = 1000, speakerLabel = "Speaker A"))
        
        summarizationManager = SummarizationManager(
            context = null,
            daoOverride = fakeDao,
            summarizer = { throw RuntimeException("Fake API Error") }
        )
        
        val summary = summarizationManager.summarizeAndSave()
        
        assertTrue(summary.contains("Summary unavailable"))
        assertTrue(summary.contains("Fake API Error"))
    }
}
