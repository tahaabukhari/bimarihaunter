package com.bimarihaunter.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.R
import com.bimarihaunter.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    // Fade-in animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        if (isLoggedIn) onNavigateToHome() else onNavigateToOnboarding()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ghost mascot
            Image(
                painter = painterResource(id = R.drawable.ghost_happy),
                contentDescription = "Bimarihaunter Ghost",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // App name
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LimeGreen)) {
                        append("bimari")
                    }
                    withStyle(SpanStyle(color = OffWhite)) {
                        append("haunter")
                    }
                },
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "stay informed. stay safe.",
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 14.sp
            )
        }

        // Bottom accent line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .width(80.dp)
                .height(3.dp)
                .background(LimeGreen)
        )
    }
}
