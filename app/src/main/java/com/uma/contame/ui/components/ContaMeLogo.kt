package com.uma.contame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Official contaME Logo icon matching contaME_opcion1.png:
 * Dark rounded square icon, vivid green "C", and centered white dot.
 */
@Composable
fun ContaMeIcon(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val strokeWidth = this.size.width * 0.22f
            val arcSize = Size(
                this.size.width - strokeWidth,
                this.size.height - strokeWidth
            )
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Draw emerald green "C" arc
            drawArc(
                color = Color(0xFF10B981),
                startAngle = 40f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw white circular dot on the right center opening of the C
            val dotRadius = this.size.width * 0.16f
            val dotCenter = Offset(
                x = this.size.width * 0.58f,
                y = this.size.height * 0.50f
            )
            drawCircle(
                color = Color.White,
                radius = dotRadius,
                center = dotCenter
            )
        }
    }
}

/**
 * Full official contaME Logo banner matching contaME_opcion1.png:
 * [Icon] + "conta" (bold) + "ME" (emerald green)
 */
@Composable
fun ContaMeLogo(
    iconSize: Dp = 38.dp,
    fontSize: Float = 22f,
    showWordmark: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        ContaMeIcon(size = iconSize)

        if (showWordmark) {
            Spacer(modifier = Modifier.width(iconSize * 0.28f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "conta",
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "ME",
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981),
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}
