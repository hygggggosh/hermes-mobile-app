package com.termux.hermesmobile.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.*
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the Hermes Agent SSE API.
 *
 * Supports both streaming (SSE) and non-streaming chat completions.
 * Instances are lightweight — create one per request or reuse a shared instance.
 *
 * @param baseUrl The base URL of the Hermes API gateway (e.g. "http://127.0.0.1:8642")
 * @param apiKey Optional API key for authenticated endpoints
 */
class HermesApiClient(
    private val baseUrl: String = "http://127.0.0.1:8642",
    private val apiKey: String = ""
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Stream event emitted by [streamChat]. */
    sealed class StreamEvent {
        data class Content(val text: String) : StreamEvent()
        data class ToolCall(val name: String, val arguments: String) : StreamEvent()
        data class Done(val fullContent: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    /**
     * Streams chat completions from the Hermes API using Server-Sent Events.
     *
     * The returned [Flow] emits zero or more [StreamEvent.Content] events,
     * followed by either [StreamEvent.Done] (on success) or [StreamEvent.Error].
     *
     * @param messages List of conversation messages (user/assistant/system roles)
     * @param systemPrompt Optional system prompt prepended to the request
     */
    fun streamChat(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<StreamEvent> = callbackFlow {
        val allMessages = buildList {
            if (systemPrompt != null) {
                add(ChatMessage(role = "system", content = systemPrompt))
            }
            addAll(messages)
        }

        val requestBody = ChatRequest(messages = allMessages)
        val jsonBody = json.encodeToString(ChatRequest.serializer(), requestBody)

        Log.d("HermesAPI", "Sending to $baseUrl/v1/chat/completions")
        Log.d("HermesAPI", "Body preview: ${jsonBody.take(200)}...")

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Content-Type", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val sseListener = object : EventSourceListener() {
            private val fullContent = StringBuilder()
            private val currentToolCalls = mutableMapOf<Int, ToolCallAccumulator>()

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                // Guard against events emitted after close
                if (isClosedForSend) return

                if (data == "[DONE]") {
                    trySend(StreamEvent.Done(fullContent.toString()))
                    close()
                    return
                }

                try {
                    val chunk = json.decodeFromString(StreamChunk.serializer(), data)

                    // API-level error in response body
                    chunk.error?.message?.let { errMsg ->
                        trySend(StreamEvent.Error(errMsg))
                        close()
                        return
                    }

                    val delta = chunk.choices?.firstOrNull()?.delta

                    // Text content
                    delta?.content?.let { text ->
                        fullContent.append(text)
                        trySend(StreamEvent.Content(text))
                    }

                    // Tool calls
                    delta?.toolCalls?.forEach { tc ->
                        val idx = tc.index ?: return@forEach
                        val acc = currentToolCalls.getOrPut(idx) { ToolCallAccumulator() }
                        tc.id?.let { acc.id = it }
                        tc.function?.name?.let { acc.name = it }
                        tc.function?.arguments?.let { acc.arguments.append(it) }

                        if (acc.name.isNotBlank() && acc.arguments.isNotEmpty()) {
                            trySend(StreamEvent.ToolCall(acc.name, acc.arguments.toString()))
                            currentToolCalls.remove(idx)
                        }
                    }
                } catch (e: Exception) {
                    // Malformed JSON chunk — skip it rather than failing the stream
                    Log.w("HermesAPI", "Parse chunk skipped: ${e.message}")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                if (isClosedForSend) return

                val msg = when {
                    response?.code == 401 -> "Authentication failed — check API key"
                    response?.code == 403 -> "Access forbidden — is the API key valid?"
                    response?.code == 404 -> "Endpoint not found — is Hermes API server running?"
                    response?.code == 422 -> "Invalid request — check server URL and model settings"
                    (response?.code ?: 0) in 500..599 -> "Server error (HTTP ${response?.code}) — try again shortly"
                    t is IOException -> "Network error: ${t.message?.replace(Regex("caused by.*"), "").orEmpty().trim()}"
                    t != null -> "Connection failed: ${t.message}"
                    else -> "Unknown error"
                }
                Log.e("HermesAPI", "SSE failure: $msg (HTTP ${response?.code})", t)
                trySend(StreamEvent.Error(msg))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client)
            .newEventSource(request, sseListener)

        awaitClose { eventSource.cancel() }
    }

    /**
     * Non-streaming chat completion — waits for the full response.
     *
     * Suitable for short requests where streaming is not needed.
     */
    fun nonStreamingChat(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<StreamEvent> = callbackFlow {
        val allMessages = buildList {
            if (systemPrompt != null) {
                add(ChatMessage(role = "system", content = systemPrompt))
            }
            addAll(messages)
        }

        val requestBody = ChatRequest(messages = allMessages, stream = false)
        val jsonBody = json.encodeToString(ChatRequest.serializer(), requestBody)

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Content-Type", "application/json")
            .apply { if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey") }
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    trySend(StreamEvent.Error("HTTP ${response.code}: $body"))
                    close()
                    return@callbackFlow
                }

                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    trySend(StreamEvent.Error("Empty response from server"))
                    close()
                    return@callbackFlow
                }

                try {
                    val chunk = json.decodeFromString(StreamChunk.serializer(), body)
                    chunk.error?.message?.let { errMsg ->
                        trySend(StreamEvent.Error(errMsg))
                        close()
                        return@callbackFlow
                    }

                    val content = chunk.choices
                        ?.firstOrNull()
                        ?.delta
                        ?.content
                        ?: run {
                            // Fallback: extract content from raw JSON as last resort
                            body.substringAfter("\"content\":\"").substringBefore("\"").takeIf {
                                it != body && it.isNotBlank()
                            } ?: body
                        }

                    trySend(StreamEvent.Content(content))
                    trySend(StreamEvent.Done(content))
                } catch (e: Exception) {
                    trySend(StreamEvent.Error("Failed to parse response: ${e.message}"))
                }
            }
        } catch (e: IOException) {
            trySend(StreamEvent.Error("Network error: ${e.message}"))
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e.message ?: "Request failed"))
        }
        close()
    }

    private class ToolCallAccumulator {
        var id: String = ""
        var name: String = ""
        val arguments = StringBuilder()
    }
}
