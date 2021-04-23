package com.riyazuddin.zing.repositories

import com.riyazuddin.zing.Auth
import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Resource
import com.riyazuddin.zing.repositories.abstraction.AuthRepository
import java.util.*

class FakeAuthRepository : AuthRepository {

    private val users = mutableListOf<User>()

    private val authList = mutableListOf<Auth>()

    init {
        val auth1 = Auth("riyazuddin515@gmail.com", "12345678")
        authList.add(auth1)
        val user = User("Riyaz", UUID.randomUUID().toString(), "riyazuddin515")
        users.add(user)
    }

    override suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String
    ): Resource<Boolean> {
        val user = User(name, UUID.randomUUID().toString(), username)
        val authOB = Auth(email, password)
        authList.forEach { auth ->
            if (auth.email == email)
                return Resource.Error("Email already exits", false)
        }
        authList.add(authOB)
        users.add(user)
        return Resource.Success(true)
    }

    override suspend fun login(email: String, password: String): Resource<Boolean> {
        return if (authList.contains(Auth(email, password)))
            Resource.Success(true)
        else
            Resource.Error("No user record found", false)
    }

    override suspend fun sendPasswordResetLink(email: String): Resource<String> {
        authList.forEach { auth ->
            return if (auth.email == email)
                Resource.Success("Mail Sent")
            else
                Resource.Error("No user record found", null)
        }
        return Resource.Error("No user record found", null)
    }

    override suspend fun searchUsername(query: String): Resource<Boolean> {
        for (user in users) {
            if (user.username == query)
                return Resource.Success(false)
        }
        return Resource.Success(true)
    }
}