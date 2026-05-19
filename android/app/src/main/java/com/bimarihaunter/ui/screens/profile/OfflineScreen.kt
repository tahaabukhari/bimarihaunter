package com.bimarihaunter.ui.screens.profile

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
fun OfflineScreen(
    onRetry: () -> Unit = {},
    onViewSaved: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MidnightBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ghost_sad),
                contentDescription = "Offline",
                modifier = Modifier.size(140.dp)
            )

            Spacer(Modifier.height(32.dp))

            Text("You are offline", color = OffWhite, fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold, fontSize = 28.sp)

            Spacer(Modifier.height(12.dp))

            Text(
                "Check your internet connection and try again.\nYour saved articles are still available.",
                color = MediumGrey, fontFamily = InterFamily, fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 22.sp
            )

            Spacer(Modifier.height(40.dp))

            BimarihaunterButton("Try Again", onClick = onRetry)

            Spacer(Modifier.height(16.dp))

            BimarihaunterButton("View Saved Articles", onClick = onViewSaved, isPrimary = false)
        }
    }
}
