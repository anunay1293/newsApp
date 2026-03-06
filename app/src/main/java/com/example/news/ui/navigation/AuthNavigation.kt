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

/**
 * Sealed class defining the destinations within the authentication navigation graph.
 *
 * @property route The route template registered with the [NavHost]. For parameterised routes
 *                 (e.g., [Confirm]) the placeholder `{email}` is replaced at navigation time.
 */
sealed class AuthScreen(val route: String) {
    /** The sign-in screen where existing users enter their credentials. */
    object SignIn : AuthScreen("sign_in")

    /** The sign-up screen where new users register an account. */
    object SignUp : AuthScreen("sign_up")

    /**
     * The email-confirmation screen shown after successful sign-up.
     *
     * @property email The email address passed as a navigation argument (defaults to empty
     *                 when used as a route template).
     */
    data class Confirm(val email: String = "") : AuthScreen("confirm/{email}") {
        /**
         * Builds a concrete route with the given [email] substituted into the template.
         *
         * @param email The email address to embed in the route.
         * @return A fully resolved route string, e.g., `"confirm/user@example.com"`.
         */
        fun createRoute(email: String) = "confirm/$email"
    }
}

/**
 * Composable that hosts the authentication navigation graph (sign-in → sign-up → confirm).
 *
 * The graph starts at [AuthScreen.SignIn]. Navigation between screens is handled via
 * lambda callbacks passed to each screen composable, keeping the screens decoupled from
 * the navigation framework.
 *
 * @param onAuthSuccess Callback invoked when the authentication flow completes successfully.
 *                      In practice the [AuthGate] observes state changes instead, so this
 *                      callback is currently a no-op placeholder.
 * @param navController The [NavHostController] managing back-stack state for this graph.
 */
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
                },
                onSignInSuccess = onAuthSuccess
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

