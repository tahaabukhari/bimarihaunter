package com.bimarihaunter

import android.app.Application
import timber.log.Timber

class BimarihaunterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
