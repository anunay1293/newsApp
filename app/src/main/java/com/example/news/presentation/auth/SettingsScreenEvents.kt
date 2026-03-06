package com.example.news.presentation.auth

interface SettingsScreenEvents {
    fun onSignOut()

    companion object : SettingsScreenEvents {
        override fun onSignOut() = Unit
    }
}
