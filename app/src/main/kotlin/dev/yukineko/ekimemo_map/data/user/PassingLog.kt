package dev.yukineko.ekimemo_map.data.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "passing_log")
data class PassingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "station_id") val stationId: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Double,
    val distance: Int,
    @ColumnInfo(name = "is_renotify") val isRenotify: Boolean,
)

@Dao
interface PassingLogDao {
    @Query("SELECT * FROM passing_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PassingLog>>

    @Insert
    suspend fun insert(log: PassingLog)

    @Insert
    suspend fun insertAll(logs: List<PassingLog>)

    @Query("DELETE FROM passing_log")
    suspend fun deleteAll()
}
