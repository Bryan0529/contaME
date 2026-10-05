package com.uma.contame.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Esquema de colores para el tema oscuro de la aplicación.
 */
private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Slate950,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Slate950,
    secondaryContainer = Slate700,
    onSecondaryContainer = Color.White,
    tertiary = IndigoGoal,
    onTertiary = Color.White,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    error = CoralExpense,
    onError = Color.White,
    outline = Slate700,
    outlineVariant = Slate850
)

/**
 * Esquema de colores para el tema claro de la aplicación.
 */
private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = BlueIncomeContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = IndigoGoal,
    onSecondary = Color.White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate900,
    tertiary = CyanAccent,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = CoralExpense,
    onError = Color.White,
    outline = Slate200,
    outlineVariant = Slate100
)

/**
 * Composable de tema principal para la aplicación `contaME`.
 *
 * @param darkTheme Determina si se aplica el tema oscuro.
 * @param dynamicColor Si es verdadero (desactivado por defecto para mantener la identidad visual financiera),
 * utiliza los colores dinámicos de Android 12+.
 * @param content Contenido de la UI al que se le aplicará el tema.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
