package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.LocalGroupHeaderStyle
import org.jetbrains.jewel.ui.icons.AllIconsKeys


@HotPreview(widthDp = 300)
@HotPreview(widthDp = 300, darkMode = false)
@Composable
private fun PreviewConfigSection() {
    SelfPreviewTheme {
        FoldableSection(modifier = Modifier, label = "Test Section", isInitiallyFolded = false) {
            Text("Config value")
        }
    }
}
@HotPreview(widthDp = 300)
@HotPreview(widthDp = 300, darkMode = false)
@Composable
private fun PreviewConfigSectionFolded() {
    SelfPreviewTheme {
        FoldableSection(modifier = Modifier, label = "Test Section", isInitiallyFolded = true) {
            Text("Config value")
        }
    }
}

@Composable
fun FoldableSection(
    label: String,
    isInitiallyFolded: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showSection by remember { mutableStateOf(isInitiallyFolded.not()) }
    val animatedRotation = animateFloatAsState(if (showSection) 0f else -90f)
    val style = LocalGroupHeaderStyle.current

    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { showSection = showSection.not() },
        ) {
            Icon(
                modifier = Modifier.rotate(animatedRotation.value),
                key = AllIconsKeys.General.ArrowDown,
                contentDescription = "Fold"
            )
            //Divider(Orientation.Horizontal, Modifier.width(8.dp), thickness = 2.dp)
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            //Divider(Orientation.Horizontal, Modifier.weight(1f), thickness = 2.dp)
        }
        AnimatedVisibility(
            visible = showSection,
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Divider(
                    Orientation.Vertical,
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    color = style.colors.divider,
                    thickness = style.metrics.dividerThickness,
                )
                Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}