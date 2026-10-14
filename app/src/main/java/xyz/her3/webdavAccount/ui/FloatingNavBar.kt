package xyz.her3.webdavAccount.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 浮动工具栏:叠在内容上层(Box overlay),常显静态,不随滚动收起。
 * 选中项带一圈莫奈 tonal 指示(secondaryContainer,去饱和)。
 */
@Composable
fun FloatingNavBar(
    selectedTab: Int,
    onListClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .width(116.dp)
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolbarIcon(
                        selected = selectedTab == 0,
                        onClick = onListClick,
                        icon = Icons.Filled.List,
                        description = "流水",
                    )
                    ToolbarIcon(
                        selected = selectedTab == 1,
                        onClick = onSettingsClick,
                        icon = Icons.Filled.Settings,
                        description = "设置",
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (pressed) 0.8f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh,
                ),
                label = "fabScale",
            )
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.scale(fabScale),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                interactionSource = interactionSource,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔", modifier = Modifier.size(28.dp))
            }
        }
    }
}

/** 工具栏图标:选中时带一圈 secondaryContainer 指示(莫奈 tonal,去饱和) */
@Composable
private fun ToolbarIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "indicator",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "iconTint",
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(bg, CircleShape),
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}
