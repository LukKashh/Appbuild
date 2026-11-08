package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "game_snapshot")
data class GameSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val payload: String,
    val schemaVersion: Int = 1,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface GameSnapshotDao {
    @Query("SELECT * FROM game_snapshot WHERE id = 1 LIMIT 1")
    suspend fun read(): GameSnapshotEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(snapshot: GameSnapshotEntity)
    @Query("DELETE FROM game_snapshot")
    suspend fun clear()
}

@Database(entities = [GameSnapshotEntity::class], version = 1, exportSchema = false)
abstract class LocalGameDatabase : RoomDatabase() {
    abstract fun snapshots(): GameSnapshotDao
}