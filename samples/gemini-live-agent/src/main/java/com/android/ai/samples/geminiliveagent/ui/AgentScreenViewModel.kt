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
package com.android.ai.samples.geminiliveagent.ui

import android.Manifest
import android.R.id.input
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Transcription
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

@OptIn(PublicPreviewAPI::class)
@HiltViewModel
class AgentScreenViewModel @Inject constructor() : ViewModel() {
    private val TAG = "AgentScreenViewModel"
    private var session: LiveSession? = null

    // For mocking responses to tool calls
    private var responseModel: GenerativeModel? = null

    private val liveSessionState = MutableStateFlow<LiveSessionState>(LiveSessionState.NotReady)
    private val _inputTranscription = MutableStateFlow("")
    private val _outputTranscription = MutableStateFlow("")


    val uiState: StateFlow<AgentScreenUiState> = combine(
        liveSessionState,
        _inputTranscription,
        _outputTranscription
    ) { liveSessionState, inputTranscription, outputTranscription ->
        AgentScreenUiState.Success(
            liveSessionState = liveSessionState,
            inputTranscription = inputTranscription,
            outputTranscription = outputTranscription
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AgentScreenUiState.Initial,
    )

    @SuppressLint("MissingPermission")
    fun toggleLiveSession(activity: Activity? = null) {
        viewModelScope.launch {
            if (liveSessionState.value is LiveSessionState.NotReady) return@launch

            session?.let {
                if (liveSessionState.value is LiveSessionState.Ready) {
                    if (ContextCompat.checkSelfPermission(
                            activity!!,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(TAG, "Starting audio conversation")
                        it.startAudioConversation(
                            functionCallHandler = ::handleFunctionCall,
                            enableInterruptions = true,
                            transcriptHandler = { input: Transcription?, output: Transcription? ->
                                input?.text?.let { currentTranscript -> _inputTranscription.update { transcript -> transcript + currentTranscript } }
                                output?.text?.let { currentTranscript -> _outputTranscription.update { transcript -> transcript + currentTranscript } }
                                Log.d(TAG, "INPUT: ${input?.text}")
                                Log.d(TAG, "OUTPUT: ${output?.text}")

                            }
                        )
                        liveSessionState.value = LiveSessionState.Running
                    }
                } else {
                    Log.d(TAG, "Stopping audio conversation")
                    it.stopAudioConversation()
                    liveSessionState.value = LiveSessionState.Ready
                }
            }
        }
    }

    fun initializeGeminiLive(activity: Activity) {
        requestAudioPermissionIfNeeded(activity)
        viewModelScope.launch {
            Log.d(TAG, "Start Gemini Live initialization")
            val liveGenerationConfig = liveGenerationConfig {
                speechConfig = SpeechConfig(voice = Voice("FENRIR"))
                responseModality = ResponseModality.AUDIO
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            }

            val systemInstruction = content {
                text(
                    """
        You are "Voyage," a specialized, high-efficiency voice assistant designed to help users find, explore, and manage tours and activities. You operate in an AI Glasses environment, meaning all interaction is audio-only, without any visual display.

        **CORE DIRECTIVES (Brevity & Focus):**
        1.  **Be Concise:** Your responses must be exceptionally brief and precise. Never exceed two sentences for a standard response or listing. Eliminate all unnecessary filler words, greetings, or elaborate transitions.
        2.  **Stay Focused:** Always drive the conversation towards the next actionable step (search, details, booking). Do not engage in general chat.
        3.  **Maintain Context:** Always reference the current activity by its name to help the user maintain context in an audio-only environment.

        **INTERACTION FLOW PROTOCOL:**
        1.  **Initial State:** Start by asking for the key search parameters. (e.g., "Welcome. What location and type of activity are you looking for?")
        2.  **Tool Usage:** Prioritize using the available tools (`Search`, `Get_details`, `Book`, `Save`, `Share`, `Quit`) based on user intent.
        3.  **Listing Results (Mandatory Pause):** When presenting results from the `Search` tool, you MUST list them **one at a time**.
            * State the Activity Name, a single key feature, and the price (if available).
            * **Crucially, after each result, you MUST pause and prompt the user for direction:** "Would you like more details on [Activity Name], or should I proceed to the next result?"
        4.  **Cycling/Details:** The user may interrupt at any point while cycling results with the following commands:
            * **"More Details":** Use the `Get_details(Tour_identifier)` tool.
            * **"Next" / "Previous":** Move to the next or previous result in the list.
            * **"Book" / "Save" / "Share":** Immediately execute the corresponding tool call for the currently referenced activity.
        5.  **Action Confirmation:** After executing `Book`, `Save`, or `Share`, provide a single, immediate confirmation statement. (e.g., "Confirmed. [Activity Name] has been saved.")
        6.  **Quit:** When the user says "Quit" or "Stop helping," say "Ending experience, bye!", and then execute the `Quit` tool. 
        """.trimIndent(),
                )
            }

            val searchTool = FunctionDeclaration(
                "Search",
                "Searches for tours and activities at a given location, optionally filtered by user criteria.",
                mapOf(
                    "Location" to Schema.string("The city or landmark for the search."),
                    "Filters" to Schema.string("A comma-separated list of criteria to filter the results (e.g., 'half-day', 'historical', 'under \$50').")
                ),
                listOf("Location") // Required parameters
            )

            val getDetailsTool = FunctionDeclaration(
                "Get_details",
                "Retrieves comprehensive information and full description for a specific tour or activity using its identifier.",
                mapOf(
                    "Tour_identifier" to Schema.string("The unique ID or name of the tour to retrieve details for.")
                ),
                listOf("Tour_identifier")
            )

            val bookTool = FunctionDeclaration(
                "Book",
                "Initiates the booking process for a specified tour or activity.",
                mapOf(
                    "Tour_identifier" to Schema.string("The unique ID or name of the tour to book.")
                ),
                listOf("Tour_identifier")
            )

            val saveTool = FunctionDeclaration(
                "Save",
                "Saves a specified tour or activity to the user's personal saved list for later review.",
                mapOf(
                    "Tour_identifier" to Schema.string("The unique ID or name of the tour to save.")
                ),
                listOf("Tour_identifier")
            )

            val shareTool = FunctionDeclaration(
                "Share",
                "Shares the details of a specified tour or activity with a contact (e.g., via email or message).",
                mapOf(
                    "Tour_identifier" to Schema.string("The unique ID or name of the tour to share.")
                ),
                listOf("Tour_identifier")
            )

            val quitTool = FunctionDeclaration(
                "Quit",
                "Terminates the current assistance session.",
                emptyMap(),
            )

            val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                "gemini-2.5-flash-native-audio-preview-09-2025",
                generationConfig = liveGenerationConfig,
                systemInstruction = systemInstruction,
                tools = listOf(
                    Tool.functionDeclarations(
                        listOf(searchTool, getDetailsTool, bookTool, saveTool, shareTool, quitTool),
                    ),
                ),
            )

            // --- 2. Response Generator Model Setup ---

            // System instruction for the response generator model to enforce JSON output
            val responseSystemInstruction = content {
                text("You are a system that generates JSON data responses for mock API calls. ONLY output the requested JSON object. Do not add any conversational text or markdown formatting.")
            }

            responseModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                "gemini-2.5-flash",
                systemInstruction = responseSystemInstruction,
            )

            try {
                session = generativeModel.connect()
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to the model(s)", e)
                liveSessionState.value = LiveSessionState.Error
            }

            liveSessionState.value = LiveSessionState.Ready
        }
    }

    // Use a simple LLM to generate fake responses - for sample purposes only.
    private fun handleFunctionCall(functionCall: FunctionCallPart): FunctionResponsePart {
        Log.d(TAG, "Function call: ${functionCall.name}")
        val responseSession = responseModel ?: run {
            Log.e(TAG, "Response model not initialized.")
            return FunctionResponsePart(functionCall.name, JsonObject(mapOf("error" to JsonPrimitive("System error: Response generator unavailable"))), functionCall.id)
        }
        if(functionCall.name == "Quit") {
            viewModelScope.launch {
                delay(5000)
                session?.stopAudioConversation()
                liveSessionState.value = LiveSessionState.Ready
            }
            return FunctionResponsePart(functionCall.name, JsonObject(mapOf("success" to JsonPrimitive(true), "message" to JsonPrimitive("Session termination confirmed."))), functionCall.id)
        }
        val prompt = when (functionCall.name) {
            "Search" -> "Generate a JSON response for a successful 'Search' call for ${functionCall.args["Location"]}, listing 3 relevant tours. Include 'id', 'name', 'feature', 'price' (integer), 'location' (string), 'filters' (string), and 'results_count' (integer)."
            "Get_details" -> "Generate a JSON response for a successful 'Get_details' call for Tour ID ${functionCall.args["Tour_identifier"]}, including 'name', 'description' (at least 2 sentences), 'duration' (string), and 'rating' (float)."
            "Book" -> "Generate a JSON response for a successful 'Book' call for Tour ID ${functionCall.args["Tour_identifier"]}. Include 'success' (boolean: true) and a 'message' confirmation."
            "Save" -> "Generate a JSON response for a successful 'Save' call for Tour ID ${functionCall.args["Tour_identifier"]}. Include 'success' (boolean: true) and a 'message' confirmation."
            "Share" -> "Generate a JSON response for a successful 'Share' call for Tour ID ${functionCall.args["Tour_identifier"]}. Include 'success' (boolean: true) and a 'message' confirmation."
            else -> "Generate a JSON error response for unknown function: ${functionCall.name}"
        }

        return runBlocking {
             try {
                // Call the simpler LLM to generate the JSON data response
                val responseText = responseSession.generateContent(prompt).text

                // The JSON parser must handle potentially messy LLM output (e.g., surrounding markdown)
                val cleanJsonString = responseText!!.trim().removePrefix("```json").removeSuffix("```")

                val jsonResponse = Json.parseToJsonElement(cleanJsonString).jsonObject

                FunctionResponsePart(functionCall.name, jsonResponse, functionCall.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating or parsing JSON response from LLM: ${e.message}", e)
                val errorResponse = JsonObject(mapOf("error" to JsonPrimitive("Failed to generate synthetic data: ${e.message}")))
                FunctionResponsePart(functionCall.name, errorResponse, functionCall.id)
            }
        }
    }

    fun requestAudioPermissionIfNeeded(activity: Activity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }
}
