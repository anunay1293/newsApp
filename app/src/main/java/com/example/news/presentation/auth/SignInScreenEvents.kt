package com.example.news.presentation.auth

interface SignInScreenEvents {
    fun onSignIn(email: String, password: String)

    companion object : SignInScreenEvents {
        override fun onSignIn(email: String, password: String) = Unit
    }
}
