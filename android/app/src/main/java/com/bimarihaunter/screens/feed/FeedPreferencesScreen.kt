package com.bimarihaunter.ui.screens.feed

/*
 * FeedPreferencesScreen.kt
 *
 * Design system: MidnightBlack bg / CharcoalGrey cards / LimeGreen accent
 * Font: SpaceGroteskFamily (headings) + InterFamily (body)
 *
 * UX flow:
 *   - User sees categorised tag grid (diseases, disasters, world health, economy)
 *   - Multi-select: tap to toggle, LimeGreen = selected, CharcoalGrey = idle
 *   - Minimum 1 tag required before "Save Preferences" is enabled
 *   - On save: writes tags to Firestore + calls backend /users/{uid}/preferences
 *   - Entry points: post-signup (first-time) and Settings screen (any time)
 */

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.FeedPreferencesViewModel
import com.bimarihaunter.ui.viewmodel.FeedPreferencesViewModelFactory

// ── Tag catalogue ──────────────────────────────────────────────────────────────

data class FeedTag(
    val id: String,
    val label: String,
    val emoji: String,
)

data class TagCategory(
    val title: String,
    val tags: List<FeedTag>,
)

val ALL_TAG_CATEGORIES: List<TagCategory> = listOf(
    TagCategory(
        title = "Viral & Infectious Diseases",
        tags = listOf(
            FeedTag("dengue",        "Dengue Fever",        "🦟"),
            FeedTag("malaria",       "Malaria",             "🦟"),
            FeedTag("covid",         "COVID-19",            "🦠"),
            FeedTag("influenza",     "Influenza / Flu",     "🤧"),
            FeedTag("mpox",          "Mpox / Monkeypox",   "⚠️"),
            FeedTag("measles",       "Measles",             "🔴"),
            FeedTag("polio",         "Polio",               "💉"),
            FeedTag("hepatitis",     "Hepatitis",           "🟡"),
            FeedTag("tuberculosis",  "Tuberculosis",        "🫁"),
            FeedTag("cholera",       "Cholera",             "💧"),
            FeedTag("typhoid",       "Typhoid",             "🌡️"),
            FeedTag("rabies",        "Rabies",              "🐕"),
        )
    ),
    TagCategory(
        title = "Natural Disasters",
        tags = listOf(
            FeedTag("floods",        "Floods",              "🌊"),
            FeedTag("earthquake",    "Earthquakes",         "🌍"),
            FeedTag("heatwave",      "Heatwaves",           "🌡️"),
            FeedTag("drought",       "Drought",             "☀️"),
            FeedTag("cyclone",       "Cyclones / Storms",   "🌀"),
            FeedTag("landslide",     "Landslides",          "⛰️"),
        )
    ),
    TagCategory(
        title = "Global Health & WHO",
        tags = listOf(
            FeedTag("who_alerts",    "WHO Alerts",          "🌐"),
            FeedTag("outbreak",      "Outbreak Watch",      "📡"),
            FeedTag("pandemic",      "Pandemic Tracker",    "🧬"),
            FeedTag("vaccination",   "Vaccination Drives",  "💉"),
            FeedTag("antimicrobial", "Antimicrobial Resistance", "🔬"),
            FeedTag("zoonotic",      "Zoonotic Diseases",   "🐾"),
        )
    ),
    TagCategory(
        title = "Pakistan Health",
        tags = listOf(
            FeedTag("pk_health",     "Pakistan Health News","🇵🇰"),
            FeedTag("nih_pakistan",  "NIH Pakistan",        "🏥"),
            FeedTag("water_quality", "Water Quality",       "🚰"),
            FeedTag("air_quality",   "Air Quality / Smog",  "🌫️"),
            FeedTag("food_safety",   "Food Safety",         "🍽️"),
        )
    ),
    TagCategory(
        title = "Economy & Society",
        tags = listOf(
            FeedTag("economy",       "Economy",             "📈"),
            FeedTag("food_crisis",   "Food Crisis",         "🌾"),
            FeedTag("refugee",       "Refugee Health",      "🏕️"),
            FeedTag("mental_health", "Mental Health",       "🧠"),
        )
    ),
)

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPreferencesScreen(
    isFirstTime: Boolean = false,
    onSaved: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: FeedPreferencesViewModel = viewModel(
        factory = FeedPreferencesViewModelFactory()
    ),
) {
    val selectedTags by viewModel.selectedTags.collectAsState()
    val saveStatus  by viewModel.saveStatus.collectAsState()

    // Navigate away on success
    LaunchedEffect(saveStatus) {
        if (saveStatus is FeedPreferencesViewModel.SaveStatus.Success) {
            onSaved()
        }
    }

    Scaffold(
        containerColor = MidnightBlack,
        topBar = {
            if (!isFirstTime) {
                TopAppBar(
                    title = {
                        Text(
                            "Feed Preferences",
                            color = OffWhite,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OffWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlack),
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightBlack)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
            ) {
                if (saveStatus is FeedPreferencesViewModel.SaveStatus.Error) {
                    Text(
                        text = (saveStatus as FeedPreferencesViewModel.SaveStatus.Error).message,
                        color = EmberRed,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = { viewModel.savePreferences() },
                    enabled = selectedTags.isNotEmpty() &&
                              saveStatus !is FeedPreferencesViewModel.SaveStatus.Saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeGreen,
                        disabledContainerColor = CharcoalGrey,
                        contentColor = MidnightBlack,
                        disabledContentColor = MediumGrey,
                    ),
                ) {
                    if (saveStatus is FeedPreferencesViewModel.SaveStatus.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MidnightBlack,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = if (isFirstTime) "Start My Feed  →" else "Save Preferences",
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
                if (isFirstTime) {
                    TextButton(
                        onClick = onSaved,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Skip for now",
                            color = MediumGrey,
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Header
            item {
                Spacer(Modifier.height(if (isFirstTime) 32.dp else 8.dp))
                if (isFirstTime) {
                    Text(
                        text = "What are you\ninterested in?",
                        color = OffWhite,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Choose topics to personalise your feed. You can change these anytime in Settings.",
                        color = MediumGrey,
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                } else {
                    Text(
                        text = "Choose your topics",
                        color = MediumGrey,
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Selected count badge
                if (selectedTags.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = LimeGreen.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = "${selectedTags.size} selected",
                            color = LimeGreen,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Tag categories
            ALL_TAG_CATEGORIES.forEach { category ->
                item(key = category.title) {
                    TagCategorySection(
                        category = category,
                        selectedTags = selectedTags,
                        onTagToggled = { tag -> viewModel.toggleTag(tag) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Category section ───────────────────────────────────────────────────────────

@Composable
private fun TagCategorySection(
    category: TagCategory,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = category.title.uppercase(),
            color = MediumGrey,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
        )
        // Wrap tags in a FlowRow-style layout using chunked rows
        val chunked = category.tags.chunked(2)
        chunked.forEach { rowTags ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTags.forEach { tag ->
                    TagChip(
                        tag = tag,
                        isSelected = tag.id in selectedTags,
                        onToggle = { onTagToggled(tag.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill empty slot if odd number of tags in row
                if (rowTags.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Individual tag chip ────────────────────────────────────────────────────────

@Composable
private fun TagChip(
    tag: FeedTag,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LimeGreen.copy(alpha = 0.15f) else CharcoalGrey,
        animationSpec = tween(150),
        label = "chip_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) LimeGreen else Color.Transparent,
        animationSpec = tween(150),
        label = "chip_border",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) LimeGreen else OffWhite,
        animationSpec = tween(150),
        label = "chip_text",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = tag.emoji, fontSize = 18.sp)
        Text(
            text = tag.label,
            color = textColor,
            fontFamily = InterFamily,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = LimeGreen,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
