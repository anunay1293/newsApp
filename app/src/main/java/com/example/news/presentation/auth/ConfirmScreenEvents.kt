package com.example.news.presentation.auth

interface ConfirmScreenEvents {
    fun onConfirmSignUp(email: String, code: String)
    fun onResendCode(email: String)

    companion object : ConfirmScreenEvents {
        override fun onConfirmSignUp(email: String, code: String) = Unit
        override fun onResendCode(email: String) = Unit
    }
}
