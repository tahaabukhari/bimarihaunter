package com.bimarihaunter.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BimarihaunterTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackArrow: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = OffWhite
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (showBackArrow) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OffWhite
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MidnightBlack,
            titleContentColor = OffWhite,
            navigationIconContentColor = OffWhite,
            actionIconContentColor = OffWhite
        )
    )
}
