package com.example.news.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.news.presentation.auth.AuthUiState
import com.example.news.presentation.auth.AuthViewModel
import com.example.news.ui.auth.ConfirmScreen
import com.example.news.ui.auth.SignInScreen
import com.example.news.ui.auth.SignUpScreen

sealed class AuthScreen(val route: String) {
    object SignIn : AuthScreen("sign_in")
    object SignUp : AuthScreen("sign_up")
    data class Confirm(val email: String = "") : AuthScreen("confirm/{email}") {
        fun createRoute(email: String) = "confirm/$email"
    }
}

@Composable
fun AuthNavigation(
    onAuthSuccess: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AuthScreen.SignIn.route
    ) {
        composable(AuthScreen.SignIn.route) {
            SignInScreen(
                onNavigateToSignUp = {
                    navController.navigate(AuthScreen.SignUp.route)
                }
            )
        }
        
        composable(AuthScreen.SignUp.route) {
            SignUpScreen(
                onNavigateToConfirm = { email ->
                    android.util.Log.d("AuthNavigation", "Navigating to confirm screen for: $email")
                    navController.navigate(AuthScreen.Confirm("").createRoute(email)) {
                        // Pop back stack to sign up so back button doesn't go back to sign up
                        popUpTo(AuthScreen.SignUp.route) { inclusive = false }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(AuthScreen.SignIn.route) {
                        popUpTo(AuthScreen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable("confirm/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ConfirmScreen(
                email = email,
                onNavigateToSignIn = {
                    navController.navigate(AuthScreen.SignIn.route) {
                        popUpTo(AuthScreen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

