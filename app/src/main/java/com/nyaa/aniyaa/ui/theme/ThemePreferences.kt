package com.nyaa.aniyaa.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

val APP_THEMES = listOf(
    AppTheme("Default Purple", Purple40, PurpleGrey40, Pink40),
    AppTheme("Nyaa Blue", NyaaPrimary, NyaaSecondary, NyaaTertiary),
    AppTheme("Sakura Pink", Color(0xFFBF3059), Color(0xFF8B1E51), Color(0xFFFF6B9D)),
    AppTheme("Matcha Green", Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF66BB6A)),
    AppTheme("Sunset Orange", Color(0xFFE64A19), Color(0xFFBF360C), Color(0xFFFF7043))
)

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = prefs.getInt("theme_index", 0)
        set(value) {
            prefs.edit().putInt("theme_index", value).apply()
        }
}
