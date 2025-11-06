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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

@OptIn(PublicPreviewAPI::class)
@HiltViewModel
class TodoScreenViewModel @Inject constructor(private val todoRepository: TodoRepository) : ViewModel() {
    private val TAG = "TodoScreenViewModel"
    private var session: LiveSession? = null

    private val liveSessionState = MutableStateFlow<LiveSessionState>(LiveSessionState.NotReady)
    private val todos = todoRepository.todos

    val uiState: StateFlow<TodoScreenUiState> = combine(liveSessionState, todos) { liveSessionState, todos ->
        TodoScreenUiState.Success(todos, liveSessionState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TodoScreenUiState.Initial,
    )

    fun addTodo(taskDescription: String) {
        todoRepository.addTodo(taskDescription)
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

            session?.let {
                if (liveSessionState.value is LiveSessionState.Ready) {
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        it.startAudioConversation(::handleFunctionCall)
                        liveSessionState.value = LiveSessionState.Running
                    }
                } else {
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
                "Change the status of the task",
                mapOf("todoId" to Schema.integer("The id of the task to remove from the todo list")),
            )

            val getTodoList = FunctionDeclaration(
                "getTodoList",
                "Get the list of all the tasks in the todo list",
                emptyMap(),
            )

            val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).liveModel(
                "gemini-2.0-flash-live-preview-04-09",
                generationConfig = liveGenerationConfig,
                systemInstruction = systemInstruction,
                tools = listOf(
                    Tool.functionDeclarations(
                        listOf(getTodoList, addTodo, removeTodo, toggleTodoStatus),
                    ),
                ),
            )

            try {
                session = generativeModel.connect()
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to the model", e)
                liveSessionState.value = LiveSessionState.Error
            }

            liveSessionState.value = LiveSessionState.Ready
        }
    }

    private fun handleFunctionCall(functionCall: FunctionCallPart): FunctionResponsePart {
        return when (functionCall.name) {
            "getTodoList" -> {
                val todoList = todoRepository.getTodoList().reversed()
                val response = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "message" to JsonPrimitive("List of tasks in the todo list: $todoList"),
                    ),
                )
                FunctionResponsePart(functionCall.name, response)
            }
            "addTodo" -> {
                val taskDescription = functionCall.args["taskDescription"]!!.jsonPrimitive.content
                val id = todoRepository.addTodo(taskDescription)

                if (id!=null) {
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(true),
                            "message" to JsonPrimitive("Task $taskDescription added to the todo list (id: $id)"),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response)
                } else {
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(false),
                            "message" to JsonPrimitive("Task $taskDescription wasn't properly added to the list"),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response)
                }

            }
            "removeTodo" -> {
                try {
                    val taskId = functionCall.args["todoId"]!!.jsonPrimitive.int
                    todoRepository.removeTodo(taskId)
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(true),
                            "message" to JsonPrimitive("Task was removed from the todo list"),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response)
                } catch (e: Exception) {
                    val response = JsonObject(
                        mapOf(
                            "success" to JsonPrimitive(false),
                            "message" to JsonPrimitive("Something went wrong: ${e.message}"),
                        ),
                    )
                    FunctionResponsePart(functionCall.name, response)
                }

            }
            "toggleTodoStatus" -> {
                val taskId = functionCall.args["todoId"]!!.jsonPrimitive.int
                todoRepository.toggleTodoStatus(taskId)
                val response = JsonObject(
                    mapOf(
                        "success" to JsonPrimitive(true),
                        "message" to JsonPrimitive("Task was toggled in the todo list"),
                    ),
                )
                FunctionResponsePart(functionCall.name, response)
            }
            else -> {
                val response = JsonObject(
                    mapOf("error" to JsonPrimitive("Unknown function: ${functionCall.name}")),
                )
                FunctionResponsePart(functionCall.name, response)
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
