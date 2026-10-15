package com.dailyvlog.app

import android.content.Intent
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.navigation.toRoute
import com.dailyvlog.app.ui.auth.AuthEffect
import com.dailyvlog.app.ui.auth.AuthUiState
import com.dailyvlog.app.ui.auth.AuthViewModel
import com.dailyvlog.app.ui.auth.LoginScreen
import com.dailyvlog.app.ui.auth.RegisterScreen
import com.dailyvlog.app.ui.group.CreateGroupScreen
import com.dailyvlog.app.ui.group.GroupDetailScreen
import com.dailyvlog.app.ui.group.GroupEffect
import com.dailyvlog.app.ui.group.GroupViewModel
import com.dailyvlog.app.ui.group.JoinGroupScreen
import com.dailyvlog.app.ui.home.HomeScreen
import com.dailyvlog.app.ui.navigation.CreateGroupRoute
import com.dailyvlog.app.ui.navigation.GroupDetailRoute
import com.dailyvlog.app.ui.navigation.HomeRoute
import com.dailyvlog.app.ui.navigation.JoinGroupRoute
import com.dailyvlog.app.ui.navigation.LoginRoute
import com.dailyvlog.app.ui.navigation.ProfileRoute
import com.dailyvlog.app.ui.navigation.RegisterRoute
import com.dailyvlog.app.ui.navigation.WelcomeRoute
import com.dailyvlog.app.ui.profile.ProfileScreen
import com.dailyvlog.app.ui.theme.DailyVlogCrewTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingInviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        setContent {
            DailyVlogCrewTheme {
                AppRoot(
                    pendingInviteCode = pendingInviteCode,
                    onClearPendingInviteCode = { pendingInviteCode = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "dailyvlog" && uri.host == "join") {
            val code = uri.lastPathSegment?.trim()
            if (!code.isNullOrEmpty()) {
                pendingInviteCode = code
            }
        }
    }
}

@Composable
fun AppRoot(
    pendingInviteCode: String? = null,
    onClearPendingInviteCode: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel(),
    todayViewModel: com.dailyvlog.app.ui.today.TodayViewModel = hiltViewModel(),
    recordVlogViewModel: com.dailyvlog.app.ui.record.RecordVlogViewModel = hiltViewModel(),
    watchVlogViewModel: com.dailyvlog.app.ui.watch.WatchVlogViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val groupState by groupViewModel.uiState.collectAsStateWithLifecycle()
    val todayState by todayViewModel.uiState.collectAsStateWithLifecycle()
    val recordVlogState by recordVlogViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current

    // Auth effects
    LaunchedEffect(Unit) {
        authViewModel.effects.collect { effect ->
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

    // Group effects
    LaunchedEffect(Unit) {
        groupViewModel.effects.collect { effect ->
            when (effect) {
                is GroupEffect.NavigateToGroup -> {
                    navController.navigate(GroupDetailRoute(effect.groupId)) {
                        popUpTo(CreateGroupRoute) { inclusive = true }
                        popUpTo(JoinGroupRoute()) { inclusive = true }
                    }
                }
                is GroupEffect.GroupDeletedOrLeft -> {
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
                is GroupEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Today effects
    LaunchedEffect(Unit) {
        todayViewModel.effects.collect { effect ->
            when (effect) {
                is com.dailyvlog.app.ui.today.TodayEffect.NavigateToRecord -> {
                    navController.navigate(
                        com.dailyvlog.app.ui.navigation.RecordVlogRoute(
                            groupId = effect.groupId,
                            maxDurationSeconds = effect.maxDuration
                        )
                    )
                }
                is com.dailyvlog.app.ui.today.TodayEffect.NavigateToWatchVlog -> {
                    navController.navigate(
                        com.dailyvlog.app.ui.navigation.WatchVlogRoute(
                            groupId = effect.groupId,
                            vlogId = effect.vlogId
                        )
                    )
                }
                is com.dailyvlog.app.ui.today.TodayEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Watch Vlog effects
    LaunchedEffect(Unit) {
        watchVlogViewModel.effects.collect { effect ->
            when (effect) {
                is com.dailyvlog.app.ui.watch.WatchVlogEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is com.dailyvlog.app.ui.watch.WatchVlogEffect.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    // Record Vlog effects
    LaunchedEffect(Unit) {
        recordVlogViewModel.effects.collect { effect ->
            when (effect) {
                is com.dailyvlog.app.ui.record.RecordVlogEffect.VlogPublished -> {
                    navController.popBackStack()
                }
                is com.dailyvlog.app.ui.record.RecordVlogEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handle deep link when user is authenticated
    LaunchedEffect(pendingInviteCode, authState) {
        val code = pendingInviteCode
        if (code != null && authState is AuthUiState.Authenticated) {
            onClearPendingInviteCode()
            navController.navigate(JoinGroupRoute(prefilledCode = code))
        }
    }

    if (authState is AuthUiState.Unauthenticated && (authState as AuthUiState.Unauthenticated).isLoading) {
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

    val startDestination = if (authState is AuthUiState.Authenticated) HomeRoute else WelcomeRoute

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onNavigateToLogin = {
                    authViewModel.clearError()
                    navController.navigate(LoginRoute)
                },
                onNavigateToRegister = {
                    authViewModel.clearError()
                    navController.navigate(RegisterRoute)
                }
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                state = authState,
                onLogin = { email, pass -> authViewModel.login(email, pass) },
                onNavigateToRegister = {
                    authViewModel.clearError()
                    navController.navigate(RegisterRoute)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                state = authState,
                onRegister = { email, pass, confirm, pseudo ->
                    authViewModel.register(email, pass, confirm, pseudo)
                },
                onNavigateToLogin = {
                    authViewModel.clearError()
                    navController.navigate(LoginRoute)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<HomeRoute> {
            HomeScreen(
                user = authState.user,
                groups = groupState.groups,
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
                onLogout = { authViewModel.logout() },
                onCreateGroup = {
                    groupViewModel.clearError()
                    navController.navigate(CreateGroupRoute)
                },
                onJoinGroup = {
                    groupViewModel.clearError()
                    navController.navigate(JoinGroupRoute())
                },
                onSelectGroup = { groupId ->
                    navController.navigate(GroupDetailRoute(groupId))
                }
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                user = authState.user,
                onUpdatePseudo = { authViewModel.updatePseudo(it) },
                onLogout = { authViewModel.logout() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<CreateGroupRoute> {
            CreateGroupScreen(
                state = groupState,
                onCreateGroup = { name, emoji, colorHex, switchTime, maxDuration ->
                    groupViewModel.createGroup(name, emoji, colorHex, switchTime, maxDuration)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<JoinGroupRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<JoinGroupRoute>()
            JoinGroupScreen(
                state = groupState,
                prefilledCode = route.prefilledCode,
                onJoinGroup = { code ->
                    groupViewModel.joinGroup(code)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GroupDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GroupDetailRoute>()
            GroupDetailScreen(
                groupId = route.groupId,
                state = groupState,
                currentUserId = authState.user?.id,
                onSelectGroup = { groupId ->
                    groupViewModel.selectGroup(groupId)
                },
                onRegenerateCode = { groupId ->
                    groupViewModel.regenerateCode(groupId)
                },
                onRemoveMember = { groupId, memberUserId ->
                    groupViewModel.removeMember(groupId, memberUserId)
                },
                onLeaveGroup = { groupId ->
                    groupViewModel.leaveGroup(groupId)
                },
                onDeleteGroup = { groupId ->
                    groupViewModel.deleteGroup(groupId)
                },
                onNavigateToToday = { gid ->
                    navController.navigate(com.dailyvlog.app.ui.navigation.TodayRoute(gid))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<com.dailyvlog.app.ui.navigation.TodayRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<com.dailyvlog.app.ui.navigation.TodayRoute>()
            com.dailyvlog.app.ui.today.TodayScreen(
                groupId = route.groupId,
                state = todayState,
                onLoadGroup = { todayViewModel.loadGroup(it) },
                onRecordClicked = { todayViewModel.onRecordClicked() },
                onWatchVlog = { vlogId ->
                    navController.navigate(
                        com.dailyvlog.app.ui.navigation.WatchVlogRoute(
                            groupId = route.groupId,
                            vlogId = vlogId
                        )
                    )
                },
                onSkipTurn = { todayViewModel.skipTurn() },
                onAdvanceDay = { todayViewModel.advanceDay() },
                onAdvanceHour = { todayViewModel.advanceHour() },
                onResetClock = { todayViewModel.resetClock() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<com.dailyvlog.app.ui.navigation.RecordVlogRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<com.dailyvlog.app.ui.navigation.RecordVlogRoute>()
            com.dailyvlog.app.ui.record.RecordVlogScreen(
                groupId = route.groupId,
                maxDurationSeconds = route.maxDurationSeconds,
                state = recordVlogState,
                onMaxDurationInit = { recordVlogViewModel.setMaxDuration(it) },
                onRecordingStarted = { recordVlogViewModel.onRecordingStarted() },
                onRecordingTick = { recordVlogViewModel.onRecordingTick(it) },
                onVideoReady = { uri, duration -> recordVlogViewModel.onVideoReady(uri, duration) },
                onCaptionChanged = { recordVlogViewModel.onCaptionChanged(it) },
                onRetake = { recordVlogViewModel.onRetake() },
                onPublish = { recordVlogViewModel.publishVlog(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<com.dailyvlog.app.ui.navigation.WatchVlogRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<com.dailyvlog.app.ui.navigation.WatchVlogRoute>()
            val watchState by watchVlogViewModel.uiState.collectAsStateWithLifecycle()
            com.dailyvlog.app.ui.watch.WatchVlogScreen(
                groupId = route.groupId,
                vlogId = route.vlogId,
                state = watchState,
                onLoadVlog = { gid, vid -> watchVlogViewModel.loadVlog(gid, vid) },
                onTogglePlayPause = { watchVlogViewModel.togglePlayPause() },
                onToggleMute = { watchVlogViewModel.toggleMute() },
                onToggleReaction = { watchVlogViewModel.toggleReaction(it) },
                onCommentInputChanged = { watchVlogViewModel.onCommentInputChanged(it) },
                onSubmitComment = { watchVlogViewModel.submitComment() },
                onOpenComments = { watchVlogViewModel.setShowCommentsSheet(true) },
                onDismissComments = { watchVlogViewModel.setShowCommentsSheet(false) },
                onOpenViewers = { watchVlogViewModel.setShowViewersSheet(true) },
                onDismissViewers = { watchVlogViewModel.setShowViewersSheet(false) },
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
