package com.bimarihaunter.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.components.BimarihaunterTopAppBar
import com.bimarihaunter.ui.theme.*

@Composable
fun InsightsScreen() {
    var selectedTab by remember { mutableStateOf("Trends") }
    val tabs = listOf("Trends", "Costs", "Severity")

    Column(modifier = Modifier.fillMaxSize().background(MidnightBlack)) {
        BimarihaunterTopAppBar(
            title = "Insights",
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.CalendarMonth, "Calendar", tint = OffWhite)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Line chart card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CharcoalGrey)
                    .padding(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Disease Trends", color = OffWhite, fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Last 7 days", color = MediumGrey, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val points = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, 0.75f)
                    val w = size.width
                    val h = size.height
                    val path = Path()
                    points.forEachIndexed { i, v ->
                        val x = w * i / (points.size - 1)
                        val y = h * (1 - v)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, LimeGreen.copy(alpha = 0.8f), style = Stroke(width = 3f))
                    points.forEachIndexed { i, v ->
                        drawCircle(LimeGreen, 5f, Offset(w * i / (points.size - 1), h * (1 - v)))
                    }
                }
            }

            // Top Outbreaks card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CharcoalGrey)
                    .padding(20.dp)
            ) {
                Text("Top Outbreaks This Week", color = OffWhite, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                val outbreaks = listOf(
                    Triple("Dengue", "1,240 cases", EmberRed),
                    Triple("Malaria", "856 cases", GoldWarning),
                    Triple("Cholera", "432 cases", TealInfo),
                    Triple("Typhoid", "198 cases", MediumGrey)
                )
                outbreaks.forEachIndexed { i, (disease, cases, color) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${i + 1}", color = MediumGrey, fontSize = 14.sp,
                            fontFamily = InterFamily, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(disease, color = OffWhite, fontSize = 14.sp, fontFamily = InterFamily,
                            modifier = Modifier.weight(1f))
                        Text(cases, color = MediumGrey, fontSize = 13.sp, fontFamily = InterFamily)
                    }
                    if (i < outbreaks.size - 1) {
                        HorizontalDivider(color = MidnightBlack.copy(alpha = 0.5f))
                    }
                }
            }

            // Bar chart card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CharcoalGrey)
                    .padding(20.dp)
            ) {
                Text("Pharmacy Price Index", color = OffWhite, fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val bars = listOf(0.6f, 0.8f, 0.45f, 0.9f)
                    val barW = size.width / (bars.size * 2f)
                    bars.forEachIndexed { i, v ->
                        val x = barW * (i * 2 + 0.5f)
                        val barH = size.height * v
                        drawRoundRect(
                            LimeGreen,
                            Offset(x, size.height - barH),
                            Size(barW, barH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Panadol", "ORS", "Net", "Dettol").forEach {
                        Text(it, color = MediumGrey, fontSize = 10.sp, fontFamily = InterFamily)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
