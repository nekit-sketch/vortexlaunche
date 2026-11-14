package com.vortex.launcher.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarIndex: Int = 0,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id") fun observeAll(): Flow<List<ProfileEntity>>
    @Insert suspend fun insert(p: ProfileEntity): Long
    @Update suspend fun update(p: ProfileEntity)
    @Delete suspend fun delete(p: ProfileEntity)
}

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = false)
abstract class VortexDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
