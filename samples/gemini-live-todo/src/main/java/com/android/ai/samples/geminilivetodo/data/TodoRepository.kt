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
package com.android.ai.samples.geminilivetodo.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.filterNot
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.text.isNotBlank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val MIC_STATUS_TODO_ID = -999

@Singleton
class TodoRepository @Inject constructor() {

    private val _todos = MutableStateFlow<List<GlassesListItem>>(
        listOf(
            Todo(1234, "buy bread", false),
            Todo(1235, "do the dishes", false),
            Todo(1236, "buy eggs", false),
            Todo(1237, "read a book", false),
            MicControl(
                id = MIC_TODO_ID,
                statusText = "Mic Status",
                isMicOn = false
            ),
        ).filterNot { it.id == MIC_STATUS_TODO_ID },
    )
    val todos: Flow<List<GlassesListItem>> = _todos.asStateFlow()

    fun getTodoList(): List<GlassesListItem> = _todos.value

    fun addTodo(taskDescription: String) {
        if (taskDescription.isNotBlank()) {
            val newTodo = Todo(task = taskDescription, isCompleted = false)
            _todos.update { currentList ->
                listOf(newTodo) + currentList
            }
        }
    }


    fun updateMicStatus(micIsOn: Boolean) {
        val newText = if (micIsOn) "Microphone Status: On" else "Microphone Status: Off"
        _todos.update { currentList ->
            currentList.map { item ->
                if (item.id == MIC_TODO_ID && item is MicControl) {
                    item.copy(
                        statusText = newText,
                        isMicOn = micIsOn
                    )
                } else {
                    item
                }
            }
        }
    }

    fun removeTodo(todoId: Int) {
        _todos.update { currentList ->

            if (todoId == MIC_TODO_ID) {
                currentList
            } else {

                currentList.filterNot { it.id == todoId }
            }
        }
    }

    fun toggleTodoStatus(todoId: Int) {
        _todos.update { currentList ->
            currentList.map { item ->
                when (item) {
                    is MicControl -> {
                        if (item.id == todoId) {
                            item.copy(isMicOn = !item.isMicOn)
                        } else {
                            item
                        }
                    }
                    is Todo -> {
                        if (item.id == todoId) {
                            item.copy(isCompleted = !item.isCompleted)
                        } else {
                            item
                        }
                    }
                }
            }
        }
    }
}