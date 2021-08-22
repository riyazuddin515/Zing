package com.riyazuddin.zing.di

import com.riyazuddin.zing.repositories.network.abstraction.GetStreamRepository
import com.riyazuddin.zing.repositories.network.abstraction.GetStreamTokenApi
import com.riyazuddin.zing.repositories.network.implementation.DefaultGetStreamRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.getstream.chat.android.client.ChatClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GetStreamModule {

    @Provides
    @Singleton
    fun provideGetStreamRepository(
        chatClient: ChatClient,
        getStreamTokenApi: GetStreamTokenApi
    ) = DefaultGetStreamRepository(chatClient, getStreamTokenApi) as GetStreamRepository
}