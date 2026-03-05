package com.example.news

import android.app.Application
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom [Application] subclass annotated with [HiltAndroidApp] to trigger Hilt's
 * code generation and serve as the application-level dependency container.
 *
 * Also bootstraps the AWS Amplify framework with the Cognito authentication plugin
 * before any Activity or Service is created.
 *
 * Declared in `AndroidManifest.xml` via `android:name=".NewsApplication"`.
 */
@HiltAndroidApp
class NewsApplication : Application() {

    /**
     * Called when the application process is first created.
     *
     * Registers the [AWSCognitoAuthPlugin] with the Amplify framework and applies the
     * configuration bundled in `amplifyconfiguration.json`. If initialization fails
     * (e.g., missing config, duplicate plugin registration), the exception is logged
     * but the app continues running so the user can still view cached data.
     */
    override fun onCreate() {
        super.onCreate()
        
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.configure(applicationContext)
        } catch (error: AmplifyException) {
            error.printStackTrace()
        }
    }
}

