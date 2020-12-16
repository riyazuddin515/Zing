package com.riyazuddin.zing.repositories

import com.riyazuddin.zing.data.entities.User
import com.riyazuddin.zing.other.Resource

interface MainRepository {

    suspend fun getUser() : Resource<User>
}