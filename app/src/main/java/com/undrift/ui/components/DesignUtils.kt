package com.undrift.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.undrift.ui.components.SquircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.undrift.ui.theme.SurfaceColor
import com.undrift.ui.theme.BorderColor

/**
 * Applies a premium, clean card style following the 8-point grid
 * and subtle border guidelines.
 */
fun Modifier.premiumCard(
    cornerRadius: Dp = 24.dp, // 8-pt multiple
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    padding: Dp = 24.dp,
    elevation: Dp = 0.dp
): Modifier = composed {
    val actualBackground = backgroundColor ?: SurfaceColor
    val actualBorder = borderColor ?: BorderColor
    this.then(
        if (elevation > 0.dp) Modifier.shadow(elevation, SquircleShape(), ambientColor = Color.Black.copy(alpha = 0.05f)) else Modifier
    )
    .clip(SquircleShape())
    .background(actualBackground)
    .border(1.dp, actualBorder, SquircleShape())
    .padding(padding)
}
