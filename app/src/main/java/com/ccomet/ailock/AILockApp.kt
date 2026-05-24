package com.ccomet.ailock

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ccomet.ailock.ui.AILockViewModel
import com.ccomet.ailock.ui.components.FloatingBottomNav
import com.ccomet.ailock.ui.home.HomeScreen
import com.ccomet.ailock.ui.onboarding.OnboardingScreen
import com.ccomet.ailock.ui.records.RecordsScreen
import com.ccomet.ailock.ui.restrictions.AppPickerScreen
import com.ccomet.ailock.ui.restrictions.RestrictionEditScreen
import com.ccomet.ailock.ui.restrictions.RestrictionsScreen
import com.ccomet.ailock.ui.settings.PermissionManagementScreen
import com.ccomet.ailock.ui.settings.ProfileEditScreen
import com.ccomet.ailock.ui.settings.SettingsScreen

@Composable
fun AILockApp(
    navController: NavHostController = rememberNavController(),
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AILockViewModel = viewModel(factory = AILockViewModel.Factory(application))
    val uiState by viewModel.uiState.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshPermissions()
    }
    val openNotification: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.openNotificationSettings()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    val mainRoutes = setOf(Routes.Home, Routes.Records, Routes.Restrictions, Routes.Settings)
    Scaffold(
        bottomBar = {
            if (currentRoute in mainRoutes) {
                FloatingBottomNav(
                    currentRoute = currentRoute ?: Routes.Home,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (uiState.onboardingCompleted) Routes.Home else Routes.Onboarding,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = {
                val forward = routeIndex(targetState.destination.route) >= routeIndex(initialState.destination.route)
                slideInHorizontally(animationSpec = tween(240)) { width -> if (forward) width else -width }
            },
            exitTransition = {
                val forward = routeIndex(targetState.destination.route) >= routeIndex(initialState.destination.route)
                slideOutHorizontally(animationSpec = tween(240)) { width -> if (forward) -width else width }
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(220)) { width -> -width }
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(220)) { width -> width }
            },
        ) {
            composable(Routes.Onboarding) {
                OnboardingScreen(
                    uiState = uiState,
                    onNext = viewModel::nextOnboardingStep,
                    onBack = viewModel::previousOnboardingStep,
                    onUsagePermission = viewModel::openUsageAccessSettings,
                    onOverlayPermission = viewModel::openOverlaySettings,
                    onAccessibilityPermission = viewModel::openAccessibilitySettings,
                    onNotificationPermission = openNotification,
                    onRefreshPermissions = viewModel::refreshPermissions,
                    onDebugContinue = viewModel::skipPermissionGateForDebug,
                    onProfileChange = viewModel::updateProfileDraft,
                    onSaveProfileAndContinue = viewModel::saveProfileAndContinue,
                    onFinish = {
                        viewModel.finishOnboarding()
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Home) {
                HomeScreen(uiState)
            }
            composable(Routes.Records) {
                RecordsScreen(uiState)
            }
            composable(Routes.Restrictions) {
                RestrictionsScreen(
                    uiState = uiState,
                    onAdd = {
                        viewModel.beginAddRestriction()
                        navController.navigate(Routes.RestrictionAdd)
                    },
                    onEdit = { id ->
                        viewModel.beginEditRestriction(id)
                        navController.navigate("${Routes.RestrictionEdit}/$id")
                    },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    uiState = uiState,
                    onProfile = {
                        viewModel.startProfileEditing()
                        navController.navigate(Routes.Profile)
                    },
                    onPermissions = { navController.navigate(Routes.Permissions) },
                    onRestartOnboarding = {
                        viewModel.restartOnboarding()
                        navController.navigate(Routes.Onboarding) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    },
                    onBaseUrlChange = viewModel::updateBackendBaseUrlInput,
                    onSaveBaseUrl = viewModel::saveBackendBaseUrl,
                    onMockChange = viewModel::setMockAiMode,
                    onStartMonitor = viewModel::startUsageMonitor,
                )
            }
            composable(Routes.RestrictionAdd) {
                RestrictionEditScreen(
                    uiState = uiState,
                    isEditing = false,
                    onBack = { navController.popBackStack() },
                    onPickApp = { navController.navigate(Routes.AppPicker) },
                    onReasonPreset = viewModel::updateReasonPreset,
                    onReasonCustom = viewModel::updateReasonCustom,
                    onRestrictionType = viewModel::updateRestrictionType,
                    onToggleDay = viewModel::toggleDay,
                    onDailyLimit = viewModel::updateDailyLimit,
                    onToggleAdvanced = viewModel::toggleAdvancedSchedule,
                    onAdvancedLimit = viewModel::updateAdvancedLimit,
                    onSave = { viewModel.saveDraft { navController.popBackStack(Routes.Restrictions, false) } },
                    onDelete = {},
                )
            }
            composable("${Routes.RestrictionEdit}/{id}") {
                RestrictionEditScreen(
                    uiState = uiState,
                    isEditing = true,
                    onBack = { navController.popBackStack() },
                    onPickApp = { navController.navigate(Routes.AppPicker) },
                    onReasonPreset = viewModel::updateReasonPreset,
                    onReasonCustom = viewModel::updateReasonCustom,
                    onRestrictionType = viewModel::updateRestrictionType,
                    onToggleDay = viewModel::toggleDay,
                    onDailyLimit = viewModel::updateDailyLimit,
                    onToggleAdvanced = viewModel::toggleAdvancedSchedule,
                    onAdvancedLimit = viewModel::updateAdvancedLimit,
                    onSave = { viewModel.saveDraft { navController.popBackStack(Routes.Restrictions, false) } },
                    onDelete = { viewModel.deleteDraft { navController.popBackStack(Routes.Restrictions, false) } },
                )
            }
            composable(Routes.AppPicker) {
                AppPickerScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onQuery = viewModel::updateAppQuery,
                    onSelect = viewModel::selectApp,
                    onConfirm = { navController.popBackStack() },
                )
            }
            composable(Routes.Profile) {
                ProfileEditScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onProfileChange = viewModel::updateProfileDraft,
                    onSave = {
                        viewModel.saveProfile()
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.Permissions) {
                PermissionManagementScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onUsagePermission = viewModel::openUsageAccessSettings,
                    onOverlayPermission = viewModel::openOverlaySettings,
                    onAccessibilityPermission = viewModel::openAccessibilitySettings,
                    onNotificationPermission = openNotification,
                    onRefresh = viewModel::refreshPermissions,
                )
            }
        }
    }
}

private fun routeIndex(route: String?): Int = when (route?.substringBefore('/')) {
    Routes.Home -> 0
    Routes.Records -> 1
    Routes.Restrictions -> 2
    Routes.Settings -> 3
    Routes.RestrictionAdd, Routes.RestrictionEdit, Routes.AppPicker -> 4
    Routes.Profile, Routes.Permissions -> 5
    Routes.Onboarding -> -1
    else -> 0
}

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Records = "records"
    const val Restrictions = "restrictions"
    const val Settings = "settings"
    const val RestrictionAdd = "restriction_add"
    const val RestrictionEdit = "restriction_edit"
    const val AppPicker = "app_picker"
    const val Profile = "profile"
    const val Permissions = "permissions"
}
