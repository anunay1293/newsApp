package com.example.news

import android.app.Application
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify

class NewsApplication : Application() {
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

