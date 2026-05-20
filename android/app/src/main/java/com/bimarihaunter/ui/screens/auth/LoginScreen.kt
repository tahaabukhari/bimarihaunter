package com.bimarihaunter.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimarihaunter.R
import com.bimarihaunter.ui.components.BimarihaunterButton
import com.bimarihaunter.ui.theme.*
import com.bimarihaunter.ui.viewmodel.AuthState
import com.bimarihaunter.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var loginMode by remember { mutableStateOf(0) } // 0: Phone, 1: Email
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }

    // Redirect to Home when logged in
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onNavigateToHome()
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
                Toast.makeText(context, "Welcome to Bimarihaunter!", Toast.LENGTH_SHORT).show()
            }
            is AuthState.Error -> {
                showOtpDialog = false
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Toast.makeText(context, "Google sign-in was canceled or failed.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account?.idToken != null) {
                authViewModel.signInWithGoogle(account.idToken)
            } else {
                Toast.makeText(context, "Failed to get Google ID token. Try again.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(
                context,
                "Google sign-in failed: ${e.statusCode}. ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        // Tab selection for Phone / Email Login
        TabRow(
            selectedTabIndex = loginMode,
            containerColor = CharcoalGrey,
            contentColor = OffWhite,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[loginMode]),
                    color = LimeGreen
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = loginMode == 0,
                onClick = { loginMode = 0 },
                text = { Text("SMS / Phone", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = loginMode == 1,
                onClick = { loginMode = 1 },
                text = { Text("Email / Pass", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (loginMode == 0) {
            // SMS Login Form
            Text(
                text = "Sign in via SMS",
                color = OffWhite,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your phone number to receive an OTP code",
                color = MediumGrey,
                fontFamily = InterFamily,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Phone field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = {
                    Text("Phone Number (e.g. +923001234567)", color = MediumGrey, fontFamily = InterFamily)
                },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MediumGrey)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Get Code button
            BimarihaunterButton(
                text = if (authState is AuthState.Loading) "Sending..." else "Send OTP Code",
                onClick = {
                    if (phoneNumber.isNotBlank() && activity != null) {
                        authViewModel.sendVerificationCode(phoneNumber.trim(), activity)
                    } else {
                        Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = authState !is AuthState.Loading
            )

        } else {
            // Email/Password Form
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

            Spacer(modifier = Modifier.height(24.dp))

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
                text = if (authState is AuthState.Loading) "Logging in..." else "Log In",
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        authViewModel.signInWithEmailAndPassword(email.trim(), password)
                    } else {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = authState !is AuthState.Loading
            )
        }

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
            onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            },
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
        Spacer(modifier = Modifier.height(24.dp))

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

    // OTP Code Verification Dialog
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
