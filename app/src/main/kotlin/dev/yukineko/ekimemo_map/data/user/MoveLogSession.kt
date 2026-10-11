package dev.yukineko.ekimemo_map.data.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "move_log_session", indices = [Index("start_time")])
data class MoveLogSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_time") val startTime: Long,
)

@Dao
interface MoveLogSessionDao {
    @Query("SELECT * FROM move_log_session ORDER BY start_time DESC")
    fun observeAll(): Flow<List<MoveLogSession>>

    @Query("SELECT * FROM move_log_session WHERE id = :id")
    suspend fun get(id: Long): MoveLogSession?

    @Insert
    suspend fun insert(session: MoveLogSession): Long

    @Query("DELETE FROM move_log_session WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
