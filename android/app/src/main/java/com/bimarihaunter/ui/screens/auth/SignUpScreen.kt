package com.bimarihaunter.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AuthState
import com.bimarihaunter.ui.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    var otpCode by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }

    // Redirect on success
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onCreateAccount()
        }
    }

    // Handle AuthState transitions
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.OtpSent -> {
                showOtpDialog = true
            }
            is AuthState.Success -> {
                showOtpDialog = false
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                onCreateAccount()
            }
            is AuthState.Error -> {
                showOtpDialog = false
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .systemBarsPadding()
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

        // Phone Number
        OutlinedTextField(
            value = phoneNumber, onValueChange = { phoneNumber = it },
            placeholder = { Text("Phone Number (+923001234567)", color = MediumGrey) },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = MediumGrey) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CharcoalGrey, focusedContainerColor = CharcoalGrey,
                unfocusedBorderColor = CharcoalGrey, focusedBorderColor = LimeGreen,
                cursorColor = LimeGreen, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )

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

        BimarihaunterButton(
            text = if (authState is AuthState.Loading) "Verifying..." else "Verify Phone & Sign Up",
            onClick = {
                if (phoneNumber.isNotBlank() && fullName.isNotBlank() && activity != null) {
                    authViewModel.sendVerificationCode(phoneNumber.trim(), activity)
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = agreedToTerms && phoneNumber.isNotBlank() && fullName.isNotBlank() && authState !is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account?  ", color = MediumGrey, fontSize = 14.sp, fontFamily = InterFamily)
            Text("Log In", color = LimeGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                fontFamily = InterFamily, modifier = Modifier.clickable { onNavigateToLogin() })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // OTP Code Verification Dialog for Sign Up
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = {
                showOtpDialog = false
                authViewModel.resetState()
            },
            title = {
                Text("Enter OTP Code", color = OffWhite, fontFamily = SpaceGroteskFamily)
            },
            text = {
                Column {
                    Text("We've sent a 6-digit verification code to $phoneNumber", color = MediumGrey, fontFamily = InterFamily)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        placeholder = { Text("6-digit code", color = MediumGrey) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MidnightBlack,
                            focusedContainerColor = MidnightBlack,
                            unfocusedBorderColor = CharcoalGrey,
                            focusedBorderColor = LimeGreen,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (otpCode.length == 6) {
                            authViewModel.verifyOtp(otpCode)
                        } else {
                            Toast.makeText(context, "Enter standard 6-digit code", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Verify", color = LimeGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOtpDialog = false
                    authViewModel.resetState()
                }) {
                    Text("Cancel", color = MediumGrey)
                }
            },
            containerColor = CharcoalGrey
        )
    }
}
