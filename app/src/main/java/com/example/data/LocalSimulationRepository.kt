package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.engine.LocalSimulationState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_game_save")
data class LocalGameSaveEntity(
    @PrimaryKey val saveId: String = "primary",
    val stateJson: String,
    val updatedAt: Long
)

@Dao
interface LocalGameSaveDao {
    @Query("SELECT * FROM local_game_save WHERE saveId = :saveId LIMIT 1")
    suspend fun find(saveId: String): LocalGameSaveEntity?

    @Query("SELECT * FROM local_game_save WHERE saveId = :saveId LIMIT 1")
    fun observe(saveId: String): Flow<LocalGameSaveEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(save: LocalGameSaveEntity)

    @Query("DELETE FROM local_game_save WHERE saveId = :saveId")
    suspend fun delete(saveId: String)
}

@Database(entities = [LocalGameSaveEntity::class], version = 1, exportSchema = false)
abstract class LocalGameDatabase : RoomDatabase() {
    abstract fun saves(): LocalGameSaveDao

    companion object {
        @Volatile private var instance: LocalGameDatabase? = null
        fun get(context: Context): LocalGameDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalGameDatabase::class.java,
                    "capital_fortune_local.db"
                ).build().also { instance = it }
            }
    }
}

/** Local authoritative save. A state snapshot and its ledger persist as one Room row. */
class LocalSimulationRepository(context: Context) {
    private val dao = LocalGameDatabase.get(context).saves()
    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(LocalSimulationState::class.java)

    suspend fun load(): LocalSimulationState {
        val json = dao.find("primary")?.stateJson ?: return LocalSimulationState()
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: LocalSimulationState()
    }

    suspend fun save(state: LocalSimulationState) {
        dao.upsert(LocalGameSaveEntity(stateJson = adapter.toJson(state), updatedAt = System.currentTimeMillis()))
    }

    fun observe(): Flow<LocalGameSaveEntity?> = dao.observe("primary")
    suspend fun clear() = dao.delete("primary")
}
