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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerLowest = Color(0xFF0F0D13),
    background = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFF3EDF7),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    background = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val darkSchemes = listOf(
    DefaultDarkColorScheme,
    darkColorScheme(
        primary = NyaaBlue80,
        secondary = NyaaBlueGrey80,
        tertiary = NyaaTeal80,
        surface = Color(0xFF0F1419),
        surfaceVariant = Color(0xFF151C24),
        surfaceContainer = Color(0xFF1A2330),
        surfaceContainerHigh = Color(0xFF1F2A38),
        surfaceContainerHighest = Color(0xFF253140),
        surfaceContainerLow = Color(0xFF151C24),
        surfaceContainerLowest = Color(0xFF0A0F14),
        background = Color(0xFF0F1419),
        onSurface = Color(0xFFE1E6ED),
        onSurfaceVariant = Color(0xFFB8C4D0),
        outline = Color(0xFF8899AA),
        outlineVariant = Color(0xFF3D4D5C)
    ),
    darkColorScheme(
        primary = Sakura80,
        secondary = SakuraGrey80,
        tertiary = SakuraPink80,
        surface = Color(0xFF1A1015),
        surfaceVariant = Color(0xFF22151C),
        surfaceContainer = Color(0xFF2A1B24),
        surfaceContainerHigh = Color(0xFF33222D),
        surfaceContainerHighest = Color(0xFF3C2936),
        surfaceContainerLow = Color(0xFF22151C),
        surfaceContainerLowest = Color(0xFF140C11),
        background = Color(0xFF1A1015),
        onSurface = Color(0xFFF0DEE6),
        onSurfaceVariant = Color(0xFFD4B8C4),
        outline = Color(0xFFA48898),
        outlineVariant = Color(0xFF534050)
    ),
    darkColorScheme(
        primary = Matcha80,
        secondary = MatchaGrey80,
        tertiary = MatchaLight80,
        surface = Color(0xFF0F1610),
        surfaceVariant = Color(0xFF151E16),
        surfaceContainer = Color(0xFF1A261C),
        surfaceContainerHigh = Color(0xFF1F2E22),
        surfaceContainerHighest = Color(0xFF253628),
        surfaceContainerLow = Color(0xFF151E16),
        surfaceContainerLowest = Color(0xFF0A110B),
        background = Color(0xFF0F1610),
        onSurface = Color(0xFFDCEBDE),
        onSurfaceVariant = Color(0xFFB4CCBA),
        outline = Color(0xFF85A08C),
        outlineVariant = Color(0xFF3D4F40)
    ),
    darkColorScheme(
        primary = Sunset80,
        secondary = SunsetGrey80,
        tertiary = SunsetLight80,
        surface = Color(0xFF1A120E),
        surfaceVariant = Color(0xFF221814),
        surfaceContainer = Color(0xFF2A1E1A),
        surfaceContainerHigh = Color(0xFF332520),
        surfaceContainerHighest = Color(0xFF3C2D27),
        surfaceContainerLow = Color(0xFF221814),
        surfaceContainerLowest = Color(0xFF140D0A),
        background = Color(0xFF1A120E),
        onSurface = Color(0xFFF0E0D8),
        onSurfaceVariant = Color(0xFFD4BBB0),
        outline = Color(0xFFA49088),
        outlineVariant = Color(0xFF534740)
    )
)

private val lightSchemes = listOf(
    DefaultLightColorScheme,
    lightColorScheme(
        primary = NyaaPrimary,
        secondary = NyaaSecondary,
        tertiary = NyaaTertiary,
        surface = Color(0xFFF8FAFF),
        surfaceVariant = Color(0xFFEDF2FA),
        surfaceContainer = Color(0xFFEDF2FA),
        surfaceContainerHigh = Color(0xFFE2EAF5),
        surfaceContainerHighest = Color(0xFFD8E4F2),
        background = Color(0xFFF8FAFF)
    ),
    lightColorScheme(
        primary = APP_THEMES[2].primary,
        secondary = APP_THEMES[2].secondary,
        tertiary = APP_THEMES[2].tertiary,
        surface = Color(0xFFFFF8FA),
        surfaceVariant = Color(0xFFFAEDF2),
        surfaceContainer = Color(0xFFFAEDF2),
        surfaceContainerHigh = Color(0xFFF5E2EA),
        surfaceContainerHighest = Color(0xFFF2D8E4),
        background = Color(0xFFFFF8FA)
    ),
    lightColorScheme(
        primary = APP_THEMES[3].primary,
        secondary = APP_THEMES[3].secondary,
        tertiary = APP_THEMES[3].tertiary,
        surface = Color(0xFFF8FFF8),
        surfaceVariant = Color(0xFFEDFAED),
        surfaceContainer = Color(0xFFEDFAED),
        surfaceContainerHigh = Color(0xFFE2F5E2),
        surfaceContainerHighest = Color(0xFFD8F2D8),
        background = Color(0xFFF8FFF8)
    ),
    lightColorScheme(
        primary = APP_THEMES[4].primary,
        secondary = APP_THEMES[4].secondary,
        tertiary = APP_THEMES[4].tertiary,
        surface = Color(0xFFFFFAF8),
        surfaceVariant = Color(0xFFFAF0ED),
        surfaceContainer = Color(0xFFFAF0ED),
        surfaceContainerHigh = Color(0xFFF5E6E2),
        surfaceContainerHighest = Color(0xFFF2DDD8),
        background = Color(0xFFFFFAF8)
    )
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
