package com.example.news.presentation.auth

interface SignUpScreenEvents {
    fun onSignUp(email: String, password: String)

    companion object : SignUpScreenEvents {
        override fun onSignUp(email: String, password: String) = Unit
    }
}
