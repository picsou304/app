package com.dailyvlog.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dailyvlog.app.ui.auth.AuthEffect
import com.dailyvlog.app.ui.auth.AuthUiState
import com.dailyvlog.app.ui.auth.AuthViewModel
import com.dailyvlog.app.ui.auth.LoginScreen
import com.dailyvlog.app.ui.auth.RegisterScreen
import com.dailyvlog.app.ui.home.HomeScreen
import com.dailyvlog.app.ui.navigation.HomeRoute
import com.dailyvlog.app.ui.navigation.LoginRoute
import com.dailyvlog.app.ui.navigation.ProfileRoute
import com.dailyvlog.app.ui.navigation.RegisterRoute
import com.dailyvlog.app.ui.navigation.WelcomeRoute
import com.dailyvlog.app.ui.profile.ProfileScreen
import com.dailyvlog.app.ui.theme.DailyVlogCrewTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DailyVlogCrewTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> {
                    navController.navigate(HomeRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                }
                is AuthEffect.NavigateToWelcome -> {
                    navController.navigate(WelcomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
                is AuthEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (state is AuthUiState.Unauthenticated && (state as AuthUiState.Unauthenticated).isLoading) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    val startDestination = if (state is AuthUiState.Authenticated) HomeRoute else WelcomeRoute

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onNavigateToLogin = {
                    viewModel.clearError()
                    navController.navigate(LoginRoute)
                },
                onNavigateToRegister = {
                    viewModel.clearError()
                    navController.navigate(RegisterRoute)
                }
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                state = state,
                onLogin = { email, pass -> viewModel.login(email, pass) },
                onNavigateToRegister = {
                    viewModel.clearError()
                    navController.navigate(RegisterRoute)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                state = state,
                onRegister = { email, pass, confirm, pseudo ->
                    viewModel.register(email, pass, confirm, pseudo)
                },
                onNavigateToLogin = {
                    viewModel.clearError()
                    navController.navigate(LoginRoute)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<HomeRoute> {
            HomeScreen(
                user = state.user,
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
                onLogout = { viewModel.logout() }
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                user = state.user,
                onUpdatePseudo = { viewModel.updatePseudo(it) },
                onLogout = { viewModel.logout() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("welcome_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .testTag("app_logo"),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.VideoCameraFront,
                    contentDescription = "Logo Daily Vlog Crew",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_subtitle")
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_login"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.login),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_register")
            ) {
                Text(
                    text = stringResource(R.string.register),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
