package com.example.heicconverter.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue600   = Color(0xFF2563EB)
private val Blue50    = Color(0xFFEFF6FF)
private val Slate900  = Color(0xFF0F172A)
private val Slate600  = Color(0xFF475569)
private val Slate200  = Color(0xFFE2E8F0)
private val White     = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary         = Blue600,
    onPrimary       = White,
    primaryContainer    = Blue50,
    onPrimaryContainer  = Slate900,
    surface         = White,
    onSurface       = Slate900,
    onSurfaceVariant    = Slate600,
    outline         = Slate200,
    background      = Color(0xFFF8FAFC),
    onBackground    = Slate900,
)

@Composable
fun HeicConverterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography(),
        content     = content
    )
}