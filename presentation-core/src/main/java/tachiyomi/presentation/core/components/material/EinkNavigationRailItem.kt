import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip

@Composable
fun EinkNavigationRailItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)? = null,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundShape: Shape = MaterialTheme.shapes.small,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(backgroundShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // <-- no ripple
                role = Role.Tab,
                onClick = onClick
            )
            .background(Color.Transparent)
            .padding(vertical = 12.dp)
            .width(72.dp), // Typical NavigationRail width
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(
                LocalContentColor provides (if (selected) selectedColor else unselectedColor)
            ) {
                icon()

                if (label != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    label()
                }
            }
        }
    }
}
