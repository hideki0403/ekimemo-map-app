package dev.yukineko.ekimemo_map

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color as ComposeColor
import dev.yukineko.ekimemo_map.ui.navigation.AppNavHost
import dev.yukineko.ekimemo_map.ui.theme.EkimemoMapTheme
import dev.yukineko.ekimemo_map.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsFlow = (application as EkimemoMapApplication).settings.data
        setContent {
            val settings = settingsFlow.collectAsState(initial = null).value ?: return@setContent
            val darkTheme = settings.themeMode.isDark()
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            EkimemoMapTheme(
                darkTheme = darkTheme,
                useDynamicColor = settings.useDynamicColor,
                seedColor = ComposeColor(settings.themeSeedColor),
                font = settings.font,
            ) {
                AppNavHost()
            }
        }
    }
}
