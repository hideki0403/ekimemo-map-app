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

data class MoveLogSessionSummary(
    val id: Long,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "first_timestamp") val firstTimestamp: Long,
    @ColumnInfo(name = "last_timestamp") val lastTimestamp: Long,
    val count: Int,
)

@Dao
interface MoveLogSessionDao {
    @Query("SELECT * FROM move_log_session ORDER BY start_time DESC")
    fun observeAll(): Flow<List<MoveLogSession>>

    @Query(
        """
        SELECT s.id, s.start_time, MIN(l.timestamp) AS first_timestamp, MAX(l.timestamp) AS last_timestamp, COUNT(*) AS count
        FROM move_log_session s JOIN move_log l ON l.session_id = s.id
        GROUP BY s.id ORDER BY s.start_time
        """,
    )
    fun observeSummaries(): Flow<List<MoveLogSessionSummary>>

    @Query("SELECT * FROM move_log_session WHERE id = :id")
    suspend fun get(id: Long): MoveLogSession?

    @Insert
    suspend fun insert(session: MoveLogSession): Long

    @Insert
    suspend fun insertAll(sessions: List<MoveLogSession>)

    @Query("DELETE FROM move_log_session WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
