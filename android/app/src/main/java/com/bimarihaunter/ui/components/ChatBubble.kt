package com.bimarihaunter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.theme.*

@Composable
fun ChatBubble(
    message: String,
    isOutgoing: Boolean,
    senderName: String? = null,
    timestamp: String? = null,
    showAvatar: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // Sender name for incoming
        if (!isOutgoing && senderName != null) {
            Text(
                text = senderName,
                color = LimeGreen,
                fontSize = 11.sp,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(
                    start = if (showAvatar) 40.dp else 0.dp,
                    bottom = 2.dp
                )
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            // Avatar for incoming
            if (!isOutgoing && showAvatar) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CharcoalGrey),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (senderName?.firstOrNull() ?: 'A').toString(),
                        color = LimeGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isOutgoing) 20.dp else 4.dp,
                            bottomEnd = if (isOutgoing) 4.dp else 20.dp
                        )
                    )
                    .background(if (isOutgoing) LimeGreen else CharcoalGrey)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message,
                    color = if (isOutgoing) MidnightBlack else OffWhite,
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Timestamp
        if (timestamp != null) {
            Text(
                text = timestamp,
                color = MediumGrey,
                fontSize = 10.sp,
                fontFamily = InterFamily,
                modifier = Modifier.padding(
                    start = if (!isOutgoing && showAvatar) 40.dp else 0.dp,
                    top = 2.dp
                )
            )
        }
    }
}
