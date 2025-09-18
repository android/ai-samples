/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ai.samples.agentassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.ai.uicomponent.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.jvm.java

sealed interface GeminiMessageState {
    data object WaitingForMessage : GeminiMessageState
    data object Generating : GeminiMessageState
    data class Error(val errorMessage: String) : GeminiMessageState
}

val welcomeMessage = """
    Hi! I’m the Android AI Dev Assistant (AADA). I’m here to help you with integrating AI features into your Android apps.
    
    To start, what kind of AI functionality are you hoping to add to your application?
""".trimIndent()

sealed interface NavigationEvent {
    data object Idle : NavigationEvent
    data class NavigateToSample(val sampleRoute: String?) : NavigationEvent
}

data class AgentAssistantUiState(
    val navigationEvent: NavigationEvent = NavigationEvent.Idle,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = welcomeMessage,
            timestamp = System.currentTimeMillis(),
            isIncoming = true,
        )
    ),
    val geminiMessageState: GeminiMessageState = GeminiMessageState.WaitingForMessage,
)
class AgentAssistantViewModelFactory(
    private val samples: JsonArray
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgentAssistantViewModel::class.java)) {
            return AgentAssistantViewModel(samples) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AgentAssistantViewModel @Inject constructor(val samples: JsonArray) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentAssistantUiState())
    val uiState: StateFlow<AgentAssistantUiState> = _uiState.asStateFlow()

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            "gemini-2.5-flash",
            systemInstruction = content { text(systemInstruction) },
            tools = listOf(
                Tool.functionDeclarations(
                    listOf(
                        getSamplesFunctionDeclaration,
                        navigateToSampleFunctionDeclaration,
                    )
                ),
            ),
        )
    }

    private val chat = generativeModel.startChat()


    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                val userMessage = ChatMessage(
                    text = message,
                    timestamp = System.currentTimeMillis(),
                )
                _uiState.update {
                    it.copy(
                        messages = listOf(userMessage) + it.messages,
                        geminiMessageState = GeminiMessageState.Generating,
                    )
                }
                var response: GenerateContentResponse? = chat.sendMessage(message)
                while(response != null) {
                    response.text.takeIf { !it.isNullOrBlank() }?.let { text ->
                        val newMessage = ChatMessage(
                            text = text.trim(),
                            timestamp = System.currentTimeMillis(),
                            isIncoming = true,
                        )
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                messages = listOf(newMessage) + currentUiState.messages,
                                geminiMessageState = GeminiMessageState.WaitingForMessage
                            )
                        }
                    }

                    if (response.functionCalls.isNotEmpty()) {
                        val newMessage = ChatMessage(
                            text = "Executing function call(s): ${response.functionCalls.joinToString { it.name }}",
                            timestamp = System.currentTimeMillis(),
                            isIncoming = true,
                        )
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                messages = listOf(newMessage) + currentUiState.messages,
                                geminiMessageState = GeminiMessageState.WaitingForMessage
                            )
                        }
                        val functionResponses = mutableListOf<FunctionResponsePart>()
                        response.functionCalls.forEach { functionCall ->
                            val functionResponse = when (functionCall.name) {
                                "get_samples" -> {
                                    val json = buildJsonObject {
                                        put("result", samples)
                                    }
                                    FunctionResponsePart("get_samples", json)
                                }
                                "navigate_to_sample" -> {

                                    _uiState.update { currentUiState ->
                                        val jsonPrimitive = functionCall.args["sample_route"] as JsonPrimitive
                                        val route = jsonPrimitive.content
                                        currentUiState.copy(
                                            navigationEvent = NavigationEvent.NavigateToSample(route)
                                        )
                                    }
                                    null
                                }

                                else -> null
                            }
                            functionResponse?.let { functionResponses.add(it) }
                        }
                        if(functionResponses.isNotEmpty()) {
                            response = chat.sendMessage(
                                content("function") {
                                    functionResponses.forEach { part(it) }
                                }
                            )
                        } else {
                            response = null
                        }
                    } else {
                        response = null
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(geminiMessageState = GeminiMessageState.Error(e.localizedMessage ?: "Something went wrong, try again"))
                }
            }
        }
    }

    fun onNavigationHandled() {
        // Resets the state, ensuring the event is "consumed"
        _uiState.update { it.copy(navigationEvent = NavigationEvent.Idle) }
    }

    fun navigationFailed(e: Exception) {
        _uiState.update {
            it.copy(geminiMessageState = GeminiMessageState.Error(e.localizedMessage ?: "Something went wrong, try again"))
        }
    }

    fun dismissError() {
        _uiState.update {
            it.copy(geminiMessageState = GeminiMessageState.WaitingForMessage)
        }
    }
}
