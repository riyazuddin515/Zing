package com.riyazuddin.zing.repositories.network.abstraction

import com.riyazuddin.zing.other.Resource

interface AuthRepository {

    suspend fun register(email: String, password: String): Resource<Boolean>

    suspend fun login(email: String, password: String): Resource<Boolean>

    suspend fun sendPasswordResetLink(email: String): Resource<String>

    suspend fun searchUsername(query: String): Resource<Boolean>
}