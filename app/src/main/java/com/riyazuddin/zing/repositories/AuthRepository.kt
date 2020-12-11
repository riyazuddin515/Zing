package com.riyazuddin.zing.repositories

import com.google.firebase.auth.AuthResult
import com.riyazuddin.zing.other.Resource

interface AuthRepository {

    suspend fun register(email: String, password: String) : Resource<AuthResult>

    suspend fun login(email: String, password: String) : Resource<AuthResult>

    suspend fun sendPasswordResetLink(email: String) : Resource<String>
}