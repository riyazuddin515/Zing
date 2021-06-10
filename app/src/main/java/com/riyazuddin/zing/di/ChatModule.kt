package com.riyazuddin.zing.di

import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.repositories.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.implementation.DefaultChatRepository
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ChatModule {

    @ActivityScoped
    @Provides
    fun provideChatRepository(firestore: FirebaseFirestore) = DefaultChatRepository(firestore) as ChatRepository

    /**
     * Providing ChatViewModel for ChatAdapter
     */
    @ActivityScoped
    @Provides
    fun provideChatVieModel(repository: ChatRepository) = ChatViewModel(repository)
}