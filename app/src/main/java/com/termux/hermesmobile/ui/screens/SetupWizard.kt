package com.termux.hermesmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.termux.hermesmobile.network.*
import com.termux.hermesmobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * First-launch setup wizard shown when no server URL is configured.
 *
 * Auto-detects configuration from bundled assets or `/sdcard/hermes_config.json`
 * and allows the user to connect immediately or set up manually.
 *
 * @param onComplete Called when the user chooses to proceed to the main chat screen
 * @param onManualSetup Called when the user chooses to go to the settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizard(
    onComplete: () -> Unit,
    onManualSetup: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }

    // Detect config on launch — run once, not on every recomposition
    var detectedConfig by remember { mutableStateOf<Pair<HermesConfig, ConfigSource>?>(null) }
    var isDetecting by remember { mutableStateOf(true) }
    var isApplying by remember { mutableStateOf(false) }
    var isApplied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            detectedConfig = importHermesConfig(context)
        }
        isDetecting = false
    }

    // Auto-apply detected config so user doesn't have to tap a button for the default case.
    // Manual Setup remains available for users who want to override before connecting.
    LaunchedEffect(detectedConfig) {
        val config = detectedConfig?.first ?: return@LaunchedEffect
        if (isApplied || isApplying) return@LaunchedEffect
        isApplying = true
        applyHermesConfig(settingsRepo, config)
        isApplied = true
        isApplying = false
    }

    Scaffold(
        containerColor = HermesBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // App icon / logo area
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = HermesPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "H",
                        style = MaterialTheme.typography.displayMedium,
                        color = HermesPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome header
            Text(
                text = "Welcome to Hermes Mobile",
                style = MaterialTheme.typography.headlineMedium,
                color = HermesOnBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personal AI agent, on the go",
                style = MaterialTheme.typography.bodyMedium,
                color = HermesOnSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

// Config detection status card
            ConfigDetectionCard(
                detectedConfig = detectedConfig,
                isDetecting = isDetecting,
                isApplying = isApplying,
                isApplied = isApplied,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Continue button — enabled once config has been auto-applied.
            // We keep a manual path here too, so users who arrive at this screen
            // with no config can still bail out to settings instead of being stuck.
            val canContinue = detectedConfig != null && isApplied && !isApplying
            val continueDescription = when {
                isDetecting -> "Detecting configuration"
                isApplying -> "Applying configuration"
                isApplied -> "Continue to chat"
                else -> "No configuration detected"
            }

            Button(
                onClick = { onComplete() },
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = continueDescription },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HermesPrimary,
                    disabledContainerColor = HermesPrimary.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                when {
                    isDetecting || isApplying -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = HermesSurface,
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        Text(
                            "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            color = HermesSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual setup button
            OutlinedButton(
                onClick = onManualSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Open settings for manual configuration" },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HermesPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(HermesPrimary.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Manual Setup",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.weight(0.35f))
        }
    }
}

@Composable
private fun ConfigDetectionCard(
    detectedConfig: Pair<HermesConfig, ConfigSource>?,
    isDetecting: Boolean,
    isApplying: Boolean,
    isApplied: Boolean,
    modifier: Modifier = Modifier
) {
    val (config, source) = detectedConfig ?: Pair(null, null)
    val hasConfig = config != null

    val (icon, statusText, detailText, containerColor) = when {
        isDetecting -> listOf(
            Icons.Default.CheckCircle,
            "Detecting configuration...",
            "Scanning device and bundled files",
            HermesSurface
        )
        hasConfig && isApplying -> {
            val srcLabel = when (source) {
                is ConfigSource.Assets -> "Bundled defaults"
                is ConfigSource.DeviceFile -> "Device file: ${source.path}"
                ConfigSource.None -> null
                null -> null
            }
            listOf(
                Icons.Default.CheckCircle,
                "Applying configuration...",
                "Server: ${config.server_url}${srcLabel?.let { " · $it" } ?: ""}",
                HermesSurface
            )
        }
        hasConfig && isApplied -> {
            val srcLabel = when (source) {
                is ConfigSource.Assets -> "Bundled defaults"
                is ConfigSource.DeviceFile -> "Device file: ${source.path}"
                ConfigSource.None -> null
                null -> null
            }
            listOf(
                Icons.Default.CheckCircle,
                "Configuration applied ✓",
                "Server: ${config.server_url} · Model: ${config.default_model} · ${srcLabel ?: ""}",
                HermesPrimaryContainer
            )
        }
        hasConfig -> {
            val srcLabel = when (source) {
                is ConfigSource.Assets -> "Bundled defaults"
                is ConfigSource.DeviceFile -> "Device file: ${source.path}"
                ConfigSource.None -> null
                null -> null
            }
            listOf(
                Icons.Default.CheckCircle,
                "Configuration found",
                "Server: ${config.server_url}${srcLabel?.let { " · $it" } ?: ""}",
                HermesPrimaryContainer
            )
        }
        else -> listOf(
            Icons.Default.Warning,
            "No configuration found",
            "Place hermes_config.json on SD card or use Manual Setup",
            HermesSurfaceVariant
        )
    }

    // Unpack safely — list access in Compose doesn't use getter syntax
    @Suppress("UNCHECKED_CAST")
    val iconVal = (icon as? androidx.compose.ui.graphics.vector.ImageVector) ?: Icons.Default.CheckCircle
    val statusTextVal = statusText as String
    val detailTextVal = detailText as String
    val containerColorVal = containerColor as androidx.compose.ui.graphics.Color

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColorVal
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVal,
                contentDescription = null,
                tint = HermesPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = statusTextVal,
                    style = MaterialTheme.typography.titleSmall,
                    color = HermesOnSurface
                )
                Text(
                    text = detailTextVal,
                    style = MaterialTheme.typography.bodySmall,
                    color = HermesOnSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}