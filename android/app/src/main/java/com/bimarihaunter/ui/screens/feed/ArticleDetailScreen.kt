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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String? = null,
    onNavigateBack: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val articles by homeViewModel.newsArticles.collectAsState()
    val article = articles.find { it.id == articleId }

    Column(
        modifier = Modifier.fillMaxSize().background(MidnightBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
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

            if (article != null) {
                // Category + Source + Timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.category,
                        color = MidnightBlack,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFamily,
                        modifier = Modifier.clip(RoundedCornerShape(50.dp))
                            .background(LimeGreen).padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(article.source, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                    Spacer(Modifier.width(8.dp))
                    Text(article.timestamp, color = MediumGrey, fontSize = 12.sp, fontFamily = InterFamily)
                }

                Spacer(Modifier.height(12.dp))

                // Headline
                Text(
                    text = article.title,
                    color = OffWhite,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 32.sp
                )

                Spacer(Modifier.height(8.dp))

                // Author
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(CharcoalGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = article.source.take(2).uppercase()
                        Text(initials, color = LimeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("By ${article.source}", color = OffWhite, fontFamily = InterFamily, fontSize = 14.sp)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = CharcoalGrey)
                Spacer(Modifier.height(16.dp))

                // Body text
                Text(
                    text = article.content,
                    color = OffWhite,
                    fontFamily = InterFamily,
                    fontSize = 16.sp,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(24.dp))

                // Related Articles
                Text("Related Articles", color = OffWhite, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val related = articles.filter { it.id != article.id }.take(3)
                    items(related) { relArticle ->
                        Column(
                            modifier = Modifier.width(160.dp).clip(RoundedCornerShape(14.dp))
                                .background(CharcoalGrey).padding(12.dp)
                        ) {
                            Text(
                                text = relArticle.title,
                                color = OffWhite,
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("${relArticle.source} • ${relArticle.timestamp}", color = MediumGrey, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Text("Article not found", color = OffWhite, fontFamily = SpaceGroteskFamily)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
