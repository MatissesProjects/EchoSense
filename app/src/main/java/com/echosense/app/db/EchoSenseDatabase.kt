package com.echosense.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AudioProfileEntity::class, ConversationNote::class], version = 1, exportSchema = false)
abstract class EchoSenseDatabase : RoomDatabase() {
    abstract fun audioProfileDao(): AudioProfileDao
    abstract fun conversationNoteDao(): ConversationNoteDao

    companion object {
        @Volatile
        private var INSTANCE: EchoSenseDatabase? = null

        fun getDatabase(context: Context): EchoSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoSenseDatabase::class.java,
                    "echosense_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
