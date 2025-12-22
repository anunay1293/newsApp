package com.example.news.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import com.example.news.presentation.auth.AuthViewModel

@Composable
fun SettingsScreen() {
    val application = LocalContext.current.applicationContext as Application
    val activity = androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity
    // Use Activity's ViewModelStoreOwner to share ViewModel instance with AuthGate
    val viewModel: AuthViewModel = if (activity != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(
            viewModelStoreOwner = activity,
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(application) as T
                }
            }
        )
    } else {
        viewModel(
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(application) as T
                }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = { viewModel.signOut() },
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Out")
        }
    }
}

