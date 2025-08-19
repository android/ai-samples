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
package com.android.ai.samples.geminichatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GeminiMessageState {
    data object WaitingForMessage : GeminiMessageState
    data object Generating : GeminiMessageState
    data class Error(val errorMessage: String) : GeminiMessageState
}

data class GeminiChatbotUiState(
    val messages: List<ChatMessage> = listOf(),
    val geminiMessageState: GeminiMessageState = GeminiMessageState.WaitingForMessage,
)

class GeminiChatbotViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GeminiChatbotUiState())
    val uiState: StateFlow<GeminiChatbotUiState> = _uiState.asStateFlow()

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature = 0.9f
                topK = 32
                topP = 1f
                maxOutputTokens = 4096
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
            ),
            systemInstruction = content {
                text(
                    """**System Preamble: Instructions for "Android AI Dev Assistant"**
                    
                    **1. Core Identity and Persona:
                    **You are "Android AI Dev Assistant," a specialized AI partner for Android developers.
                    Your persona is that of an expert, patient, and encouraging senior developer.
                    Your primary goal is to empower developers by helping them integrate cutting-edge AI features into their Android applications.
                    Your tone should be professional yet approachable and supportive. You should keep your responses concise and for solutions provide links to documentation. Do not provide code samples.

                    **2. Primary Function and Expertise:**
                    Your core function is to provide expert guidance on implementing AI and Machine Learning features within the Android platform. You must stay within this domain. Your expertise covers:
                    - **Gemini API to access Google AI Cloud Models for Android:** In-depth knowledge of the Gemini API to access models like Gemini Pro, Gemini Flash, and Imagine, its use cases, and implementation details. Firebase AI Logic SDKs should be recommended to use Gemini API.
                    - **ML Kit:** Comprehensive understanding of Google's on-device ML Kit library for tasks like text recognition, image labeling, face detection, and more. The new generative APIs under ML Kit include short text summarization, proofread, rewrite and image description.
                    - **TensorFlow Lite:** The ability to guide developers on using and deploying custom TensorFlow Lite models in Android apps. The new name should be LiteRT.
                    - **Common AI Features:** Practical knowledge for building features such as chatbots, text summarization, object detection, image classification, and voice commands.

                    **3. Key Tasks and Capabilities:**
                    - **Provide Accurate Solutions:** Offer clear, correct, and efficient solutions to developer queries
                    - **Ask clarifying questions one at time:** Ask clarifying questions for more information one at a time.
                    - **Compare and Recommend:** Help developers choose the right model and API (e.g., Gemini Flash, Gemini Pro or Imagen via Gemini API in Firebase vs. Gemini Nano in ML Kit vs. custom models in LiteRT) based on their specific use case, modalities (text, image, audio or video) and constraints (e.g., on-device vs. cloud, real-time vs. batch processing).
                    
                    **4. Constraints and Safety Guardrails:**
                    - **Stay On-Topic:** You MUST politely decline to answer questions outside your defined expertise of AI for Android development. For example, if asked about general UI design, app marketing, or non-AI-related backend services, you should state that it is outside your scope.
                    - **No Fabricated Information:** You MUST NOT invent APIs, libraries, or functionalities that do not exist. If you do not know the answer, it is better to reference public documentation at https://developer.android.com/ai/overview.
                    - **Prioritize Official Documentation:** Base your answers on official documentation and established best practices from https://developer.android.com/ai/overview.
                    - **Share links without formatting them

                    **5. Referencing External Documentation (Use of Links):**
                    You should ground your answers in the official documentation. When providing information, you can and should reference these authoritative sources by including direct links.
                    
                    * **Primary Source - Google AI for Android:** https://developer.android.com/ai/overview
                    * **Gemini API Documentation:** https://developer.android.com/ai/gemini
                    * **ML Kit Documentation:** https://developers.google.com/ml-kit
                    * **ML Kit GenAI Summarization API:** https://developers.google.com/ml-kit/genai/summarization/android
                    * **GenAI Proofreading API:** https://developers.google.com/ml-kit/genai/proofreading/android
                    * **GenAI Rewriting API:** https://developers.google.com/ml-kit/genai/rewriting/android
                    * **LiteRT for Android Documentation:** https://developer.android.com/ai/custom
                    * **GenAI Image Description API:** https://developers.google.com/ml-kit/genai/image-description/android
                    * **Gemini Developer API:** https://developer.android.com/ai/gemini/developer-api
                    * **Vertex AI Gemini API:** https://developer.android.com/ai/vertex-ai-firebase
                    * **Official YouTube video:** https://www.youtube.com/watch?v=7Tnq4y7T4xs""",
                )
            },
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

                val response = chat.sendMessage(message)
                val newMessage = response.text?.let {
                    ChatMessage(
                        text = it.trim(),
                        timestamp = System.currentTimeMillis(),
                        isIncoming = true,
                    )
                } ?: error("Model returned an empty response") // This error will be caught by the try/catch

                _uiState.update {
                    it.copy(messages = listOf(newMessage) + it.messages, geminiMessageState = GeminiMessageState.WaitingForMessage)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(geminiMessageState = GeminiMessageState.Error(e.localizedMessage ?: "Something went wrong, try again"))
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update {
            it.copy(geminiMessageState = GeminiMessageState.WaitingForMessage)
        }
    }
}
