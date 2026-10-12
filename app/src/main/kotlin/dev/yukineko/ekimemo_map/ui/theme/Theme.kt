package dev.yukineko.ekimemo_map.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import dev.yukineko.ekimemo_map.data.font.DownloadableFont

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppFont(val googleFontName: String, val files: Map<FontWeight, String>) {
    NOTO_SANS_JP(
        "Noto Sans JP",
        mapOf(
            FontWeight.Normal to "209c70f533554d512ef0a417b70dfe2997aeec080d2fe41695c55b361643f9ba",
            FontWeight.Medium to "c5233cdc5a2901be5503f0d95ff48b4b5170afff6a39f95a076520cb73f17860",
            FontWeight.Bold to "eee16e4913b766be0eb7b9a02cd6ec3daf27292ca0ddf194cae01279aac1c9d0",
        ),
    ),
    M_PLUS_1P(
        "M PLUS 1p",
        mapOf(
            FontWeight.Normal to "e97abdba17f7de7b88b860a1dd16e7d03e3bb4c75982d71471f7f8efd27d9197",
            FontWeight.Medium to "d47efc2873264bb6b404aefe8a48a18f6496c0f8c69a038923ff983f7b6f4207",
            FontWeight.Bold to "ddfe21a5f8a9dc313a5c3f56ca83843fd87454d8f3890b10dafd5afd4db198a6",
        ),
    ),
    M_PLUS_ROUNDED_1C(
        "M PLUS Rounded 1c",
        mapOf(
            FontWeight.Normal to "e30fa1b6bfbb0a32efa620039763ecf4155c425078e5946aabab3e869776b5c3",
            FontWeight.Medium to "0a1d8fc54e3d396c30c265597945feb97bf3a2b90c6ff8c6bda98e433cd54825",
            FontWeight.Bold to "329a8ed11bfe4054de884fcf8692759a19bde914a47d470a0b93fc4ed45d2d1c",
        ),
    ),
    BIZ_UD_GOTHIC(
        "BIZ UDGothic",
        mapOf(
            FontWeight.Normal to "f0e93ce9e3edfc58d2874908e14b36b77a6f6dde51e7e1d919b99b27018b1621",
            FontWeight.Bold to "aa66342f3412d3890b66d95543e0d0fa9fac466c227e596b9a36cf3ec1267387",
        ),
    ),
    KOSUGI_MARU(
        "Kosugi Maru",
        mapOf(
            FontWeight.Normal to "1a90c857da87ed71b82528569fe9da02b7743228a973168640a523000aadcc6f",
        ),
    ),
    ZEN_MARU_GOTHIC(
        "Zen Maru Gothic",
        mapOf(
            FontWeight.Normal to "189be41b75ca817dcf5b40dcbb61544c9409522306d6e8a4b34a45544e23bbf9",
            FontWeight.Medium to "264a433ac84944fd11a8220a4a3af42442d3d49eb73b3314d67dc04f5c17c171",
            FontWeight.Bold to "68fb56f30194e4d394b7d8f6f7833e1837a9ea6d2b1a1121ccca01821ed7fd29",
        ),
    ),
}

val DefaultSeedColor = Color(0xFF2196F3)

val ColorScheme.isDark: Boolean get() = surface.luminance() < 0.5f

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private fun AppFont.toFontFamily() = FontFamily(files.map { (weight, hash) -> DownloadableFont(hash, weight) })

@Composable
fun EkimemoMapTheme(
    darkTheme: Boolean,
    useDynamicColor: Boolean = true,
    seedColor: Color = DefaultSeedColor,
    font: AppFont = AppFont.NOTO_SANS_JP,
    content: @Composable () -> Unit,
) {
    val baseColorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme, specVersion = ColorSpec.SpecVersion.SPEC_2025)
    }
    val colorScheme = baseColorScheme.copy(background = baseColorScheme.surfaceContainer)
    val typography = remember(font) { Typography(fontFamily = font.toFontFamily()) }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content = content,
    )
}
