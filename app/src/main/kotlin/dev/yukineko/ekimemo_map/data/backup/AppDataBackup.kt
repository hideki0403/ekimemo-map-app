package dev.yukineko.ekimemo_map.data.backup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.core.DataStore
import dev.yukineko.ekimemo_map.MainActivity
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.data.settings.JsonSerializer
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.user.UserDatabase
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

class AppDataBackup(
    private val context: Context,
    private val database: UserDatabase,
    private val settings: DataStore<Settings>,
    private val assistantSettings: DataStore<AssistantSettings>,
) {
    private val settingsSerializer = JsonSerializer(Settings.serializer(), Settings())
    private val assistantSettingsSerializer = JsonSerializer(AssistantSettings.serializer(), AssistantSettings())

    suspend fun export(output: OutputStream) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use {
            check(it.moveToFirst() && it.getInt(0) == 0) { "Failed to checkpoint database" }
        }
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            context.getDatabasePath(UserDatabase.NAME).inputStream().use { it.copyTo(zip) }
            zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
            settingsSerializer.writeTo(settings.data.first(), zip)
            zip.putNextEntry(ZipEntry(ASSISTANT_SETTINGS_ENTRY))
            assistantSettingsSerializer.writeTo(assistantSettings.data.first(), zip)
        }
    }

    suspend fun restore(input: InputStream) {
        val pending = pendingFile(context)
        pending.delete()
        try {
            var restoredSettings: Settings? = null
            var restoredAssistantSettings: AssistantSettings? = null
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        DATABASE_ENTRY -> pending.outputStream().use { zip.copyTo(it) }
                        SETTINGS_ENTRY -> restoredSettings = settingsSerializer.readFrom(zip)
                        ASSISTANT_SETTINGS_ENTRY -> restoredAssistantSettings = assistantSettingsSerializer.readFrom(zip)
                    }
                }
            }
            check(pending.exists()) { "$DATABASE_ENTRY not found" }
            val newSettings = checkNotNull(restoredSettings) { "$SETTINGS_ENTRY not found" }
            val newAssistantSettings = checkNotNull(restoredAssistantSettings) { "$ASSISTANT_SETTINGS_ENTRY not found" }
            verify(pending)
            settings.updateData { newSettings }
            assistantSettings.updateData { newAssistantSettings }
        } catch (e: Throwable) {
            pending.delete()
            throw e
        } finally {
            pending.deleteSidecars()
        }
        context.startActivity(Intent.makeRestartActivityTask(ComponentName(context, MainActivity::class.java)))
        exitProcess(0)
    }

    private fun verify(candidate: File) {
        val expected = database.openHelper.readableDatabase.query(IDENTITY_HASH_QUERY).use {
            check(it.moveToFirst()) { "Identity hash not found" }
            it.getString(0)
        }
        SQLiteDatabase.openDatabase(candidate.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            check(db.version == UserDatabase.VERSION) { "Unsupported database version: ${db.version}" }
            val hash = db.rawQuery(IDENTITY_HASH_QUERY, null).use { if (it.moveToFirst()) it.getString(0) else null }
            check(hash == expected) { "Database schema mismatch" }
        }
    }

    companion object {
        private const val DATABASE_ENTRY = UserDatabase.NAME
        private const val SETTINGS_ENTRY = "settings.json"
        private const val ASSISTANT_SETTINGS_ENTRY = "assistant_settings.json"
        private const val IDENTITY_HASH_QUERY = "SELECT identity_hash FROM room_master_table"

        private fun pendingFile(context: Context) = context.getDatabasePath("${UserDatabase.NAME}.restore")

        private fun File.deleteSidecars() = listOf("-wal", "-shm", "-journal").forEach { File(path + it).delete() }

        fun applyPendingRestore(context: Context): Boolean {
            val pending = pendingFile(context)
            if (!pending.exists()) return false
            val file = context.getDatabasePath(UserDatabase.NAME)
            file.deleteSidecars()
            check(pending.renameTo(file)) { "Failed to replace database" }
            return true
        }
    }
}
