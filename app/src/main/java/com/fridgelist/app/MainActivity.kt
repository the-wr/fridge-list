package com.fridgelist.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fridgelist.app.ui.main.MainScreen
import com.fridgelist.app.ui.setup.SetupViewModel
import com.fridgelist.app.ui.theme.FridgeListTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen always on (kiosk mode)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            FridgeListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val setupViewModel: SetupViewModel = hiltViewModel()
                    val isSetupComplete by setupViewModel.isSetupComplete.collectAsState(initial = null)

                    if (isSetupComplete != null) {
                        NavHost(
                            navController = navController,
                            startDestination = if (isSetupComplete == true) "main" else "setup"
                        ) {
                            composable("main") {
                                MainScreen(
                                    onNavigateToSetup = { navController.navigate("setup") },
                                    onNavigateToSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("setup") {
                                com.fridgelist.app.ui.setup.SetupScreen(
                                    onSetupComplete = {
                                        navController.navigate("main") {
                                            popUpTo("setup") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("settings") {
                                com.fridgelist.app.ui.settings.SettingsScreen(
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
