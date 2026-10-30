package com.lukeneedham.lyrictutor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Every colour and typeface the app uses. Read them with `AppTheme.current`. No text styles: each composable sets its own size. */
@Immutable
data class AppThemeValues(
    /** The page. */
    val background: Color,
    val onBackground: Color,
    /** Cards and sheets, one step above the page. */
    val surface: Color,
    val onSurface: Color,
    /** Fields, chips and quiet buttons, one step above [surface]. */
    val surfaceVariant: Color,
    /** Secondary text. */
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    /** A soft tint of [primary]: the selected word, the form of a word. */
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    /** The second colour of a song's page (from its album art); the app's own accent elsewhere. */
    val secondary: Color,
    val onSecondary: Color,
    /** A soft tint of [secondary]: the selected word. */
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    /** Hairlines and the borders of fields. */
    val border: Color,
    val outline: Color,
    val error: Color,
    val warning: Color,
    /** The two ends of the gradient behind the logo and the first card of the home page. */
    val gradientStart: Color,
    val gradientEnd: Color,
    val fontFamily: FontFamily,
)

internal val LightAppThemeValues = AppThemeValues(
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF17161C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17161C),
    surfaceVariant = Color(0xFFECEAF6),
    onSurfaceVariant = Color(0xFF625F70),
    primary = Color(0xFF5546E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF2A1F9E),
    secondary = Color(0xFFC13FA6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8DCF0),
    onSecondaryContainer = Color(0xFF5A1E4C),
    border = Color(0xFFDDDAE8),
    outline = Color(0xFF8A8A94),
    error = Color(0xFFB3261E),
    warning = Color(0xFF8D6E00),
    gradientStart = Color(0xFF5B3FE8),
    gradientEnd = Color(0xFFC13FA6),
    fontFamily = FontFamily.Default,
)

internal val DarkAppThemeValues = AppThemeValues(
    background = Color(0xFF0E0D13),
    onBackground = Color(0xFFEAE7F0),
    surface = Color(0xFF1A1921),
    onSurface = Color(0xFFEAE7F0),
    surfaceVariant = Color(0xFF26242F),
    onSurfaceVariant = Color(0xFFA9A5B8),
    primary = Color(0xFFB9AEFF),
    onPrimary = Color(0xFF2A1F9E),
    primaryContainer = Color(0xFF3A2FA8),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFFE08BD0),
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF5A2350),
    onSecondaryContainer = Color(0xFFFBD9F2),
    border = Color(0xFF34313F),
    outline = Color(0xFF8E8E98),
    error = Color(0xFFF2B8B5),
    warning = Color(0xFFE6C34A),
    gradientStart = Color(0xFF4B33C9),
    gradientEnd = Color(0xFFA02E8A),
    fontFamily = FontFamily.Default,
)
