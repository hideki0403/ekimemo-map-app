package dev.yukineko.ekimemo_map.data.legacy

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.room.withTransaction
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.data.user.AccessLog
import dev.yukineko.ekimemo_map.data.user.IntervalTimer
import dev.yukineko.ekimemo_map.data.user.MoveLog
import dev.yukineko.ekimemo_map.data.user.MoveLogSession
import dev.yukineko.ekimemo_map.data.user.PassingLog
import dev.yukineko.ekimemo_map.data.user.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface LegacyImportResult {
    data object NotFound : LegacyImportResult
    data object Imported : LegacyImportResult
    data class Discarded(val dumpPath: String) : LegacyImportResult
}

class LegacyDatabaseImporter(
    private val context: Context,
    private val database: UserDatabase,
    private val assistantSettings: DataStore<AssistantSettings>,
) {
    private val file = context.getDatabasePath(NAME)

    suspend fun import(): LegacyImportResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext LegacyImportResult.NotFound

        val (version, data) = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.version to if (db.version < SUPPORTED_VERSION) null else read(db)
        }
        val result = if (data == null) {
            LegacyImportResult.Discarded(dump(version))
        } else {
            database.withTransaction {
                database.accessLogDao().insertAll(data.accessLogs)
                database.passingLogDao().insertAll(data.passingLogs)
                database.moveLogSessionDao().insertAll(data.moveLogSessions)
                database.moveLogDao().insertAll(data.moveLogs)
                database.intervalTimerDao().insertAll(data.intervalTimers)
            }
            assistantSettings.updateData { data.assistantSettings }
            LegacyImportResult.Imported
        }
        SQLiteDatabase.deleteDatabase(file)
        result
    }

    private fun dump(version: Int): String {
        val name = "ekimemo_map_database_v$version.db"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dest = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name)
            file.copyTo(dest, overwrite = true)
            return dest.path
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        checkNotNull(resolver.openOutputStream(uri)).use { output -> file.inputStream().use { it.copyTo(output) } }
        return "${Environment.DIRECTORY_DOWNLOADS}/$name"
    }

    private fun read(db: SQLiteDatabase): LegacyData {
        val zone = ZoneId.systemDefault()

        val accessLogs = db.readAll("access_log") {
            AccessLog(
                stationId = int("id"),
                firstAccess = parseLegacyDateTime(string("first_access"), zone),
                lastAccess = parseLegacyDateTime(string("last_access"), zone),
                accessCount = int("access_count"),
                accessed = int("accessed") != 0,
            )
        }

        val passingLogs = db.readAll("passing_log", orderBy = "timestamp") {
            if (isNull("id")) return@readAll null
            PassingLog(
                stationId = int("id"),
                timestamp = parseLegacyDateTime(string("timestamp"), zone),
                latitude = double("latitude"),
                longitude = double("longitude"),
                speed = double("speed"),
                accuracy = double("accuracy"),
                distance = int("distance"),
                isRenotify = int("isReNotify") != 0,
            )
        }

        val sessionIds = mutableMapOf<String, Long>()
        val moveLogSessions = db.readAll("move_log_session", orderBy = "start_time") {
            val id = sessionIds.size + 1L
            sessionIds[string("id")] = id
            MoveLogSession(id = id, startTime = long("start_time"))
        }

        val moveLogs = db.readAll("move_log", orderBy = "id") {
            MoveLog(
                sessionId = sessionIds[string("session_id")] ?: return@readAll null,
                timestamp = long("timestamp"),
                latitude = double("latitude"),
                longitude = double("longitude"),
                altitude = null,
                speed = double("speed"),
                accuracy = double("accuracy"),
            )
        }

        val intervalTimers = db.readAll("interval_timer") {
            IntervalTimer(
                name = stringOrNull("name").orEmpty(),
                durationSec = int("duration"),
                enableNotification = int("enable_notification") != 0,
                enableTts = int("enable_tts") != 0,
                notificationSound = legacyEnum(stringOrNull("notification_sound")),
                vibrationPattern = legacyEnum(stringOrNull("vibration_pattern")),
            )
        }

        val meta = db.readAll("meta") { string("key") to stringOrNull("value") }.toMap()
        val defaults = AssistantSettings()
        val assistantSettings = AssistantSettings(
            enabled = meta["enabled_assistant_flow"]?.toBooleanStrictOrNull() ?: defaults.enabled,
            flow = meta["assistant_flow"]?.let { Json.decodeFromString(it) } ?: defaults.flow,
            debugPackageName = meta["debug_package_name"] ?: defaults.debugPackageName,
        )

        return LegacyData(accessLogs, passingLogs, moveLogSessions, moveLogs, intervalTimers, assistantSettings)
    }

    private class LegacyData(
        val accessLogs: List<AccessLog>,
        val passingLogs: List<PassingLog>,
        val moveLogSessions: List<MoveLogSession>,
        val moveLogs: List<MoveLog>,
        val intervalTimers: List<IntervalTimer>,
        val assistantSettings: AssistantSettings,
    )

    companion object {
        private const val NAME = "database.db"
        private const val SUPPORTED_VERSION = 10
    }
}

internal fun parseLegacyDateTime(text: String, zone: ZoneId): Long =
    LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

internal inline fun <reified T : Enum<T>> legacyEnum(name: String?): T? {
    val normalized = name?.removePrefix("se")?.uppercase() ?: return null
    return enumValues<T>().firstOrNull { it.name.replace("_", "") == normalized }
}

private inline fun <T : Any> SQLiteDatabase.readAll(
    table: String,
    orderBy: String? = null,
    transform: Cursor.() -> T?,
): List<T> = query(table, null, null, null, null, null, orderBy).use { cursor ->
    buildList { while (cursor.moveToNext()) cursor.transform()?.let(::add) }
}

private fun Cursor.isNull(column: String) = isNull(getColumnIndexOrThrow(column))
private fun Cursor.int(column: String) = getInt(getColumnIndexOrThrow(column))
private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))
private fun Cursor.double(column: String) = getDouble(getColumnIndexOrThrow(column))
private fun Cursor.stringOrNull(column: String): String? = getString(getColumnIndexOrThrow(column))
private fun Cursor.string(column: String) = checkNotNull(stringOrNull(column)) { "$column is null" }
