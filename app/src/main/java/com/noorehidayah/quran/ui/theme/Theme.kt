package com.noorehidayah.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF0D6E52)
val EmeraldDark = Color(0xFF084B38)
val WarmGold = Color(0xFFC89B3C)
val SoftIvory = Color(0xFFFAF8F4)
val CardLight = Color(0xFFFFFFFF)
val NightBg = Color(0xFF0F1715)
val NightCard = Color(0xFF182421)

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFE6),
    onPrimaryContainer = Color(0xFF052E22),
    secondary = WarmGold,
    onSecondary = Color(0xFF1E1706),
    background = SoftIvory,
    onBackground = Color(0xFF1A221F),
    surface = CardLight,
    onSurface = Color(0xFF1A221F),
    surfaceVariant = Color(0xFFEDF3F0),
    onSurfaceVariant = Color(0xFF42524C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5AD0A8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0D523D),
    onPrimaryContainer = Color(0xFFD6EFE6),
    secondary = Color(0xFFE5BD66),
    onSecondary = Color(0xFF2B2004),
    background = NightBg,
    onBackground = Color(0xFFE7EFEA),
    surface = NightCard,
    onSurface = Color(0xFFE7EFEA),
    surfaceVariant = Color(0xFF21312D),
    onSurfaceVariant = Color(0xFFB5C7C0)
)

@Composable
fun NoorHidayahTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
