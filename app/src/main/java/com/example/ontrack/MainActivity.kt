package com.example.ontrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ontrack.ui.MainViewModel
import com.example.ontrack.ui.MainViewModelFactory
import com.example.ontrack.ui.createsystem.CreateSystemScreen
import com.example.ontrack.ui.createsystem.CreateSystemViewModel
import com.example.ontrack.ui.createsystem.CreateSystemViewModelFactory
import com.example.ontrack.ui.editsystem.EditSystemScreen
import com.example.ontrack.ui.editsystem.EditSystemViewModel
import com.example.ontrack.ui.editsystem.EditSystemViewModelFactory
import com.example.ontrack.ui.activity.ActivityScreen
import com.example.ontrack.ui.activity.ActivityViewModel
import com.example.ontrack.ui.activity.ActivityViewModelFactory
import com.example.ontrack.ui.home.HomeScreen
import com.example.ontrack.ui.home.HomeViewModel
import com.example.ontrack.ui.home.HomeViewModelFactory
import com.example.ontrack.ui.onboarding.OnboardingScreen
import com.example.ontrack.ui.theme.OnTrackTheme
import com.example.ontrack.ui.tracker.TrackerScreen
import com.example.ontrack.ui.tracker.TrackerViewModel
import com.example.ontrack.ui.tracker.TrackerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as OnTrackApplication
        val startPage = intent.getIntExtra("start_page", 0)

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(application.userPreferences, startPage)
            )
            val userName by mainViewModel.userName.collectAsState(initial = "")
            val trackTimeEnabled by mainViewModel.trackTimeEnabled.collectAsState(initial = true)
            val skipOnboardingEnabled by mainViewModel.skipOnboardingEnabled.collectAsState(initial = false)
            val initialPageToUse by mainViewModel.initialPageToUse.collectAsState(initial = 0)

            OnTrackTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                val homeViewModel: HomeViewModel = viewModel(
                                    factory = HomeViewModelFactory(
                                        database = application.database,
                                        streakManager = application.streakManager,
                                        userPreferences = application.userPreferences
                                    )
                                )
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    userName = userName,
                                    trackTimeEnabled = trackTimeEnabled,
                                    onTrackTimeEnabledChange = { mainViewModel.setTrackTimeEnabled(it) },
                                    skipOnboardingEnabled = skipOnboardingEnabled,
                                    onSkipOnboardingEnabledChange = { mainViewModel.setSkipOnboardingEnabled(it) },
                                    initialPage = initialPageToUse,
                                    onConsumeInitialPage = { mainViewModel.consumeInitialPage() },
                                    onCreateSystemClick = { navController.navigate("create_system") },
                                    onOpenSystemClick = { systemId ->
                                        navController.navigate("tracker/$systemId")
                                    },
                                    onActivityClick = { systemId ->
                                        navController.navigate("activity/$systemId")
                                    },
                                    onEditSystemClick = { systemId ->
                                        navController.navigate("edit_system/$systemId")
                                    },
                                    onStartTimerFromToday = { systemId, habitId, habitTitle, totalSeconds ->
                                        homeViewModel.startTimerFromToday(systemId, habitId, habitTitle, totalSeconds)
                                    }
                                )
                            }
                            composable("create_system") {
                                val createViewModel: CreateSystemViewModel = viewModel(
                                    factory = CreateSystemViewModelFactory(application.database)
                                )
                                CreateSystemScreen(
                                    viewModel = createViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "edit_system/{systemId}",
                                arguments = listOf(
                                    navArgument("systemId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val editSystemId = backStackEntry.arguments?.getLong("systemId") ?: 0L
                                val editViewModel: EditSystemViewModel = viewModel(
                                    factory = EditSystemViewModelFactory(
                                        systemId = editSystemId,
                                        systemDao = application.database.systemDao(),
                                        habitDao = application.database.habitDao()
                                    )
                                )
                                EditSystemScreen(
                                    viewModel = editViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "tracker/{systemId}",
                                arguments = listOf(
                                    navArgument("systemId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val systemId = backStackEntry.arguments?.getLong("systemId") ?: 0L
                                val trackerViewModel: TrackerViewModel = viewModel(
                                    factory = TrackerViewModelFactory(
                                        database = application.database,
                                        streakManager = application.streakManager,
                                        systemId = systemId
                                    )
                                )
                                TrackerScreen(
                                    viewModel = trackerViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onActivityClick = { navController.navigate("activity/$systemId") }
                                )
                            }
                            composable(
                                route = "activity/{systemId}",
                                arguments = listOf(
                                    navArgument("systemId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val systemId = backStackEntry.arguments?.getLong("systemId") ?: 0L
                                val activityViewModel: ActivityViewModel = viewModel(
                                    factory = ActivityViewModelFactory(
                                        systemId = systemId,
                                        systemDao = application.database.systemDao(),
                                        habitDao = application.database.habitDao(),
                                        habitLogDao = application.database.habitLogDao(),
                                        streakManager = application.streakManager
                                    )
                                )
                                ActivityScreen(
                                    viewModel = activityViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                }
            }
        }
    }
}
