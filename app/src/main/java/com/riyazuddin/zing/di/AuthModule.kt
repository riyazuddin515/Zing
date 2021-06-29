package com.riyazuddin.zing.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riyazuddin.zing.repositories.network.abstraction.AuthRepository
import com.riyazuddin.zing.repositories.network.implementation.DefaultAuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object AuthModule {

    @ActivityScoped
    @Provides
    fun provideAuthRepository(auth: FirebaseAuth, firestore: FirebaseFirestore) =
        DefaultAuthRepository(auth, firestore) as AuthRepository
}