package com.klartext.simulator.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klartext.simulator.ui.theme.CncRapidOrange
import com.klartext.simulator.ui.theme.ConsoleColors

/**
 * Pixel-accurate industrial CNC keycap component that bypasses Material 3's 48.dp
 * minimum touch target expansion so compact hardware rows never overlap.
 */
@Composable
fun CncKeyCap(
    label: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 4.dp,
    height: Dp = 30.dp,
    fontSize: TextUnit = 10.sp,
    fontFamily: FontFamily = FontFamily.Default,
    fontWeight: FontWeight = FontWeight.Bold,
    icon: ImageVector? = null,
    iconSize: Dp = 14.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBg = if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
    val effectiveText = if (enabled) textColor else textColor.copy(alpha = 0.45f)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Box(
            modifier = modifier
                .height(height)
                .clip(shape)
                .background(effectiveBg, shape)
                .then(
                    if (borderColor != null) {
                        Modifier.border(borderWidth, borderColor, shape)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = textColor.copy(alpha = 0.25f)),
                    onClick = onClick
                )
                .padding(horizontal = 3.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null && label.isEmpty()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveText,
                    modifier = Modifier.size(iconSize)
                )
            } else if (icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = effectiveText,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = label,
                        fontSize = fontSize,
                        color = effectiveText,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = label,
                    fontSize = fontSize,
                    color = effectiveText,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Authentic TNC 640 Bottom Softkey Button with zero vertical overflow.
 */
@Composable
fun CncSoftkey(
    label: String,
    colors: ConsoleColors,
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    fontSize: TextUnit = 9.sp,
    isAlert: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        isAlert -> CncRapidOrange
        isSelected -> colors.headerBadgeBackground
        else -> colors.softkeyBackground
    }
    val fg = when {
        isAlert -> Color.White
        isSelected -> colors.headerBadgeTextColor
        else -> colors.softkeyText
    }
    val border = when {
        isAlert -> CncRapidOrange
        isSelected -> colors.headerBadgeBackground
        else -> colors.softkeyBorder
    }

    CncKeyCap(
        label = label,
        containerColor = bg,
        textColor = fg,
        borderColor = border,
        borderWidth = 0.8.dp,
        cornerRadius = 3.dp,
        height = height,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
        onClick = onClick
    )
}
