/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ai.samples.geminilivetodo.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.geminilivetodo.data.MIC_STATUS_TODO_ID
import com.android.ai.samples.geminilivetodo.data.MicControl
import com.android.ai.samples.geminilivetodo.data.Todo
import com.android.ai.samples.geminilivetodo.data.TodoRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

@OptIn(PublicPreviewAPI::class)
@HiltViewModel
class TodoScreenViewModel @Inject constructor(private val todoRepository: TodoRepository) : ViewModel() {
    private val TAG = "TodoScreenViewModel"
    internal var session: LiveSession? = null
    private var hostActivityRef: WeakReference<Activity>? = null

    private val liveSessionState = MutableStateFlow<LiveSessionState>(LiveSessionState.NotReady)
    private val todos = todoRepository.todos

    val uiState: StateFlow<TodoScreenUiState> = combine(liveSessionState, todos) { liveSessionState, currentTodos ->
        val micItem = currentTodos.filterIsInstance<MicControl>().firstOrNull()
        val isMicOn = micItem?.isMicOn ?: false

        val todoItems = currentTodos
            .filterIsInstance<Todo>()
            .filterNot { it.id == MIC_STATUS_TODO_ID }
            .reversed()

        TodoScreenUiState.Success(
            todoItems = todoItems,
            isMicOn = isMicOn,
            liveSessionState = liveSessionState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TodoScreenUiState.Initial,
    )

    fun addTodo(taskDescription: String): Int? {
        return todoRepository.addTodo(taskDescription)
    }

    fun removeTodo(todoId: Int) {
        todoRepository.removeTodo(todoId)
    }

    fun toggleTodoStatus(todoId: Int) {
        todoRepository.toggleTodoStatus(todoId)
    }

    @SuppressLint("MissingPermission")
    fun toggleLiveSession(activity: Activity) {
        viewModelScope.launch {
            if (liveSessionState.value is LiveSessionState.NotReady) return@launch

            session?.let { currentSession ->
                if (liveSessionState.value is LiveSessionState.Ready) {
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            Log.i(TAG, "API Sync: Live Session Starting...")
                            currentSession.startAudioConversation(::handleFunctionCall)
                            liveSessionState.update { LiveSessionState.Running }
                            todoRepository.updateMicStatus(micIsOn = true)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting Live Session: ${e.message}", e)
                            liveSessionState.update { LiveSessionState.Ready }
                            todoRepository.updateMicStatus(micIsOn = false)
                        }
                    } else {
                        requestAudioPermissionIfNeeded(activity)
                    }
                } else {
                    try {
                        currentSession.stopAudioConversation()
                        liveSessionState.update { LiveSessionState.Ready }
                        todoRepository.updateMicStatus(micIsOn = false)
                        Log.i(TAG, "API Sync: Live Session Stopped.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping Live Session: ${e.message}", e)
                        liveSessionState.update { LiveSessionState.Ready }
                    }
                }
            }
        }
    }

    fun initializeGeminiLive(activity: Activity) {
        if (liveSessionState.value !is LiveSessionState.NotReady) {
            Log.d(TAG, "Gemini Live already initialized or initializing")
            return
        }
        hostActivityRef = WeakReference(activity)
        requestAudioPermissionIfNeeded(activity)

        viewModelScope.launch {
            Log.d(TAG, "Start Gemini Live initialization")

            val liveGenerationConfig = liveGenerationConfig {
                speechConfig = SpeechConfig(voice = Voice("FENRIR"))
                responseModality = ResponseModality.AUDIO
            }

            val systemInstruction = content {
                text(
                    """
                **Your Role:** You are a friendly and helpful voice assistant in this app. 
                Your main job is to change update the tasks in the todo list based on user requests.
    
                **Interaction Steps:**
                **Get the task id to remove or toggle a task:** If you need to remove or check/uncheck a task,
                    you'll need to retrieve the list of items in the list first to get the task id. Don't share 
                    the id with the user, just identify the id of the task mentioned and directly pass this id to the 
                    tool.
          
                **Never share the id with the user:** you don't need to share the id with the user. It is 
                    just here to help you perform the check/uncheck and remove operations to the list.
    
                **Voice Feedback Commands:** If the user sends a message like 'Turning Microphone On', you MUST acknowledge it immediately by saying that exact phrase clearly.

                **If Unsure:** If you can't determine the update from the request, politely ask the user to rephrase or try something else.
                    """.trimIndent(),
                )
            }

            val addTodo = FunctionDeclaration(
                "addTodo",
                "Add a task to the todo list",
                mapOf("taskDescription" to Schema.string("A succinct string describing the task")),
            )

            val removeTodo = FunctionDeclaration(
                "removeTodo",
                "Remove a task from the todo list",
                mapOf("todoId" to Schema.integer("The id of the task to remove from the todo list")),
            )

            val toggleTodoStatus = FunctionDeclaration(
                "toggleTodoStatus",
                "Toggle the completion status of a task (e.g. check off or uncheck)",
                mapOf("todoId" to Schema.integer("The id of the task to mark as completed or incomplete")),
            )

            val getTodoList = FunctionDeclaration(
                "getTodoList",
                "Get the list of all the tasks in the todo list",
                emptyMap(),
            )

            // See https://firebase.google.com/docs/ai-logic/live-api for an overview of available models
            val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                "gemini-2.5-flash-native-audio-preview-12-2025",
                generationConfig = liveGenerationConfig,
                systemInstruction = systemInstruction,
                tools = listOf(
                    Tool.functionDeclarations(
                        listOf(getTodoList, addTodo, removeTodo, toggleTodoStatus),
                    ),
                ),
            )

            todoRepository.updateMicStatus(micIsOn = false)

            try {
                session = generativeModel.connect()
                liveSessionState.update { LiveSessionState.Ready }
                Log.i(TAG, "MIC STATE UPDATE: Session connected (LiveSessionState.Ready).")
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to the model", e)
                liveSessionState.update { LiveSessionState.Error }
            }
        }
    }

    private fun handleFunctionCall(functionCall: FunctionCallPart): FunctionResponsePart {
        return when (functionCall.name) {
            "getTodoList" -> {
                Log.d(TAG, "Tool Call: getTodoList")
                val todoList = todoRepository.getTodoList().filterIsInstance<Todo>()
                
                // Return structured JSON for reliable tool use
                val jsonList = todoList.reversed().map { item ->
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(item.id),
                            "task" to JsonPrimitive(item.task),
                            "isCompleted" to JsonPrimitive(item.isCompleted),
                        ),
                    )
                }
                val response = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "tasks" to kotlinx.serialization.json.JsonArray(jsonList),
                    ),
                )
                FunctionResponsePart(functionCall.name, response, functionCall.id)
            }
            "addTodo" -> {
                val taskDescription = functionCall.args["taskDescription"]!!.jsonPrimitive.content
                val id = todoRepository.addTodo(taskDescription)
                if (id != null) {
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(true),
                            "message" to JsonPrimitive("Task $taskDescription added to the todo list (id: $id)"),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response, functionCall.id)
                } else {
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(false),
                            "message" to JsonPrimitive("Task $taskDescription wasn't properly added to the list"),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response, functionCall.id)
                }
            }
            "removeTodo" -> {
                try {
                    val taskId = functionCall.args["todoId"]!!.jsonPrimitive.int
                    todoRepository.removeTodo(taskId)
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(true),
                            "message" to JsonPrimitive("Task removed successfully."),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response, functionCall.id)
                } catch (e: Exception) {
                    val response = JsonObject(
                        mapOf("success" to JsonPrimitive(false), "error" to JsonPrimitive(e.message))
                    )
                    FunctionResponsePart(functionCall.name, response, functionCall.id)
                }
            }
            "toggleTodoStatus" -> {
                try {
                    val taskId = functionCall.args["todoId"]!!.jsonPrimitive.int
                    todoRepository.toggleTodoStatus(taskId)
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(true),
                            "message" to JsonPrimitive("Task status toggled successfully."),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response, functionCall.id)
                } catch (e: Exception) {
                    val response = JsonObject(
                        mapOf("success" to JsonPrimitive(false), "error" to JsonPrimitive(e.message))
                    )
                    FunctionResponsePart(functionCall.name, response, functionCall.id)
                }
            }
            else -> {
                val response = JsonObject(
                    mapOf("error" to JsonPrimitive("Unknown function: ${functionCall.name}")),
                )
                FunctionResponsePart(functionCall.name, response, functionCall.id)
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
