package com.bimarihaunter.ui.screens.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.ghost_alert,
            title = "Track Outbreaks",
            description = "Know what's spreading in your area before it reaches you."
        ),
        OnboardingPage(
            imageRes = R.drawable.ghost_waving,
            title = "Stay Alert",
            description = "Real-time notifications for health and disaster alerts near you."
        ),
        OnboardingPage(
            imageRes = R.drawable.ghost_happy,
            title = "Connect & Discuss",
            description = "Join community groups and talk about what matters."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        // Skip button
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = "Skip",
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = pages[page].imageRes),
                        contentDescription = pages[page].title,
                        modifier = Modifier.size(220.dp)
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = pages[page].title,
                        color = OffWhite,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = pages[page].description,
                        color = MediumGrey,
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) LimeGreen
                                else MediumGrey.copy(alpha = 0.5f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Get Started / Next button
            BimarihaunterButton(
                text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onGetStarted()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
