package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.MonoMetrics
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TacticalTopAppBar(
    title: String = "SHADOW_NET",
    onLeftIconClick: () -> Unit = {},
    onRightIconClick: () -> Unit = {},
    leftIcon: ImageVector = Icons.Default.Security,
    rightIcon: ImageVector = Icons.Default.SettingsInputAntenna
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BackgroundDeep)
            .border(
                width = 1.dp,
                color = OutlineVariant,
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onLeftIconClick() }
                    .testTag("top_bar_left_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leftIcon,
                    contentDescription = "Security",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.02).sp
                    ),
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PrimaryCobalt)
                    .clickable { onRightIconClick() }
                    .testTag("top_bar_right_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SN",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Speed),
    SERVERS("Servers", Icons.Default.Dns),
    TOOLKIT("Toolkit", Icons.Default.Terminal),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun TacticalBottomNavBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainer)
            .border(
                width = 1.dp,
                color = OutlineVariant,
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PrimaryContainer else Color.Transparent)
                                .padding(horizontal = 20.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) Color(0xFF001D36) else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    color: Color,
    value: Double,
    modifier: Modifier = Modifier,
    isDownload: Boolean = true
) {
    var points by remember(isDownload) { mutableStateOf(List(11) { 0f }) }
    LaunchedEffect(value) {
        // Display samples reported by VpnService instead of a decorative,
        // fabricated traffic graph.
        val normalized = (value / 200.0).toFloat().coerceIn(0f, 1f)
        points = points.drop(1) + normalized
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        val stepX = width / (points.size - 1)
        path.moveTo(0f, height - (points[0] * height * 0.8f))

        for (i in 1 until points.size) {
            val prevX = (i - 1) * stepX
            val prevY = height - (points[i - 1] * height * 0.8f)
            val currentX = i * stepX
            val currentY = height - (points[i] * height * 0.8f)
            val midX = (prevX + currentX) / 2
            path.cubicTo(midX, prevY, midX, currentY, currentX, currentY)
        }

        // Fill path
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.0f))
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

@Composable
fun TacticalGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = OutlineVariant,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainer)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        content()
    }
}

@Composable
fun PulsingStatusDot(
    color: Color = SecondaryEmerald,
    size: Int = 8
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size((size * 2).dp)
    ) {
        Box(
            modifier = Modifier
                .size((size * 1.8).dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha * 0.35f))
        )
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun FlagBadge(
    countryCode: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SurfaceBright)
            .border(1.dp, OutlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val (flagEmoji, _) = when (countryCode.uppercase()) {
            "DE" -> "🇩🇪" to "Germany"
            "TR" -> "🇹🇷" to "Turkey"
            "AE" -> "🇦🇪" to "UAE"
            "SG" -> "🇸🇬" to "Singapore"
            "NL" -> "🇳🇱" to "Netherlands"
            "US" -> "🇺🇸" to "USA"
            "GB" -> "🇬🇧" to "UK"
            else -> "🌐" to "Global"
        }
        Text(
            text = flagEmoji,
            fontSize = 18.sp
        )
    }
}
