package com.nyaa.aniyaa.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val darkSchemes = listOf(
    DefaultDarkColorScheme,
    darkColorScheme(primary = NyaaBlue80, secondary = NyaaBlueGrey80, tertiary = NyaaTeal80),
    darkColorScheme(primary = Sakura80, secondary = SakuraGrey80, tertiary = SakuraPink80),
    darkColorScheme(primary = Matcha80, secondary = MatchaGrey80, tertiary = MatchaLight80),
    darkColorScheme(primary = Sunset80, secondary = SunsetGrey80, tertiary = SunsetLight80)
)

private val lightSchemes = listOf(
    DefaultLightColorScheme,
    lightColorScheme(primary = NyaaPrimary, secondary = NyaaSecondary, tertiary = NyaaTertiary),
    lightColorScheme(primary = APP_THEMES[2].primary, secondary = APP_THEMES[2].secondary, tertiary = APP_THEMES[2].tertiary),
    lightColorScheme(primary = APP_THEMES[3].primary, secondary = APP_THEMES[3].secondary, tertiary = APP_THEMES[3].tertiary),
    lightColorScheme(primary = APP_THEMES[4].primary, secondary = APP_THEMES[4].secondary, tertiary = APP_THEMES[4].tertiary)
)

@Composable
fun AniyaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val index = themeIndex.coerceIn(0, APP_THEMES.lastIndex)
    val colorScheme = when {
        themeIndex == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkSchemes[index]
        else -> lightSchemes[index]
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
