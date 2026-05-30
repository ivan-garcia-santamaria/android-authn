package com.masstack.authn.di

import android.content.Context
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.data.repositories.SettingsRepository
import com.masstack.authn.services.AuthorizationCodeService
import com.masstack.authn.services.CibaService
import com.masstack.authn.services.WebAuthnService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideAuthorizationCodeService(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        authRepository: AuthRepository
    ): AuthorizationCodeService {
        return AuthorizationCodeService(context, settingsRepository, authRepository)
    }

    @Provides
    @Singleton
    fun provideCibaService(
        authRepository: AuthRepository
    ): CibaService {
        return CibaService(authRepository)
    }

    @Provides
    @Singleton
    fun provideWebAuthnService(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
        settingsRepository: SettingsRepository
    ): WebAuthnService {
        return WebAuthnService(context, authRepository, settingsRepository)
    }
}
