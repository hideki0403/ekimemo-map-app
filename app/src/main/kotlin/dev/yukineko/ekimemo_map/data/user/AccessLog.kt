package dev.yukineko.ekimemo_map.data.user

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "access_log")
data class AccessLog(
    @PrimaryKey @ColumnInfo(name = "station_id") val stationId: Int,
    @ColumnInfo(name = "first_access") val firstAccess: Long,
    @ColumnInfo(name = "last_access") val lastAccess: Long,
    @ColumnInfo(name = "access_count") val accessCount: Int,
    val accessed: Boolean,
)

@Dao
interface AccessLogDao {
    @Query("SELECT * FROM access_log")
    fun observeAll(): Flow<List<AccessLog>>

    @Query("SELECT * FROM access_log")
    suspend fun getAll(): List<AccessLog>

    @Query("SELECT * FROM access_log WHERE station_id = :stationId")
    suspend fun get(stationId: Int): AccessLog?

    @Query("SELECT * FROM access_log WHERE station_id = :stationId")
    fun observe(stationId: Int): Flow<AccessLog?>

    @Query("SELECT * FROM access_log WHERE station_id IN (:stationIds)")
    suspend fun getByStationIds(stationIds: List<Int>): List<AccessLog>

    @Query("SELECT * FROM access_log WHERE station_id IN (:stationIds)")
    fun observeByStationIds(stationIds: List<Int>): Flow<List<AccessLog>>

    @Upsert
    suspend fun upsert(log: AccessLog)

    @Insert
    suspend fun insertAll(logs: List<AccessLog>)

    @Query("DELETE FROM access_log")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(logs: List<AccessLog>) {
        deleteAll()
        insertAll(logs)
    }
}
