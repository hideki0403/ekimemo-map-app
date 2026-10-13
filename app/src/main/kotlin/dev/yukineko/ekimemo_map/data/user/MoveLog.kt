package dev.yukineko.ekimemo_map.data.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "move_log",
    foreignKeys = [
        ForeignKey(
            entity = MoveLogSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id", "timestamp")],
)
data class MoveLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Double,
    val accuracy: Double,
)

@Dao
interface MoveLogDao {
    @Query("SELECT * FROM move_log WHERE session_id = :sessionId ORDER BY timestamp")
    suspend fun getBySessionId(sessionId: Long): List<MoveLog>

    @Query("SELECT * FROM move_log WHERE session_id IN (:sessionIds) ORDER BY timestamp")
    fun observeBySessionIds(sessionIds: List<Long>): Flow<List<MoveLog>>

    @Insert
    suspend fun insert(log: MoveLog)

    @Insert
    suspend fun insertAll(logs: List<MoveLog>)
}
