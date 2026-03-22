package com.echosense.app.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioProfileDao {
    @Query("SELECT * FROM audio_profiles")
    fun getAllProfiles(): Flow<List<AudioProfileEntity>>

    @Query("SELECT * FROM audio_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): AudioProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AudioProfileEntity): Long

    @Delete
    suspend fun deleteProfile(profile: AudioProfileEntity)
}
