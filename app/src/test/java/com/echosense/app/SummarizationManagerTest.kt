package com.echosense.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
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
        summarizationManager = SummarizationManager(context)
        // Note: SummarizationManager internally gets the real DB, 
        // so for this test we need to be careful or use a Factory pattern.
        // For simplicity, let's test the formatting logic if possible.
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testEmptySummary() = runBlocking {
        // This will use the real context from Robolectric
        val summary = summarizationManager.getRecentNotesSummary()
        assertTrue(summary.contains("No conversation history"))
    }
}
