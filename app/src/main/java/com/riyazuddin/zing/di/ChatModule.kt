package com.riyazuddin.zing.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.repositories.local.LastMessageDao
import com.riyazuddin.zing.repositories.network.abstraction.ChatRepository
import com.riyazuddin.zing.repositories.network.implementation.DefaultChatRepository
import com.riyazuddin.zing.ui.main.viewmodels.ChatViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ChatModule {

    @ActivityScoped
    @Provides
    fun provideChatRepository(
        @ApplicationContext context: Context, firestore: FirebaseFirestore,
        cloudStorage: FirebaseStorage, lastMessageDao: LastMessageDao
    ) = DefaultChatRepository(context, firestore,cloudStorage, lastMessageDao) as ChatRepository

    /**
     * Providing ChatViewModel for ChatAdapter
     */
    @ActivityScoped
    @Provides
    fun provideChatVieModel(repository: ChatRepository) = ChatViewModel(repository)
}