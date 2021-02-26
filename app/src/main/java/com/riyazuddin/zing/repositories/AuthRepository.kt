package com.riyazuddin.zing.repositories

import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.QuerySnapshot
import com.riyazuddin.zing.other.Resource

interface AuthRepository {

    suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String
    ): Resource<AuthResult>

    suspend fun login(email: String, password: String): Resource<AuthResult>

    suspend fun sendPasswordResetLink(email: String): Resource<String>

    suspend fun searchUsername(query: String): Resource<QuerySnapshot>
}