package com.bimarihaunter.ui.screens.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.db.OutbreakReportEntity
import com.bimarihaunter.repository.FeedRepository
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(feedRepository: FeedRepository) {
    var selectedTab by remember { mutableStateOf("Trends") }
    val tabs = listOf("Trends", "Diseases", "Locations")

    // Live data from Room (which is synced from Firestore /reports)
    val reports by feedRepository.getCachedFeed().collectAsState(initial = emptyList())
    var isRefreshing by remember { mutableStateOf(false) }

    // Derived analytics
    val analytics = remember(reports) { computeAnalytics(reports) }

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        BimarihaunterTopAppBar(
            title = "Insights",
            actions = {
                IconButton(onClick = { /* refresh handled by FeedViewModel */ }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = OffWhite)
                }
            }
        )

        // Tabs
        Row(modifier = Modifier.padding(horizontal = 20.dp)) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .clickable { selectedTab = tab },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab,
                        color = if (isSelected) OffWhite else MediumGrey,
                        fontFamily = InterFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    if (isSelected) {
                        Box(
                            Modifier
                                .width(30.dp)
                                .height(2.dp)
                                .background(LimeGreen, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(com.bimarihaunter.R.drawable.ghost_thinking),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(color = LimeGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Crunching live data…", color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp)
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Today's Story ─────────────────────────────────────────────────
            TodaysStoryCard(analytics)

            // ── Summary stats row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard(
                    icon = Icons.Default.Article,
                    value = "${analytics.totalReports}",
                    label = "Reports",
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    icon = Icons.Default.BugReport,
                    value = "${analytics.uniqueDiseases}",
                    label = "Diseases",
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    icon = Icons.Default.LocationOn,
                    value = "${analytics.uniqueLocations}",
                    label = "Locations",
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    icon = Icons.Default.TrendingUp,
                    value = "${analytics.highSeverity}",
                    label = "High Risk",
                    modifier = Modifier.weight(1f),
                    valueColor = EmberRed
                )
            }

            // ── Tab content ───────────────────────────────────────────────────
            when (selectedTab) {
                "Trends"    -> TrendsTab(analytics)
                "Diseases"  -> DiseasesTab(analytics)
                "Locations" -> LocationsTab(analytics)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ─────────────────────────── Today's Story ───────────────────────────────────

@Composable
private fun TodaysStoryCard(analytics: InsightAnalytics) {
    val topReport = analytics.topReport ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CharcoalGrey,
                        when (topReport.severity.lowercase()) {
                            "high", "critical" -> EmberRed.copy(alpha = 0.25f)
                            "medium"           -> GoldWarning.copy(alpha = 0.18f)
                            else               -> LimeGreen.copy(alpha = 0.12f)
                        }
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (topReport.severity.lowercase()) {
                            "high", "critical" -> EmberRed
                            "medium"           -> GoldWarning
                            else               -> LimeGreen
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "TODAY'S STORY",
                    color = MidnightBlack,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                topReport.severity.uppercase(),
                color = when (topReport.severity.lowercase()) {
                    "high", "critical" -> EmberRed
                    "medium"           -> GoldWarning
                    else               -> LimeGreen
                },
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            topReport.title,
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        val summaryText = topReport.summary.firstOrNull()?.take(200) ?: topReport.raw_text.take(200)
        if (summaryText.isNotBlank()) {
            Text(
                summaryText,
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                topReport.source.take(30),
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 11.sp
            )
            Text(
                analytics.topReportAge,
                color = LimeGreen,
                fontFamily = InterFamily,
                fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────── Trends Tab ──────────────────────────────────────

@Composable
private fun TrendsTab(analytics: InsightAnalytics) {
    // Severity distribution bar chart
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalGrey)
            .padding(20.dp)
    ) {
        Text(
            "Severity Distribution",
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            "Based on ${analytics.totalReports} live reports",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        val severityData = listOf(
            Triple("High", analytics.highSeverity, EmberRed),
            Triple("Medium", analytics.mediumSeverity, GoldWarning),
            Triple("Low", analytics.lowSeverity, LimeGreen)
        )
        val total = analytics.totalReports.coerceAtLeast(1)
        severityData.forEach { (label, count, color) ->
            val fraction = count.toFloat() / total
            val animFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(800),
                label = "bar_$label"
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = OffWhite,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    modifier = Modifier.width(52.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MidnightBlack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animFraction)
                            .clip(RoundedCornerShape(7.dp))
                            .background(color)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "$count",
                    color = color,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }

    // Confidence score distribution
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalGrey)
            .padding(20.dp)
    ) {
        Text(
            "AI Confidence Scores",
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Avg: ${String.format("%.0f", analytics.avgConfidence * 100)}% · " +
            "High (>80%): ${analytics.highConfidenceCount} reports",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        // Mini bar chart for confidence buckets
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            val buckets = analytics.confidenceBuckets
            val maxVal = buckets.maxOrNull()?.coerceAtLeast(1) ?: 1
            val barW = size.width / (buckets.size * 1.5f)
            val gap = barW * 0.5f
            buckets.forEachIndexed { i, v ->
                val x = (barW + gap) * i
                val barH = size.height * (v.toFloat() / maxVal)
                drawRoundRect(
                    LimeGreen.copy(alpha = 0.6f + 0.4f * (i.toFloat() / buckets.size)),
                    Offset(x, size.height - barH),
                    Size(barW, barH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("0-20%", "20-40%", "40-60%", "60-80%", "80-100%").forEach {
                Text(it, color = MediumGrey, fontSize = 9.sp, fontFamily = InterFamily)
            }
        }
    }
}

// ─────────────────────────── Diseases Tab ────────────────────────────────────

@Composable
private fun DiseasesTab(analytics: InsightAnalytics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalGrey)
            .padding(20.dp)
    ) {
        Text(
            "Top Diseases",
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            "Ranked by report count",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(14.dp))

        val colors = listOf(EmberRed, GoldWarning, TealInfo, LimeGreen, MediumGrey)
        analytics.topDiseases.forEachIndexed { i, (disease, count) ->
            val color = colors.getOrElse(i) { MediumGrey }
            val fraction = count.toFloat() / analytics.totalReports.coerceAtLeast(1)
            val animFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(900, delayMillis = i * 80),
                label = "disease_$i"
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${i + 1}",
                    color = MediumGrey,
                    fontSize = 12.sp,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(
                    disease.replaceFirstChar { it.uppercase() },
                    color = OffWhite,
                    fontSize = 14.sp,
                    fontFamily = InterFamily,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MidnightBlack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animFraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "$count",
                    color = MediumGrey,
                    fontSize = 13.sp,
                    fontFamily = InterFamily,
                    modifier = Modifier.width(24.dp)
                )
            }
            if (i < analytics.topDiseases.size - 1) {
                HorizontalDivider(color = MidnightBlack.copy(alpha = 0.4f))
            }
        }
    }
}

// ─────────────────────────── Locations Tab ───────────────────────────────────

@Composable
private fun LocationsTab(analytics: InsightAnalytics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalGrey)
            .padding(20.dp)
    ) {
        Text(
            "Affected Locations",
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            "${analytics.uniqueLocations} unique locations",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(14.dp))

        analytics.topLocations.forEachIndexed { i, (location, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = LimeGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    location,
                    color = OffWhite,
                    fontSize = 14.sp,
                    fontFamily = InterFamily,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$count reports",
                    color = MediumGrey,
                    fontSize = 12.sp,
                    fontFamily = InterFamily
                )
            }
            if (i < analytics.topLocations.size - 1) {
                HorizontalDivider(color = MidnightBlack.copy(alpha = 0.4f))
            }
        }
    }
}

// ─────────────────────────── Shared Composables ──────────────────────────────

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OffWhite
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CharcoalGrey)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = LimeGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, color = valueColor, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = MediumGrey, fontFamily = InterFamily, fontSize = 10.sp)
    }
}

// ─────────────────────────── Analytics Engine ────────────────────────────────

private data class InsightAnalytics(
    val totalReports: Int,
    val uniqueDiseases: Int,
    val uniqueLocations: Int,
    val highSeverity: Int,
    val mediumSeverity: Int,
    val lowSeverity: Int,
    val avgConfidence: Double,
    val highConfidenceCount: Int,
    val confidenceBuckets: List<Int>,   // 5 buckets: 0-20, 20-40, 40-60, 60-80, 80-100
    val topDiseases: List<Pair<String, Int>>,
    val topLocations: List<Pair<String, Int>>,
    val topReport: OutbreakReportEntity?,
    val topReportAge: String
)

private fun computeAnalytics(reports: List<OutbreakReportEntity>): InsightAnalytics {
    if (reports.isEmpty()) return InsightAnalytics(
        0, 0, 0, 0, 0, 0, 0.0, 0,
        listOf(0, 0, 0, 0, 0),
        emptyList(), emptyList(), null, ""
    )

    val totalReports = reports.size
    val uniqueDiseases = reports.map { it.disease.lowercase().trim() }.filter { it.isNotBlank() }.toSet().size
    val uniqueLocations = reports.flatMap { it.locations }.map { it.trim() }.filter { it.isNotBlank() }.toSet().size
    val highSeverity = reports.count { it.severity.lowercase() in listOf("high", "critical") }
    val mediumSeverity = reports.count { it.severity.lowercase() == "medium" }
    val lowSeverity = reports.count { it.severity.lowercase() in listOf("low", "minimal") }
    val avgConfidence = reports.map { it.confidence_score }.average().takeIf { !it.isNaN() } ?: 0.0
    val highConfidenceCount = reports.count { it.confidence_score >= 0.8 }

    // Confidence buckets: 0-0.2, 0.2-0.4, 0.4-0.6, 0.6-0.8, 0.8-1.0
    val buckets = (0 until 5).map { b ->
        reports.count { it.confidence_score >= b * 0.2 && it.confidence_score < (b + 1) * 0.2 }
    }

    val topDiseases = reports
        .groupBy { it.disease.lowercase().trim().ifBlank { "unknown" } }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(8)

    val topLocations = reports
        .flatMap { it.locations }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .groupBy { it }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(10)

    // Top report: highest confidence + high severity, most recent
    val topReport = reports
        .filter { it.severity.lowercase() in listOf("high", "critical", "medium") }
        .maxByOrNull { it.confidence_score * 2 + (if (it.severity.lowercase() == "high") 1.0 else 0.0) }
        ?: reports.firstOrNull()

    val topReportAge = topReport?.let { r ->
        val raw = r.scraped_at.ifBlank { r.published_at }
        try {
            val formats = listOf(
                "MMM d, yyyy 'at' h:mm:ss a z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            )
            var parsed: Date? = null
            for (fmt in formats) {
                try { parsed = SimpleDateFormat(fmt, Locale.ENGLISH).parse(raw); break } catch (_: Exception) {}
            }
            if (parsed != null) {
                val diff = System.currentTimeMillis() - parsed.time
                when {
                    diff < 3_600_000L   -> "${diff / 60_000}m ago"
                    diff < 86_400_000L  -> "${diff / 3_600_000}h ago"
                    else                -> "${diff / 86_400_000}d ago"
                }
            } else ""
        } catch (_: Exception) { "" }
    } ?: ""

    return InsightAnalytics(
        totalReports, uniqueDiseases, uniqueLocations,
        highSeverity, mediumSeverity, lowSeverity,
        avgConfidence, highConfidenceCount, buckets,
        topDiseases, topLocations, topReport, topReportAge
    )
}
