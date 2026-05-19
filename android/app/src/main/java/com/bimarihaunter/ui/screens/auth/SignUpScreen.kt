package com.bimarihaunter.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SignUpScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onCreateAccount: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }

    val passwordStrength = when {
        password.length >= 12 -> 1f
        password.length >= 8 -> 0.66f
        password.length >= 4 -> 0.33f
        else -> 0f
    }
    val strengthLabel = when {
        password.isEmpty() -> ""
        passwordStrength >= 1f -> "Strong"
        passwordStrength >= 0.66f -> "Good strength"
        passwordStrength >= 0.33f -> "Weak"
        else -> "Too short"
    }
    val strengthColor = when {
        passwordStrength >= 0.66f -> LimeGreen
        passwordStrength >= 0.33f -> GoldWarning
        else -> EmberRed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Back arrow
        IconButton(onClick = onNavigateBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OffWhite)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ghost_happy), "Logo", Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = LimeGreen)) { append("bimari") }
                    withStyle(SpanStyle(color = OffWhite)) { append("haunter") }
                },
                fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Create Account", color = OffWhite, fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold, fontSize = 28.sp)

        Spacer(modifier = Modifier.height(28.dp))

        // Full Name
        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it },
            placeholder = { Text("Full Name", color = MediumGrey) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = MediumGrey) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
            ), singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Email
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            placeholder = { Text("Email Address", color = MediumGrey) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = MediumGrey) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
            ), singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            placeholder = { Text("Password", color = MediumGrey) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = MediumGrey) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle", tint = MediumGrey)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
            ), singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            placeholder = { Text("Confirm Password", color = MediumGrey) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = MediumGrey) },
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Toggle", tint = MediumGrey)
                }
            },
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
            ), singleLine = true
        )

        // Password strength bar
        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.weight(1f).height(4.dp).background(
                            if (i < (passwordStrength * 3).toInt()) strengthColor
                            else MediumGrey.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(strengthLabel, color = strengthColor, fontSize = 12.sp, fontFamily = InterFamily)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Terms checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreedToTerms, onCheckedChange = { agreedToTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = LimeGreen, uncheckedColor = MediumGrey,
                    checkmarkColor = MidnightBlack
                )
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = OffWhite)) { append("I agree to ") }
                    withStyle(SpanStyle(color = LimeGreen)) { append("Terms of Service") }
                    withStyle(SpanStyle(color = OffWhite)) { append(" and ") }
                    withStyle(SpanStyle(color = LimeGreen)) { append("Privacy Policy") }
                },
                fontSize = 13.sp, fontFamily = InterFamily
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BimarihaunterButton(text = "Create Account", onClick = onCreateAccount,
            enabled = agreedToTerms && email.isNotBlank() && password.isNotBlank())

        Spacer(modifier = Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account?  ", color = MediumGrey, fontSize = 14.sp, fontFamily = InterFamily)
            Text("Log In", color = LimeGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                fontFamily = InterFamily, modifier = Modifier.clickable { onNavigateToLogin() })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
