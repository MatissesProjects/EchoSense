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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SummarizationManagerTest {

    private lateinit var db: EchoSenseDatabase
    private lateinit var summarizationManager: SummarizationManager

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
        
        summarizationManager = SummarizationManager(context)
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
    }

    @Test
    fun testSummaryWithNotes() = runBlocking {
        val dao = db.conversationNoteDao()
        dao.insertNote(ConversationNote(text = "Hello, how are you?", timestamp = System.currentTimeMillis()))
        dao.insertNote(ConversationNote(text = "I am fine, thanks.", timestamp = System.currentTimeMillis() + 1000))
        
        val summary = summarizationManager.getRecentNotesSummary()
        
        // Since we are not actually calling the real AI (or if we are, it might fail/be mocked), 
        // we check if the full transcript is at least present.
        assertTrue(summary.contains("Hello, how are you?"))
        assertTrue(summary.contains("I am fine, thanks."))
        // It should contain either the AI summary header or the fallback header
        assertTrue(summary.contains("CONTEXT SUMMARY") || summary.contains("CONVERSATION LOG"))
    }
}
