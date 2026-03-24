package com.echosense.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SummarizationManagerTest {

    private lateinit var db: EchoSenseDatabase
    private lateinit var summarizationManager: SummarizationManager
    private lateinit var mockGenerativeModel: GenerativeModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EchoSenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Inject the in-memory DB into the singleton
        val field = EchoSenseDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, db)
        
        mockGenerativeModel = mock()
        summarizationManager = SummarizationManager(context, mockGenerativeModel)
    }

    @After
    fun tearDown() {
        db.close()
        // Clear the singleton
        val field = EchoSenseDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun testEmptySummary() = runBlocking {
        val summary = summarizationManager.getRecentNotesSummary()
        assertTrue(summary.contains("No conversation history"))
        verifyNoInteractions(mockGenerativeModel)
    }

    @Test
    fun testSummaryWithNotesSuccess() = runBlocking {
        val dao = db.conversationNoteDao()
        dao.insertNote(ConversationNote(text = "Hello", timestamp = 1000))
        
        val mockResponse: GenerateContentResponse = mock {
            on { text } doReturn "This is a summary"
        }
        whenever(mockGenerativeModel.generateContent(any())).thenReturn(mockResponse)
        
        val summary = summarizationManager.getRecentNotesSummary()
        
        assertTrue(summary.contains("--- AI CONTEXT SUMMARY ---"))
        assertTrue(summary.contains("This is a summary"))
        assertTrue(summary.contains("Hello"))
    }

    @Test
    fun testSummaryWithNotesFailure() = runBlocking {
        val dao = db.conversationNoteDao()
        dao.insertNote(ConversationNote(text = "Hello", timestamp = 1000))
        
        whenever(mockGenerativeModel.generateContent(any())).thenThrow(RuntimeException("API Error"))
        
        val summary = summarizationManager.getRecentNotesSummary()
        
        assertTrue(summary.contains("--- CONVERSATION LOG ---"))
        assertTrue(summary.contains("AI Summarization unavailable"))
        assertTrue(summary.contains("Hello"))
    }
}
