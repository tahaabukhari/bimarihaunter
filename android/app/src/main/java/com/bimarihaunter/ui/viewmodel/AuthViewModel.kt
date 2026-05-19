package com.bimarihaunter.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimarihaunter.data.model.User
import com.bimarihaunter.data.repository.FirebaseRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AuthViewModel(private val repository: FirebaseRepository = FirebaseRepository()) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var verificationId: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                fetchUserProfile(user.uid)
            } else {
                _userProfile.value = null
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        repository.getUserProfile(uid) { profile ->
            if (profile != null) {
                _userProfile.value = profile
            } else {
                // If profile doesn't exist, create a default profile using user auth info
                val user = auth.currentUser
                if (user != null) {
                    val name = user.displayName ?: user.phoneNumber ?: "User"
                    val email = user.email ?: ""
                    val initials = name.split(" ").filter { it.isNotEmpty() }.joinToString("") { it.take(1) }.uppercase()
                    val newProfile = User(
                        uid = uid,
                        name = name,
                        email = email,
                        phoneNumber = user.phoneNumber ?: "",
                        initials = if (initials.isNotEmpty()) initials else "US"
                    )
                    repository.saveUserProfile(newProfile)
                    _userProfile.value = newProfile
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google Sign-In failed")
                }
            }
    }

    fun sendVerificationCode(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _authState.value = AuthState.Error(e.message ?: "Verification failed")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@AuthViewModel.verificationId = verificationId
                    _authState.value = AuthState.OtpSent
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        val verificationId = this.verificationId
        if (verificationId == null) {
            _authState.value = AuthState.Error("Verification ID is missing. Send code first.")
            return
        }
        _authState.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "OTP Verification failed")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object OtpSent : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}
