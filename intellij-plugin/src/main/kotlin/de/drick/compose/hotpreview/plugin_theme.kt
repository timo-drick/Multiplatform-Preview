package de.drick.compose.hotpreview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.jetbrains.compose.theme.intellij.SwingColor

@Composable
fun HotPreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit,
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    val swingColor = SwingColor()

    MaterialTheme(
        colorScheme = colors.copy(
            background = swingColor.background,
            onBackground = swingColor.onBackground,
            surface = swingColor.background,
            onSurface = swingColor.onBackground,
        ),
        content = content
    )
}