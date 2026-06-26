package com.termux.hermesmobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termux.hermesmobile.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val serverUrl: StateFlow<String> = settingsRepo.serverUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.HERMES_GATEWAY_URL)

    val apiKey: StateFlow<String> = settingsRepo.apiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private var currentAssistantId: String? = null

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        _error.value = null
        val userMsg = UiMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text.trim()
        )
        _messages.value = _messages.value + userMsg

        val assistantId = UUID.randomUUID().toString()
        currentAssistantId = assistantId
        _messages.value = _messages.value + UiMessage(
            id = assistantId,
            role = "assistant",
            content = "",
            isStreaming = true
        )

        _isLoading.value = true

        viewModelScope.launch {
            val client = HermesApiClient(
                baseUrl = serverUrl.value,
                apiKey = apiKey.value
            )

            val apiMessages = _messages.value
                .filter { it.role != "assistant" || !it.isStreaming }
                .takeLast(20)
                .map { ChatMessage(role = it.role, content = it.content) }

            client.streamChat(messages = apiMessages)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    finishStreaming(assistantId)
                }
                .collect { event ->
                    when (event) {
                        is HermesApiClient.StreamEvent.Content -> {
                            appendToMessage(assistantId, event.text)
                        }
                        is HermesApiClient.StreamEvent.ToolCall -> {
                            appendToolCall(assistantId, event.name, event.arguments)
                        }
                        is HermesApiClient.StreamEvent.Done -> {
                            finishStreaming(assistantId)
                        }
                        is HermesApiClient.StreamEvent.Error -> {
                            _error.value = event.message
                            finishStreaming(assistantId)
                        }
                    }
                }
        }
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch { settingsRepo.setServerUrl(url) }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch { settingsRepo.setApiKey(key) }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _error.value = null
    }

    fun dismissError() {
        _error.value = null
    }

    private fun appendToMessage(id: String, text: String) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(content = it.content + text) else it
        }
    }

    private fun appendToolCall(id: String, name: String, args: String) {
        _messages.value = _messages.value.map {
            if (it.id == id) {
                it.copy(
                    content = it.content + "\n\n🔧 `$name`",
                    toolCalls = it.toolCalls + UiToolCall(name, args)
                )
            } else it
        }
    }

    private fun finishStreaming(id: String) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(isStreaming = false) else it
        }
        _isLoading.value = false
    }
}
