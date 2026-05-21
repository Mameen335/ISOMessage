package com.mameen.isomessage

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — required entry point for Hilt dependency injection.
 *
 * @HiltAndroidApp triggers Hilt's code generation and bootstraps the DI container.
 * All subsequent @Inject constructors and Hilt modules become available after this.
 *
 * Must be registered in AndroidManifest.xml:
 *   android:name=".IsoMessageApplication"
 */
@HiltAndroidApp
class IsoMessageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Application-level initialization goes here
        // e.g., Timber logging setup, crash reporting, etc.
    }
}
