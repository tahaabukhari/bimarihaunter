package com.bimarihaunter.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MidnightBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OffWhite)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Share, "Share", tint = OffWhite)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Bookmark, "Bookmark", tint = OffWhite)
            }
        }

        // Header image placeholder
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CharcoalGrey),
            contentAlignment = Alignment.Center
        ) {
            Text("📰", fontSize = 48.sp)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // Category + Source + Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Outbreak", color = MidnightBlack, fontSize = 10.sp,
                    fontWeight = FontWeight.Medium, fontFamily = InterFamily,
                    modifier = Modifier.clip(RoundedCornerShape(50.dp))
                        .background(LimeGreen).padding(horizontal = 10.dp, vertical = 3.dp))
                Spacer(Modifier.width(8.dp))
                Text("Dawn News", color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                Spacer(Modifier.width(8.dp))
                Text("2h ago", color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
            }

            Spacer(Modifier.height(12.dp))

            // Headline
            Text("Dengue Cases Surge in Lahore's Urban Centers as Monsoon Season Intensifies",
                color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp)

            Spacer(Modifier.height(8.dp))

            // Author
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(CharcoalGrey),
                    contentAlignment = Alignment.Center) {
                    Text("AK", color = LimeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text("By Ahmed Khan", color = OffWhite, fontFamily = InterFamily, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = CharcoalGrey)
            Spacer(Modifier.height(16.dp))

            // Body text
            Text(
                text = "Health authorities in Lahore have reported a significant surge in dengue cases as the monsoon season intensifies across Punjab. Over 340 new cases were confirmed in the past week alone, with multiple districts reporting clusters of infections.\n\nThe Provincial Health Department has activated emergency response teams and deployed additional fumigation squads across the most affected areas, including Gulberg, Model Town, and Defence Housing Authority.\n\nDr. Fatima Zahra, head of the Dengue Response Unit, stated that the current spike is directly linked to stagnant water pools formed after recent heavy rainfall. \"We are seeing a pattern similar to the 2019 outbreak, and we urge citizens to take preventive measures immediately,\" she said.\n\nThe government has also announced free dengue testing at all public hospitals and has set up dedicated dengue wards at Services Hospital, Mayo Hospital, and Jinnah Hospital Lahore.\n\nCitizens are advised to use mosquito repellent, wear full-sleeved clothing, and ensure no standing water collects around their homes. Any symptoms of high fever, body aches, and rash should be reported to the nearest health facility immediately.",
                color = OffWhite, fontFamily = InterFamily, fontSize = 16.sp, lineHeight = 26.sp
            )

            Spacer(Modifier.height(24.dp))

            // Related Articles
            Text("Related Articles", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf(
                    "Punjab Govt Allocates Emergency Funds",
                    "WHO Warns of Regional Dengue Spread",
                    "Prevention Tips for Monsoon Season"
                )) { title ->
                    Column(
                        modifier = Modifier.width(160.dp).clip(RoundedCornerShape(14.dp))
                            .background(CharcoalGrey).padding(12.dp)
                    ) {
                        Text(title, color = OffWhite, fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Dawn News • 3h ago", color = MediumGrey, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
