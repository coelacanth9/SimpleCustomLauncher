package com.example.simplecustomlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.simplecustomlauncher.data.ThemeMode

// ===== ライトモード ColorScheme =====
private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueContainer,
    onPrimaryContainer = BluePrimary,

    secondary = OrangeSecondary,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OrangeSecondary,

    tertiary = GreenTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GreenContainer,
    onTertiaryContainer = GreenTertiary,

    error = RedError,
    onError = Color.White,
    errorContainer = RedContainer,
    onErrorContainer = RedError,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = DisabledColor
)

// ===== ダークモード ColorScheme =====
private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = BluePrimary,
    onPrimaryContainer = Color.White,

    secondary = OrangeSecondaryLight,
    onSecondary = Color.Black,
    secondaryContainer = OrangeSecondary,
    onSecondaryContainer = Color.White,  // 統一して白文字

    tertiary = GreenTertiaryLight,
    onTertiary = Color.Black,
    tertiaryContainer = GreenTertiary,
    onTertiaryContainer = Color.White,  // 統一して白文字

    error = RedErrorLight,
    onError = Color.Black,
    errorContainer = RedError,
    onErrorContainer = Color.White,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = LightOnSurfaceVariant
)

// ===== 拡張カラー（MaterialThemeにない色用）=====
data class ExtendedColors(
    val todayHighlight: Color,
    val editModeBorder: Color,
    val cardBackground: Color,
    val iconMemo: Color,
    val iconCalendar: Color,
    val iconDialer: Color,
    val iconAllApps: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        todayHighlight = TodayHighlight,
        editModeBorder = OrangeSecondary,
        cardBackground = LightSurface,
        iconMemo = IconMemoLight,
        iconCalendar = IconCalendarLight,
        iconDialer = IconDialerLight,
        iconAllApps = IconAllAppsLight
    )
}

private val LightExtendedColors = ExtendedColors(
    todayHighlight = TodayHighlight,
    editModeBorder = OrangeSecondary,
    cardBackground = Color.White,
    iconMemo = IconMemoLight,
    iconCalendar = IconCalendarLight,
    iconDialer = IconDialerLight,
    iconAllApps = IconAllAppsLight
)

private val DarkExtendedColors = ExtendedColors(
    todayHighlight = TodayHighlightDark,
    editModeBorder = OrangeSecondaryLight,
    cardBackground = DarkSurface,
    iconMemo = IconMemoDark,
    iconCalendar = IconCalendarDark,
    iconDialer = IconDialerDark,
    iconAllApps = IconAllAppsDark
)

// ===== テーマ適用 =====
@Composable
fun SimpleCustomLauncherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// 拡張カラーへのアクセス用
object AppTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
