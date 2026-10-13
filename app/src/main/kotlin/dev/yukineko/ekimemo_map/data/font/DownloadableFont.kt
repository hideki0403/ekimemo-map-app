package dev.yukineko.ekimemo_map.data.font

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class DownloadableFont(val hash: String, override val weight: FontWeight) :
    AndroidFont(FontLoadingStrategy.Async, FontFileLoader, FontVariation.Settings()) {
    override val style = FontStyle.Normal
}

private object FontFileLoader : AndroidFont.TypefaceLoader {
    private val logger = Logger("FontFileLoader")
    private val client = OkHttpClient()
    private val mutex = Mutex()

    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? = null

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        val hash = (font as DownloadableFont).hash
        val file = File(context.filesDir, "fonts/$hash.ttf")
        return runCatching {
            mutex.withLock { if (!file.exists()) download(hash, file) }
            Typeface.createFromFile(file)
        }.onFailure { logger.error("Failed to load font $hash", it) }.getOrNull()
    }

    private suspend fun download(hash: String, file: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://fonts.gstatic.com/s/a/$hash.ttf").build()
        val bytes = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.bytes()
        }
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (actual != hash) throw IOException("Hash mismatch: $actual")
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(file)) throw IOException("Failed to save ${file.name}")
    }
}
