/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetpacker.feature.booking_assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Duration
import com.agui.client.agent.HttpAgent
import com.agui.client.agent.HttpAgentConfig
import com.agui.core.types.BaseEvent
import com.agui.core.types.RunAgentInput
import com.agui.core.types.UserMessage
import com.agui.core.types.TextInputContent
import com.agui.core.types.TextMessageStartEvent
import com.agui.core.types.TextMessageContentEvent
import com.agui.core.types.TextMessageEndEvent
import com.example.jetpacker.data.itinerary.EventDao
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

// A2UI imports
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.processor.A2uiJsonMessageParser
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiFunctionCallAction

@HiltViewModel
class BookingAssistantViewModel @Inject constructor(
    private val eventDao: EventDao
) : ViewModel() {
    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    private val _activeSurfaces = MutableStateFlow<List<A2uiSurfaceModel>>(emptyList())
    val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> = _activeSurfaces.asStateFlow()

    private val lastUpdatedSurfaces = MutableStateFlow<List<String>>(emptyList())

    private val catalog = bookingAssistantCatalog()

    private val actionInterceptor = object : A2uiActionInterceptor {
        override suspend fun onInterceptAction(request: A2uiUserAction): A2uiUserAction? {
            handleAction(request)
            return request
        }
    }

    private val messageProcessor = A2uiMessageProcessor(
        catalogs = listOf(catalog),
        interceptors = listOf(actionInterceptor)
    )
    private val parser = A2uiJsonMessageParser { json -> AndroidA2uiJsonReader(json) }
    private var currentThreadId: String? = null

    init {
        viewModelScope.launch {
            messageProcessor.collectMessages()
        }
    }

    private val agUiJson = Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                readTimeout(Duration.ZERO)
            }
        }
        install(SSE)
        install(ContentNegotiation) {
            json(agUiJson)
        }
    }

    fun startBookingChat(tripId: String) {
        viewModelScope.launch {
            try {
                _events.value = listOf("Loading itinerary events from database...")
                val eventsList = eventDao.getEventsForTrip(tripId).first()

                val itineraryJson = buildJsonObject {
                    put("events", JsonArray(eventsList.map { event ->
                        buildJsonObject {
                            put("id", event.id)
                            put("type", event.type.name)
                            put("timestamp", event.timestamp)
                            put("title", event.title)
                            put("location", event.location)
                            put("description", event.description ?: "")
                            put("extraInfo", event.extraInfo ?: "")
                        }
                    }))
                }.toString()

                _events.value = _events.value + "Connecting to local booking server at http://10.0.2.2:8000..."
                
                val emptyJsonObject = JsonObject(emptyMap())
                val threadId = UUID.randomUUID().toString()
                currentThreadId = threadId
                
                val config = HttpAgentConfig(
                    "booking-assistant", // 1: agentId
                    "Booking Assistant Agent", // 2: description
                    threadId, // 3: threadId
                    emptyList(), // 4: initialMessages
                    emptyJsonObject, // 5: initialState
                    false, // 6: debug
                    "http://localhost:8000", // 7: url
                    emptyMap(), // 8: headers
                    30000L, // 9: requestTimeout
                    10000L // 10: connectTimeout
                )
                val agent = HttpAgent(config, httpClient)

                // Collect active surfaces combined with sorting order
                viewModelScope.launch {
                    messageProcessor.activeSurfaces
                        .combine(lastUpdatedSurfaces) { surfaces, order ->
                            surfaces.sortedBy { surface ->
                                val id = (surface as? A2uiCoreSurfaceModel)?.id ?: ""
                                val index = order.indexOf(id)
                                if (index == -1) Int.MAX_VALUE else index
                            }
                        }
                        .collect { sortedSurfaces ->
                            android.util.Log.d("BookingAssistant", "Active surfaces updated. Count=${sortedSurfaces.size}, Surfaces=${sortedSurfaces.map { (it as? A2uiCoreSurfaceModel)?.id ?: "" }}")
                            _events.value = _events.value + "Active surfaces updated. Count=${sortedSurfaces.size}"
                            _activeSurfaces.value = sortedSurfaces
                        }
                }



                val runId = UUID.randomUUID().toString()
                val userMessage = UserMessage(
                    id = UUID.randomUUID().toString(),
                    content = itineraryJson,
                    name = "user",
                    contentParts = listOf(TextInputContent(itineraryJson))
                )
                
                val input = RunAgentInput(
                    threadId,
                    runId,
                    "",
                    emptyJsonObject,
                    listOf(userMessage)
                )

                var currentMessageId: String? = null
                val messageBuffer = StringBuilder()

                agent.runAgentObservable(input)
                    .catch { e ->
                        _events.value = _events.value + "Connection error: ${e.message}. Make sure the host booking server is running."
                    }
                    .collect { event ->
                        when (event) {
                            is TextMessageStartEvent -> {
                                currentMessageId = event.messageId
                                messageBuffer.setLength(0)
                            }
                            is TextMessageContentEvent -> {
                                if (event.messageId == currentMessageId) {
                                    messageBuffer.append(event.delta)
                                }
                            }
                            is TextMessageEndEvent -> {
                                if (event.messageId == currentMessageId) {
                                    val fullMessageText = messageBuffer.toString().trim()
                                    if (fullMessageText.startsWith("{") && fullMessageText.endsWith("}")) {
                                        try {
                                            android.util.Log.d("BookingAssistant", "Parsing JSON message: $fullMessageText")
                                            val parsedMsg = parser.parse(fullMessageText)
                                            android.util.Log.d("BookingAssistant", "Successfully parsed JSON message: $parsedMsg")
                                            val createdSurfaceId = when (parsedMsg) {
                                                is A2uiCreateSurfaceMessage -> parsedMsg.surfaceId
                                                else -> null
                                            }
                                            if (createdSurfaceId != null && !lastUpdatedSurfaces.value.contains(createdSurfaceId)) {
                                                lastUpdatedSurfaces.value = lastUpdatedSurfaces.value + createdSurfaceId
                                            }
                                            messageProcessor.processMessage(parsedMsg)
                                        } catch (e: Throwable) {
                                            android.util.Log.e("BookingAssistant", "A2UI parsing/processing error", e)
                                            _events.value = _events.value + "A2UI parsing error: ${e.message}"
                                        }
                                    } else {
                                        android.util.Log.d("BookingAssistant", "Plain text message received: $fullMessageText")
                                        _events.value = _events.value + "[Assistant] $fullMessageText"
                                    }
                                }
                                currentMessageId = null
                            }
                            else -> {}
                        }
                    }
            } catch (e: Throwable) {
                _events.value = _events.value + "Error: ${e.message}"
            }
        }
    }

    fun handleAction(action: A2uiUserAction) {
        android.util.Log.d("BookingAssistant", "handleAction called: action=$action, threadId=$currentThreadId")
        val threadId = currentThreadId ?: return
        viewModelScope.launch {
            try {
                val value = when (action) {
                    is A2uiEventAction -> (action.context["name"] as? String) ?: action.eventName
                    is A2uiFunctionCallAction -> (action.args["name"] as? String) ?: action.functionName
                    else -> ""
                }
                val surfaceId = action.surfaceId
                android.util.Log.d("BookingAssistant", "Coroutine started: surfaceId=$surfaceId, value=$value")
                _events.value = _events.value + "Sending action response for $surfaceId: $value"
                android.util.Log.d("BookingAssistant", "Making HTTP POST request to /respond...")
                val response = httpClient.post("http://localhost:8000/respond") {
                    parameter("sessionId", threadId)
                    parameter("agentId", surfaceId)
                    parameter("value", value)
                }
                android.util.Log.d("BookingAssistant", "HTTP POST request finished: status=${response.status}")
                _events.value = _events.value + "Response status: ${response.status}"
            } catch (e: Exception) {
                _events.value = _events.value + "Error sending response: ${e.message}"
                android.util.Log.e("BookingAssistant", "Error inside coroutine: ${e.message}", e)
            }
        }
    }
}
