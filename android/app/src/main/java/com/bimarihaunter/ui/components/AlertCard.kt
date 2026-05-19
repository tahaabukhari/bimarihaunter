package com.bimarihaunter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.theme.*

enum class AlertSeverity(val color: Color) {
    CRITICAL(EmberRed),
    WARNING(GoldWarning),
    INFO(TealInfo)
}

data class AlertCardData(
    val id: String,
    val severity: AlertSeverity,
    val heading: String,
    val subtitle: String,
    val timestamp: String,
    val isRead: Boolean = false
)

@Composable
fun AlertCard(
    data: AlertCardData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (data.isRead) 0.6f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalGrey)
            .clickable { onClick() }
    ) {
        // Left severity border
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(data.severity.color)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Severity icon
            Icon(
                imageVector = when (data.severity) {
                    AlertSeverity.CRITICAL -> Icons.Default.Error
                    AlertSeverity.WARNING -> Icons.Default.Warning
                    AlertSeverity.INFO -> Icons.Default.Info
                },
                contentDescription = null,
                tint = data.severity.color.copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.heading,
                    color = OffWhite.copy(alpha = contentAlpha),
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = data.subtitle,
                    color = MediumGrey.copy(alpha = contentAlpha),
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = data.timestamp,
                    color = MediumGrey.copy(alpha = contentAlpha),
                    fontFamily = InterFamily,
                    fontSize = 11.sp
                )
            }

            // Unread dot
            if (!data.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LimeGreen)
                )
            }
        }
    }
}
