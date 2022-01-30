package com.riyazuddin.zing.di

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.repositories.network.abstraction.MainRepository
import com.riyazuddin.zing.repositories.network.implementation.DefaultMainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun provideMainRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        database: FirebaseDatabase,
        cloudStorage: FirebaseStorage,
        @SharedPreferencesAnnotated sharedPreferences: SharedPreferences
    ) =
        DefaultMainRepository(
            auth,
            firestore,
            database,
            cloudStorage,
            sharedPreferences
        ) as MainRepository
}