package com.android.ai.samples.agentauditor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// --- Data Models ---
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolResponses: List<ToolResponse>? = null
)

data class ToolCall(
    val name: String,
    val args: Map<String, String>
)

data class ToolResponse(
    val name: String,
    val response: Map<String, Any>
)

// --- UI State ---
sealed class ChatUiState {
    data object Idle : ChatUiState()
    data object Loading : ChatUiState()
    data class AwaitingUserApproval(val message: String, val pendingToolCalls: List<ToolCall>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

@HiltViewModel
class AgentViewModel @Inject constructor(private val androidTools: AndroidTools) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Server URL defaults to 10.0.2.2:8000/chat (emulator localhost) but can be overridden in local.properties
    private val serverUrl = BuildConfig.AGENT_SERVER_URL

    fun sendMessage(userText: String) {
        val userMessage = ChatMessage(role = "user", content = userText)
        _messages.value += userMessage
        
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            runDispatcherLoop()
        }
    }

    /**
     * The recursive Dispatcher Loop
     * This orchestrates the multi-step agent reasoning loop by dispatching messages to the server,
     * executing local client tools, and returning results to the server until a final answer is given
     * or user approval is required.
     */
    private suspend fun runDispatcherLoop() {
        while (true) {
            val currentMessages = _messages.value
            
            try {
                val responseMessage = postToServer(currentMessages)
                
                if (responseMessage.content != null) {
                    // It's a final text response from the model
                    _messages.value += responseMessage
                    _uiState.value = ChatUiState.Idle
                    break // Exit the loop
                } else if (!responseMessage.toolCalls.isNullOrEmpty()) {
                    // The model wants the Android client to run tools
                    val toolCalls = responseMessage.toolCalls
                    
                    // Check if it's requesting approval
                    val approvalCall = toolCalls.find { it.name == "request_user_approval" }
                    if (approvalCall != null) {
                        val messageDesc = approvalCall.args["message"] ?: "Approve transaction?"
                        // Suspend loop to wait for UI interaction
                        _uiState.value = ChatUiState.AwaitingUserApproval(messageDesc, toolCalls)
                        break 
                    }
                    
                    // Otherwise, execute the background tools immediately
                    val toolResponses = mutableListOf<ToolResponse>()
                    for (call in toolCalls) {
                        if (call.name == "get_device_location") {
                            val loc = androidTools.getDeviceLocation()
                            toolResponses.add(ToolResponse(name = call.name, response = mapOf("location" to loc)))
                        } else if (call.name == "open_maps") {
                            val locName = call.args["location_name"] ?: ""
                            androidTools.openMaps(locName)
                            toolResponses.add(ToolResponse(name = call.name, response = mapOf("status" to "opened")))
                        }
                    }
                    
                    // Append the model's tool call request and the user's tool response to history
                    _messages.value += responseMessage
                    _messages.value += ChatMessage(role = "user", toolResponses = toolResponses)
                    
                    // Loop restarts to send the tool results back to the server
                } else {
                    // Empty or malformed response
                    _uiState.value = ChatUiState.Error("Received empty response from server")
                    break
                }
                
            } catch (e: java.net.ConnectException) {
                Log.e("AgentViewModel", "Connection error", e)
                _uiState.value = ChatUiState.Error("Server is not running. Please start the local Python backend server first.")
                break
            } catch (e: Exception) {
                Log.e("AgentViewModel", "Error in dispatcher", e)
                _uiState.value = ChatUiState.Error("Network error: ${e.message}")
                break
            }
        }
    }

    fun onApprovalResult(approved: Boolean, pendingToolCalls: List<ToolCall>) {
        _uiState.value = ChatUiState.Loading
        
        // Form the tool response message
        val responses = pendingToolCalls.map {
            ToolResponse(
                name = it.name,
                response = mapOf("approved" to approved)
            )
        }
        
        // Send the tool response as the user
        val userResponseMsg = ChatMessage(role = "user", toolResponses = responses)
        
        // To be completely correct, we need to append the model's tool call request before the response
        // But since we didn't add the tool call request to the history before suspending, we should add it now
        val modelToolCallMsg = ChatMessage(role = "model", toolCalls = pendingToolCalls)
        
        _messages.value = _messages.value + modelToolCallMsg + userResponseMsg
        
        viewModelScope.launch {
            runDispatcherLoop()
        }
    }

    // --- Simple Network Layer using HttpURLConnection to avoid external dependencies ---
    private suspend fun postToServer(messages: List<ChatMessage>): ChatMessage {
        return withContext(Dispatchers.IO) {
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                // Build Request JSON
                val requestJson = JSONObject()
                val msgArray = JSONArray()
                
                for (msg in messages) {
                    val msgObj = JSONObject()
                    msgObj.put("role", msg.role)
                    if (msg.content != null) {
                        msgObj.put("content", msg.content)
                    }
                    
                    if (msg.toolCalls != null) {
                        val tcArray = JSONArray()
                        for (tc in msg.toolCalls) {
                            val tcObj = JSONObject()
                            tcObj.put("name", tc.name)
                            val argsObj = JSONObject()
                            for ((k, v) in tc.args) argsObj.put(k, v)
                            tcObj.put("args", argsObj)
                            tcArray.put(tcObj)
                        }
                        msgObj.put("tool_calls", tcArray)
                    }
                    
                    if (msg.toolResponses != null) {
                         val trArray = JSONArray()
                         for (tr in msg.toolResponses) {
                             val trObj = JSONObject()
                             trObj.put("name", tr.name)
                             val resObj = JSONObject()
                             for ((k, v) in tr.response) resObj.put(k, v)
                             trObj.put("response", resObj)
                             trArray.put(trObj)
                         }
                         msgObj.put("tool_responses", trArray)
                    }
                    
                    msgArray.put(msgObj)
                }
                
                requestJson.put("messages", msgArray)

                try {
                    val appCheckTokenResult = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                    if (appCheckTokenResult.token.isNotEmpty()) {
                        connection.setRequestProperty("X-Firebase-AppCheck", appCheckTokenResult.token)
                    }
                } catch (e: Exception) {
                    Log.e("AgentViewModel", "Failed to retrieve App Check token", e)
                }

                OutputStreamWriter(connection.outputStream).use { it.write(requestJson.toString()) }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP Error: $responseCode")
                }

                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseString)
                
                // Parse Response JSON back into ChatMessage
                val role = responseJson.getString("role")
                var content: String? = null
                if (responseJson.has("content") && !responseJson.isNull("content")) {
                    content = responseJson.getString("content")
                }
                
                var toolCalls: MutableList<ToolCall>? = null
                if (responseJson.has("tool_calls")) {
                    toolCalls = mutableListOf()
                    val tcArray = responseJson.getJSONArray("tool_calls")
                    for (i in 0 until tcArray.length()) {
                        val tcObj = tcArray.getJSONObject(i)
                        val name = tcObj.getString("name")
                        val argsObj = tcObj.getJSONObject("args")
                        val argsMap = mutableMapOf<String, String>()
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            argsMap[key] = argsObj.getString(key)
                        }
                        toolCalls.add(ToolCall(name, argsMap))
                    }
                }
                
                ChatMessage(role = role, content = content, toolCalls = toolCalls)
                
            } finally {
                connection.disconnect()
            }
        }
    }
}
