package com.mdfoysal.jarvis

import android.app.Application

/**
 * Application entry point. It creates the dependency container that the whole app uses.
 */
class JarvisApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
