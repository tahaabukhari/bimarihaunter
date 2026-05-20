package com.bimarihaunter.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    private val tag = "FirebaseAuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            fetchFirebaseIdToken()
        }

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d(tag, "Firebase ID Token injected into request")
        } else {
            Log.w(tag, "No valid Firebase token available; request will proceed unauthenticated")
        }

        return chain.proceed(requestBuilder.build())
    }

    private suspend fun fetchFirebaseIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            user.getIdToken(true).await().token
        } catch (e: Exception) {
            Log.e(tag, "Failed to get Firebase ID Token", e)
            null
        }
    }
}
