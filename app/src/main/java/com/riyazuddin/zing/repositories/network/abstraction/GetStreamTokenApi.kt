package com.riyazuddin.zing.repositories.network.abstraction

import com.riyazuddin.zing.data.entities.GetStreamToken
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GetStreamTokenApi {

    @GET("/streamTokenGenerator")
    suspend fun getToken(@Query("uid") uid: String): Response<GetStreamToken>
}