package com.ccomet.ailock

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.ccomet.ailock.ui.theme.AppBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BottomBarAnimationMillis = 150
private const val NavigateAfterBottomBarHideMillis = 90L

@Composable
fun AILockApp(
    navController: NavHostController = rememberNavController(),
) {
    val application = LocalContext.current.applicationContext as Application
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: AILockViewModel = viewModel(factory = AILockViewModel.Factory(application))
    val uiState by viewModel.uiState.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var bottomNavHidden by remember { mutableStateOf(false) }
    var appPickerAfterAdd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mainRoutes = setOf(Routes.HOME, Routes.RECORDS, Routes.RESTRICTIONS, Routes.SETTINGS)
    fun navigateAfterBottomBarHide(block: () -> Unit) {
        bottomNavHidden = true
        scope.launch {
            delay(NavigateAfterBottomBarHideMillis)
            block()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute in mainRoutes && currentRoute != Routes.RESTRICTIONS) {
            bottomNavHidden = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (uiState.onboardingCompleted) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    uiState = uiState,
                    onOpenNextPermission = viewModel::openNextMissingPermission,
                    onUsagePermission = viewModel::openUsageAccessSettings,
                    onOverlayPermission = viewModel::openOverlaySettings,
                    onAccessibilityPermission = viewModel::openAccessibilitySettings,
                    onNotificationPermission = openNotification,
                    onBatteryPermission = viewModel::openBatteryOptimizationSettings,
                    onRefreshPermissions = viewModel::refreshPermissions,
                    onNext = viewModel::nextOnboardingStep,
                    onPrevious = viewModel::previousOnboardingStep,
                    onProfileChange = viewModel::updateProfileDraft,
                    onSaveProfileAndContinue = viewModel::saveProfileAndContinue,
                    onAppQuery = viewModel::updateAppQuery,
                    onToggleApp = viewModel::toggleOnboardingApp,
                    onSaveAppsAndContinue = viewModel::saveOnboardingAppsAndContinue,
                    onFinish = {
                        viewModel.saveProfileAndFinish()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(uiState)
            }
            composable(Routes.RECORDS) {
                RecordsScreen(uiState)
            }
            composable(Routes.RESTRICTIONS) {
                RestrictionsScreen(
                    uiState = uiState,
                    onDeleteModeChange = { bottomNavHidden = it },
                    onAdd = {
                        navigateAfterBottomBarHide {
                            viewModel.beginAddRestriction()
                            appPickerAfterAdd = true
                            navController.navigate(Routes.APP_PICKER)
                        }
                    },
                    onEdit = { id ->
                        navigateAfterBottomBarHide {
                            viewModel.beginEditRestriction(id)
                            navController.navigate("${Routes.RESTRICTION_EDIT}/$id")
                        }
                    },
                    onDeleteApp = viewModel::deleteLockedApp,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    uiState = uiState,
                    onProfile = {
                        navigateAfterBottomBarHide {
                            viewModel.startProfileEditing()
                            navController.navigate(Routes.PROFILE)
                        }
                    },
                    onPermissions = {
                        navigateAfterBottomBarHide {
                            navController.navigate(Routes.PERMISSIONS)
                        }
                    },
                    onRestartOnboarding = {
                        navigateAfterBottomBarHide {
                            viewModel.restartOnboarding()
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable(Routes.RESTRICTION_ADD) {
                RestrictionEditScreen(
                    uiState = uiState,
                    isEditing = false,
                    onBack = { navController.popBackStack() },
                    onPickApp = { navController.navigate(Routes.APP_PICKER) },
                    onDailyLimit = viewModel::updateDailyLimit,
                    onSave = { viewModel.saveDraft { navController.popBackStack(Routes.RESTRICTIONS, false) } },
                    onDelete = {},
                )
            }
            composable("${Routes.RESTRICTION_EDIT}/{id}") {
                RestrictionEditScreen(
                    uiState = uiState,
                    isEditing = true,
                    onBack = { navController.popBackStack() },
                    onPickApp = { navController.navigate(Routes.APP_PICKER) },
                    onDailyLimit = viewModel::updateDailyLimit,
                    onSave = { viewModel.saveDraft { navController.popBackStack(Routes.RESTRICTIONS, false) } },
                    onDelete = { viewModel.deleteDraft { navController.popBackStack(Routes.RESTRICTIONS, false) } },
                )
            }
            composable(Routes.APP_PICKER) {
                AppPickerScreen(
                    uiState = uiState,
                    onBack = {
                        appPickerAfterAdd = false
                        navController.popBackStack()
                    },
                    onQuery = viewModel::updateAppQuery,
                    onSelect = viewModel::selectApp,
                    onConfirm = {
                        if (appPickerAfterAdd) {
                            appPickerAfterAdd = false
                            navController.navigate(Routes.RESTRICTION_ADD) {
                                popUpTo(Routes.RESTRICTIONS)
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable(Routes.PROFILE) {
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
            composable(Routes.PERMISSIONS) {
                PermissionManagementScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onUsagePermission = viewModel::openUsageAccessSettings,
                    onOverlayPermission = viewModel::openOverlaySettings,
                    onAccessibilityPermission = viewModel::openAccessibilitySettings,
                    onNotificationPermission = openNotification,
                    onBatteryPermission = viewModel::openBatteryOptimizationSettings,
                    onRefresh = viewModel::refreshPermissions,
                )
            }
        }
        if (currentRoute in mainRoutes) {
            AnimatedVisibility(
                visible = !bottomNavHidden,
                enter = slideInVertically(
                    animationSpec = tween(BottomBarAnimationMillis),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(BottomBarAnimationMillis),
                    targetOffsetY = { it },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(132.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.42f to AppBackground.copy(alpha = 0.68f),
                                    1f to AppBackground,
                                ),
                            ),
                    )
                    FloatingBottomNav(
                        currentRoute = currentRoute ?: Routes.HOME,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RECORDS = "records"
    const val RESTRICTIONS = "restrictions"
    const val SETTINGS = "settings"
    const val RESTRICTION_ADD = "restriction_add"
    const val RESTRICTION_EDIT = "restriction_edit"
    const val APP_PICKER = "app_picker"
    const val PROFILE = "profile"
    const val PERMISSIONS = "permissions"
}

