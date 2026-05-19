package com.bimarihaunter.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Ghost logo
        Image(
            painter = painterResource(id = R.drawable.ghost_happy),
            contentDescription = "Ghost Logo",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // App name
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = LimeGreen)) { append("bimari") }
                withStyle(SpanStyle(color = OffWhite)) { append("haunter") }
            },
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Welcome back
        Text(
            text = "Welcome back",
            color = OffWhite,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in to continue",
            color = MediumGrey,
            fontFamily = InterFamily,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = {
                Text("Email Address", color = MediumGrey, fontFamily = InterFamily)
            },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = MediumGrey)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = {
                Text("Password", color = MediumGrey, fontFamily = InterFamily)
            },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MediumGrey)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = MediumGrey
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot password
        Text(
            text = "Forgot password?",
            color = LimeGreen,
            fontFamily = InterFamily,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onNavigateToForgotPassword() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sign In button
        BimarihaunterButton(
            text = "Log In",
            onClick = onNavigateToHome
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalGrey)
            Text(
                text = "  or  ",
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 12.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = CharcoalGrey)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google button
        OutlinedButton(
            onClick = { /* TODO: Google sign in */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CharcoalGrey,
                contentColor = OffWhite
            ),
            border = null
        ) {
            Text(
                text = "Continue with Google",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom link
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Don't have an account?  ",
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 14.sp
            )
            Text(
                text = "Sign Up",
                color = LimeGreen,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )
        }
    }
}
