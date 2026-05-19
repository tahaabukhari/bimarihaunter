package com.bimarihaunter.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit = {},
    onSendResetLink: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back arrow
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.Start).offset(x = (-12).dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OffWhite)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Ghost thinking
        Image(
            painter = painterResource(id = R.drawable.ghost_thinking),
            contentDescription = "Ghost Thinking",
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Reset Password",
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enter your email and we will send you\na link to reset your password.",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email Address", color = MediumGrey) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = MediumGrey) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey,
                focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey,
                focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen,
                focusedTextColor = OffWhite,
                unfocusedTextColor = OffWhite
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        BimarihaunterButton(
            text = "Send Reset Link",
            onClick = onSendResetLink,
            enabled = email.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Check your spam folder if you don't see the email.",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Back to Log In",
            color = LimeGreen,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onNavigateBack() }
        )
    }
}
