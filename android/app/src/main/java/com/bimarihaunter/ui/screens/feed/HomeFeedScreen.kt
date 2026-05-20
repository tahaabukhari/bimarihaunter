package com.bimarihaunter.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.data.model.NewsArticle
import com.bimarihaunter.ui.components.*
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.HomeViewModel

@Composable
fun HomeFeedScreen(
    onNavigateToArticle: (String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    var selectedChip by remember { mutableStateOf("All") }
    val chips = listOf("All", "Outbreaks", "Disasters", "Research", "Local", "Pharmacy")
    
    val articles by homeViewModel.newsArticles.collectAsState()
    
    val filteredArticles = remember(articles, selectedChip) {
        if (selectedChip == "All") {
            articles
        } else {
            articles.filter { it.category.equals(selectedChip, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .statusBarsPadding()
    ) {
        // Custom Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LimeGreen)) { append("bimari") }
                    withStyle(SpanStyle(color = OffWhite)) { append("haunter") }
                },
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNavigateToAlerts) {
                Icon(Icons.Default.Notifications, "Alerts", tint = OffWhite)
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(CharcoalGrey),
                contentAlignment = Alignment.Center
            ) {
                Text("US", color = LimeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CharcoalGrey)
                .clickable { onNavigateToSearch() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = MediumGrey, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Search outbreaks, diseases...", color = MediumGrey, fontSize = 14.sp,
                fontFamily = InterFamily)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Tune, "Filter", tint = LimeGreen, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips
        FilterChipRow(
            chips = chips, selectedChip = selectedChip,
            onChipSelected = { selectedChip = it },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Breaking news banner
            val criticalArticle = articles.firstOrNull { it.severity == "CRITICAL" }
            if (criticalArticle != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalGrey)
                            .clickable { onNavigateToArticle(criticalArticle.id) }
                    ) {
                        Box(Modifier.width(4.dp).height(80.dp).background(LimeGreen))
                        Column(Modifier.padding(16.dp)) {
                            Text("BREAKING", color = LimeGreen, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold, fontFamily = InterFamily,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(LimeGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(criticalArticle.title,
                                color = OffWhite, fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(criticalArticle.timestamp, color = MediumGrey, fontSize = 11.sp)
                        }
                    }
                }
            }

            // News cards
            items(filteredArticles) { news ->
                NewsCard(
                    data = NewsCardData(
                        id = news.id,
                        category = news.category,
                        source = news.source,
                        timestamp = news.timestamp,
                        headline = news.title,
                        snippet = news.content,
                        location = news.location
                    ),
                    onClick = { onNavigateToArticle(news.id) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
