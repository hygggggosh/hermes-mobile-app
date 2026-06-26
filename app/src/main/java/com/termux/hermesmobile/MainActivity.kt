package com.termux.hermesmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.termux.hermesmobile.network.SettingsRepository
import com.termux.hermesmobile.ui.screens.ChatScreen
import com.termux.hermesmobile.ui.screens.SettingsScreen
import com.termux.hermesmobile.ui.screens.SetupWizard
import com.termux.hermesmobile.ui.theme.HermesMobileTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HermesMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isConfigured by remember { mutableStateOf(false) }
                    var isLoading by remember { mutableStateOf(true) }
                    var showSettings by remember { mutableStateOf(false) }

                    // Check if app is already configured on first launch
                    LaunchedEffect(Unit) {
                        val settingsRepo = SettingsRepository(this@MainActivity)
                        isConfigured = settingsRepo.isConfigured()
                        isLoading = false
                    }

                    when {
                        isLoading -> {
                            // Brief loading state while checking configuration
                        }
                        showSettings -> {
                            SettingsScreen(onBack = { showSettings = false })
                        }
                        !isConfigured -> {
                            SetupWizard(
                                onComplete = {
                                    isConfigured = true
                                },
                                onManualSetup = {
                                    showSettings = true
                                }
                            )
                        }
                        else -> {
                            ChatScreen(onSettingsClick = { showSettings = true })
                        }
                    }
                }
            }
        }
    }
}