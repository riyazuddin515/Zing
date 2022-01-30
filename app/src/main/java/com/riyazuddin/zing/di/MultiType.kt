package com.riyazuddin.zing.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedSharedPreferencesAnnotated

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SharedPreferencesAnnotated