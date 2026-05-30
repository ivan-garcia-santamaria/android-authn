package com.masstack.authn.di

import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.network.api.AuthApi
import com.masstack.authn.network.interceptors.AuthInterceptor
import com.masstack.authn.network.interceptors.LoggingInterceptor
import com.masstack.authn.network.interceptors.OriginInterceptor
import com.masstack.authn.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(@Suppress("UNUSED_PARAMETER") settingsRepository: SettingsRepository): AuthInterceptor {
        return AuthInterceptor {
            // This will be called when a request needs authentication
            // For now, returns null as tokens are managed per-flow
            null
        }
    }

    @Provides
    @Singleton
    fun provideCustomLoggingInterceptor(): LoggingInterceptor {
        // Use -1 for unlimited logging (no truncation)
        // Use positive number to limit body length (e.g., 500)
        return LoggingInterceptor(maxBodyLength = -1)
    }

    @Provides
    @Singleton
    fun provideOriginInterceptor(): OriginInterceptor {
        return OriginInterceptor()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        customLoggingInterceptor: LoggingInterceptor,
        authInterceptor: AuthInterceptor,
        originInterceptor: OriginInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(originInterceptor)          // Add Origin header for WebAuthn
            .addInterceptor(customLoggingInterceptor)  // Our custom logger (shows in app logs)
            .addInterceptor(loggingInterceptor)         // Standard logger (shows in Logcat)
            .addInterceptor(authInterceptor)
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        // Base URL is required by Retrofit but we use @Url for dynamic endpoints
        // So we use a placeholder that won't be used
        val baseUrl = "https://authn.sta.masstack.com/v1/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
}
