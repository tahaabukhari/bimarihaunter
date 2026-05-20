package com.bimarihaunter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
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

data class NewsCardData(
    val id: String,
    val category: String,
    val source: String,
    val timestamp: String,
    val headline: String,
    val snippet: String,
    val location: String,
    val isBookmarked: Boolean = false
)

@Composable
fun NewsCard(
    data: NewsCardData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalGrey)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Category + Source + Timestamp row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.category,
                color = MidnightBlack,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(LimeGreen)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = data.source,
                color = MediumGrey,
                fontSize = 12.sp,
                fontFamily = InterFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = data.timestamp,
                color = MediumGrey,
                fontSize = 12.sp,
                fontFamily = InterFamily
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Headline
        Text(
            text = data.headline,
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Snippet
        Text(
            text = data.snippet,
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location + Bookmark row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MediumGrey,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = data.location,
                color = MediumGrey,
                fontSize = 12.sp,
                fontFamily = InterFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Bookmark,
                contentDescription = "Bookmark",
                tint = if (data.isBookmarked) LimeGreen else OffWhite,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
