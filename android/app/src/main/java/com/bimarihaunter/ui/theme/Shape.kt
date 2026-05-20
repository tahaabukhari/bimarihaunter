package com.bimarihaunter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BimarihaunterShapes = Shapes(
    // Input fields
    small = RoundedCornerShape(14.dp),
    // Cards
    medium = RoundedCornerShape(16.dp),
    // Bottom sheets
    large = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    // Buttons (pill)
    extraLarge = RoundedCornerShape(50.dp)
)
