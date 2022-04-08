package com.riyazuddin.zing.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.riyazuddin.zing.BuildConfig
import com.riyazuddin.zing.R
import com.riyazuddin.zing.other.Constants.ENCRYPTED_SHARED_PREF_NAME
import com.riyazuddin.zing.other.Constants.STREAM_TOKEN_API_URL
import com.riyazuddin.zing.repositories.network.abstraction.GetStreamTokenApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.livedata.ChatDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
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
        context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .error(R.drawable.ic_outline_error)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )

    @Singleton
    @Provides
    fun provideFirebaseAuth() = run {
        val auth = FirebaseAuth.getInstance()
//        if (BuildConfig.DEBUG) {
//            auth.useEmulator("192.168.0.8", 1111)
//        }
        auth
    }

    @Singleton
    @Provides
    fun provideFirestore() = run {
        val instance = FirebaseFirestore.getInstance()
//        if (BuildConfig.DEBUG) {
//            val settings = FirebaseFirestoreSettings.Builder()
//                .setHost("192.168.0.8:2221")
//                .setSslEnabled(false)
//                .setPersistenceEnabled(false)
//                .build()
//            instance.firestoreSettings = settings
//        }
        instance
    }

    @Singleton
    @Provides
    fun providesFirebaseDatabase() = run {
        val database = FirebaseDatabase.getInstance()
//        if (BuildConfig.DEBUG) {
//            database.useEmulator("192.168.0.8", 3331)
//        }
        database
    }

    @Provides
    @Singleton
    fun provideCloudStorage() = run {
        val storage = FirebaseStorage.getInstance()
//        if (BuildConfig.DEBUG) {
//            storage.useEmulator("192.168.0.8", 5555)
//        }
        storage
    }

    @Provides
    @Singleton
    fun provideChatClient(context: Context): ChatClient = run {
        ChatClient.Builder(BuildConfig.STREAM_KEY, context).build()
        ChatDomain.Builder(ChatClient.instance(), context).offlineEnabled().enableBackgroundSync()
            .build()
        ChatClient.instance()
    }

    @Provides
    @Singleton
    fun provideGetStreamTokenApi(): GetStreamTokenApi = Retrofit.Builder()
        .baseUrl(STREAM_TOKEN_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GetStreamTokenApi::class.java)

    @EncryptedSharedPreferencesAnnotated
    @Singleton
    @Provides
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_SHARED_PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @SharedPreferencesAnnotated
    @Singleton
    @Provides
    fun provideUserSharedPreference(
        @ApplicationContext context: Context
    ): SharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)
}