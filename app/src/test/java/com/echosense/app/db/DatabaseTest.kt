package com.echosense.app.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseTest {
    private lateinit var db: EchoSenseDatabase
    private lateinit var audioProfileDao: AudioProfileDao
    private lateinit var conversationNoteDao: ConversationNoteDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EchoSenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        audioProfileDao = db.audioProfileDao()
        conversationNoteDao = db.conversationNoteDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndGetProfile() = runBlocking {
        val profile = AudioProfileEntity(name = "Test Profile", preAmpGain = 2.5f)
        val id = audioProfileDao.insertProfile(profile)
        
        val allProfiles = audioProfileDao.getAllProfiles().first()
        assertEquals(1, allProfiles.size)
        assertEquals("Test Profile", allProfiles[0].name)
        assertEquals(2.5f, allProfiles[0].preAmpGain)
    }

    @Test
    fun testInsertAndGetNote() = runBlocking {
        val note = ConversationNote(text = "Important conversation", speakerLabel = "Doctor")
        conversationNoteDao.insertNote(note)
        
        val allNotes = conversationNoteDao.getAllNotes().first()
        assertEquals(1, allNotes.size)
        assertEquals("Important conversation", allNotes[0].text)
        assertEquals("Doctor", allNotes[0].speakerLabel)
    }

    @Test
    fun testDeleteNote() = runBlocking {
        val note = ConversationNote(text = "Temporary note")
        val id = conversationNoteDao.insertNote(note)
        val insertedNote = conversationNoteDao.getAllNotes().first()[0]
        
        conversationNoteDao.deleteNote(insertedNote)
        
        val allNotes = conversationNoteDao.getAllNotes().first()
        assertTrue(allNotes.isEmpty())
    }

    @Test
    fun testGetProfileById() = runBlocking {
        val profile = AudioProfileEntity(name = "Specific Profile", preAmpGain = 1.0f)
        val id = audioProfileDao.insertProfile(profile).toInt()
        
        val fetched = audioProfileDao.getProfileById(id)
        assertNotNull(fetched)
        assertEquals("Specific Profile", fetched?.name)
    }
}
