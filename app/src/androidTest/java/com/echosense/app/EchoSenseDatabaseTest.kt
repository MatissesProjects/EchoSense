package com.echosense.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.ConversationNoteDao
import com.echosense.app.db.EchoSenseDatabase
import com.echosense.app.db.AudioProfileEntity
import com.echosense.app.db.AudioProfileDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EchoSenseDatabaseTest {
    private lateinit var conversationNoteDao: ConversationNoteDao
    private lateinit var audioProfileDao: AudioProfileDao
    private lateinit var db: EchoSenseDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EchoSenseDatabase::class.java).build()
        conversationNoteDao = db.conversationNoteDao()
        audioProfileDao = db.audioProfileDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeNoteAndReadInList() = runBlocking {
        val note = ConversationNote(text = "Testing EchoSense Room DB")
        conversationNoteDao.insertNote(note)
        val allNotes = conversationNoteDao.getAllNotes().first()
        assertEquals(allNotes[0].text, "Testing EchoSense Room DB")
    }

    @Test
    @Throws(Exception::class)
    fun clearHistoryTest() = runBlocking {
        conversationNoteDao.insertNote(ConversationNote(text = "Note 1"))
        conversationNoteDao.insertNote(ConversationNote(text = "Note 2"))
        conversationNoteDao.deleteAllNotes()
        val allNotes = conversationNoteDao.getAllNotes().first()
        assertTrue(allNotes.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun profileStorageTest() = runBlocking {
        val profile = AudioProfileEntity(name = "Bus Ride", hpfFreq = 350.0f)
        val id = audioProfileDao.insertProfile(profile)
        val retrieved = audioProfileDao.getProfileById(id.toInt())
        assertEquals(350.0f, retrieved?.hpfFreq)
    }
}
