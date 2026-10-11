package dev.yukineko.ekimemo_map.data.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "interval_timer")
data class IntervalTimer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "duration_sec") val durationSec: Int,
    @ColumnInfo(name = "enable_notification") val enableNotification: Boolean,
    @ColumnInfo(name = "enable_tts") val enableTts: Boolean,
    @ColumnInfo(name = "notification_sound") val notificationSound: String?,
    @ColumnInfo(name = "vibration_pattern") val vibrationPattern: String?,
)

@Dao
interface IntervalTimerDao {
    @Query("SELECT * FROM interval_timer ORDER BY id")
    fun observeAll(): Flow<List<IntervalTimer>>

    @Insert
    suspend fun insert(timer: IntervalTimer): Long

    @Insert
    suspend fun insertAll(timers: List<IntervalTimer>)

    @Update
    suspend fun update(timer: IntervalTimer)

    @Delete
    suspend fun delete(timer: IntervalTimer)
}
