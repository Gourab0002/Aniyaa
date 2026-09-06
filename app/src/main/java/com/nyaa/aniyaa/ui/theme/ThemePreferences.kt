package com.nyaa.aniyaa.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val usesDynamicColor: Boolean = false,
    val staticSchemeIndex: Int = 0
)

val APP_THEMES = listOf(
    AppTheme(
        name = "Material You",
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
        usesDynamicColor = true,
        staticSchemeIndex = 0
    ),
    AppTheme("Nyaa Blue", NyaaPrimary, NyaaSecondary, NyaaTertiary, staticSchemeIndex = 1),
    AppTheme("Sakura Pink", Color(0xFFBF3059), Color(0xFF8B1E51), Color(0xFFFF6B9D), staticSchemeIndex = 2),
    AppTheme("Matcha Green", Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF66BB6A), staticSchemeIndex = 3),
    AppTheme("Sunset Orange", Color(0xFFE64A19), Color(0xFFBF360C), Color(0xFFFF7043), staticSchemeIndex = 4),
    AppTheme("Aniyaa Purple", Purple40, PurpleGrey40, Pink40, staticSchemeIndex = 0)
)

fun themeAt(index: Int): AppTheme =
    APP_THEMES.getOrElse(index.coerceIn(0, APP_THEMES.lastIndex)) { APP_THEMES.first() }

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = prefs.getInt("theme_index", 0)
        set(value) {
            prefs.edit().putInt("theme_index", value).apply()
        }
}
