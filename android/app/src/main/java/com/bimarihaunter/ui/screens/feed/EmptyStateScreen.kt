package com.bimarihaunter.ui.screens.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.theme.*

@Composable
fun EmptyStateScreen(
    onClearFilters: () -> Unit = {},
    onBrowseAll: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MidnightBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)) {
            Image(
                painter = painterResource(id = R.drawable.ghost_sleep),
                contentDescription = "Empty State",
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Nothing here yet", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text("Try adjusting your filters or check back later.",
                color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            BimarihaunterButton("Clear Filters", onClick = onClearFilters)
            Spacer(Modifier.height(12.dp))
            BimarihaunterButton("Browse All News", onClick = onBrowseAll, isPrimary = false)
        }
    }
}
