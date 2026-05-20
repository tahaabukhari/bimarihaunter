package com.bimarihaunter.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.ui.theme.*

@Composable
fun BimarihaunterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LimeGreen,
                contentColor = MidnightBlack,
                disabledContainerColor = LimeGreen.copy(alpha = 0.4f),
                disabledContentColor = MidnightBlack.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = text,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CharcoalGrey,
                contentColor = LimeGreen,
                disabledContainerColor = CharcoalGrey.copy(alpha = 0.4f),
                disabledContentColor = LimeGreen.copy(alpha = 0.4f)
            ),
            border = null,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = text,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}
