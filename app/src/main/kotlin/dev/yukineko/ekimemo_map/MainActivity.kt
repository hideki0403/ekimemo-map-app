package dev.yukineko.ekimemo_map

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import dev.yukineko.ekimemo_map.ui.navigation.AppNavHost
import dev.yukineko.ekimemo_map.ui.theme.EkimemoMapTheme
import dev.yukineko.ekimemo_map.ui.theme.ThemeMode
import dev.yukineko.ekimemo_map.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = ThemeMode.SYSTEM.isDark()
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            EkimemoMapTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
