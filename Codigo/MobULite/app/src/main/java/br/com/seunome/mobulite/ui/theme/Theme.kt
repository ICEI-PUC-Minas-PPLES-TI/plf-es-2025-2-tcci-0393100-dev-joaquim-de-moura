package br.com.seunome.mobulite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = AppPurple,
    onPrimary            = Color.White,
    primaryContainer     = AppPurpleLight,
    onPrimaryContainer   = AppVioletDarker,
    secondary            = Slate600,
    onSecondary          = Color.White,
    secondaryContainer   = Slate100,
    onSecondaryContainer = Slate800,
    tertiary             = AppLilac,
    onTertiary           = Color.White,
    tertiaryContainer    = AppLilacSoft,
    onTertiaryContainer  = AppVioletDark,
    background           = Slate50,
    onBackground         = Slate900,
    surface              = Color.White,
    onSurface            = Slate900,
    surfaceVariant       = Slate100,
    onSurfaceVariant     = Slate500,
    outline              = Slate200,
    outlineVariant       = Slate100,
    error                = AppRed,
    onError              = Color.White,
    errorContainer       = AppRedLight,
    onErrorContainer     = AppRedDarker,
    scrim                = Color(0xCC000000)
)

@Composable
fun MobULiteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
