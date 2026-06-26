package com.termux.hermesmobile.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ---- Request ----
@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val model: String = "hermes"
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

// ---- SSE Response ----
@Serializable
data class StreamChunk(
    val choices: List<Choice>? = null,
    val error: ErrorBody? = null
)

@Serializable
data class Choice(
    val delta: Delta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class Delta(
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDelta>? = null
)

@Serializable
data class ToolCallDelta(
    val index: Int? = null,
    val id: String? = null,
    val function: ToolFunction? = null
)

@Serializable
data class ToolFunction(
    val name: String? = null,
    val arguments: String? = null
)

@Serializable
data class ErrorBody(
    val message: String? = null,
    val type: String? = null
)

// ---- UI Model ----
data class UiMessage(
    val id: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val isStreaming: Boolean = false,
    val toolCalls: List<UiToolCall> = emptyList()
)

data class UiToolCall(
    val name: String,
    val arguments: String
)
