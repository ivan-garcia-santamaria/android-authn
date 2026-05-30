package com.masstack.authn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for dependency injection with Hilt
 */
@HiltAndroidApp
class AuthnApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
