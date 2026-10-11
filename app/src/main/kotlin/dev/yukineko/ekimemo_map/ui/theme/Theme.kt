package dev.yukineko.ekimemo_map.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.materialkolor.rememberDynamicColorScheme
import dev.yukineko.ekimemo_map.R

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppFont(val googleFontName: String) {
    NOTO_SANS_JP("Noto Sans JP"),
    M_PLUS_1P("M PLUS 1p"),
    M_PLUS_ROUNDED_1C("M PLUS Rounded 1c"),
    BIZ_UD_GOTHIC("BIZ UDGothic"),
    KOSUGI_MARU("Kosugi Maru"),
    ZEN_MARU_GOTHIC("Zen Maru Gothic"),
}

val DefaultSeedColor = Color(0xFF2196F3)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun AppFont.toFontFamily(): FontFamily {
    val font = GoogleFont(googleFontName)
    return FontFamily(listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.Bold).map { Font(font, fontProvider, it) })
}

private fun Typography.withFontFamily(fontFamily: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)

@Composable
fun EkimemoMapTheme(
    darkTheme: Boolean,
    useDynamicColor: Boolean = false,
    seedColor: Color = DefaultSeedColor,
    font: AppFont = AppFont.NOTO_SANS_JP,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme)
    }
    val typography = remember(font) { Typography().withFontFamily(font.toFontFamily()) }

    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
