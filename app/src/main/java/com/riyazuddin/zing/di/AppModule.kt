package com.riyazuddin.zing.di

import android.content.Context
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.riyazuddin.zing.R
import com.riyazuddin.zing.repositories.local.ChatDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ) = context

    @Singleton
    @Provides
    fun provideMainDispatcher() = Dispatchers.Main as CoroutineDispatcher

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .error(R.drawable.ic_outline_error)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )

    @Singleton
    @Provides
    fun provideFirebaseAuth() = run {
        val auth = FirebaseAuth.getInstance()
//        auth.useEmulator("192.168.0.7", 1111)
        auth
    }

    @Singleton
    @Provides
    fun provideFirestore() = run {
        val instance = FirebaseFirestore.getInstance()
//        val settings = FirebaseFirestoreSettings.Builder()
//            .setHost("192.168.0.7:2222")
//            .setSslEnabled(false)
//            .setPersistenceEnabled(false)
//            .build()
//        instance.firestoreSettings = settings
        instance
    }

    @Singleton
    @Provides
    fun providesFirebaseDatabase() = run {
        val database = FirebaseDatabase.getInstance()
//        database.useEmulator("192.168.0.7", 3333)
        database
    }

    @Provides
    @Singleton
    fun providesChatDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context,
        ChatDatabase::class.java,
        "chat_db"
    ).build()

    @Provides
    @Singleton
    fun provideLastMessagesDao(chatDatabase: ChatDatabase) = chatDatabase.getLastMessagesDao()
}