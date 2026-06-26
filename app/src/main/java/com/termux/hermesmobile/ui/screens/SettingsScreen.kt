package com.termux.hermesmobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termux.hermesmobile.ui.theme.*
import com.termux.hermesmobile.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()

    var urlText by remember(serverUrl) { mutableStateOf(serverUrl) }
    var keyText by remember(apiKey) { mutableStateOf(apiKey) }
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Server section
            Text(
                "Hermes Server",
                style = MaterialTheme.typography.titleMedium,
                color = HermesPrimary
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HermesSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://127.0.0.1:8642") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HermesPrimary,
                            unfocusedBorderColor = HermesSurfaceVariant,
                            cursorColor = HermesPrimary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hermes gateway running on Termux (default 8642).\nOllama fallback: http://127.0.0.1:11434",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesOnSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.updateServerUrl(urlText) },
                        colors = ButtonDefaults.buttonColors(containerColor = HermesPrimary)
                    ) {
                        Text("Save Server URL")
                    }
                }
            }

            // API Key section
            Text(
                "API Key",
                style = MaterialTheme.typography.titleMedium,
                color = HermesPrimary
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HermesSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showKey) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    "Toggle visibility"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HermesPrimary,
                            unfocusedBorderColor = HermesSurfaceVariant,
                            cursorColor = HermesPrimary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Leave blank if using local models via Ollama.\nRequired for cloud providers (DeepSeek, OpenAI, etc.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesOnSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.updateApiKey(keyText) },
                        colors = ButtonDefaults.buttonColors(containerColor = HermesPrimary)
                    ) {
                        Text("Save API Key")
                    }
                }
            }

            // Setup guide
            Text(
                "Setup Guide",
                style = MaterialTheme.typography.titleMedium,
                color = HermesSecondary
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HermesSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. Install Termux on your phone",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "2. In Termux, run:\n   pkg install nodejs-lts\n   npm install -g hermes-agent",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesOnSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "3. Start Hermes API server:\n   hermes gateway run",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesOnSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "4. Set Server URL to http://127.0.0.1:8642",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesOnSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "5. Enter your API key above and start chatting!",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesOnSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
