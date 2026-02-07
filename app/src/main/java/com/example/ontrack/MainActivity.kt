package com.example.ontrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ontrack.ui.MainViewModel
import com.example.ontrack.ui.MainViewModelFactory
import com.example.ontrack.ui.components.LoadingScreen
import com.example.ontrack.ui.createsystem.CreateSystemScreen
import com.example.ontrack.ui.createsystem.CreateSystemViewModel
import com.example.ontrack.ui.createsystem.CreateSystemViewModelFactory
import com.example.ontrack.ui.home.HomeScreen
import com.example.ontrack.ui.home.HomeViewModel
import com.example.ontrack.ui.home.HomeViewModelFactory
import com.example.ontrack.ui.onboarding.OnboardingScreen
import com.example.ontrack.ui.theme.OnTrackTheme
import com.example.ontrack.ui.tracker.TrackerScreen
import com.example.ontrack.ui.tracker.TrackerViewModel
import com.example.ontrack.ui.tracker.TrackerViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as OnTrackApplication
        val mainViewModel: MainViewModel = androidx.lifecycle.ViewModelProvider(
            this,
            MainViewModelFactory(application.userPreferences)
        )[MainViewModel::class.java]

        setContent {
            OnTrackTheme {
                var isLoading by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1_200)
                    isLoading = false
                }

                if (isLoading) {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
                } else {
                    val isFirstLaunch by mainViewModel.isFirstLaunch.collectAsState(initial = true)
                    val userName by mainViewModel.userName.collectAsState(initial = "")

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isFirstLaunch) {
                        OnboardingScreen(
                            onStartClick = { name -> mainViewModel.completeOnboarding(name) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
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
                                        streakManager = application.streakManager
                                    )
                                )
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    userName = userName,
                                    onCreateSystemClick = { navController.navigate("create_system") },
                                    onSystemClick = { systemId ->
                                        navController.navigate("tracker/$systemId")
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
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
