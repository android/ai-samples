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

@Singleton
class TodoRepository @Inject constructor() {

    private val _todos = MutableStateFlow<List<Todo>>(
        listOf(
            Todo(1234, "buy bread", false),
            Todo(1235, "do the dishes", false),
            Todo(1236, "buy eggs", false),
            Todo(1237, "read a book", false),
        ),
    )
    val todos: Flow<List<Todo>> = _todos.asStateFlow()

    fun getTodoList(): List<Todo> = _todos.value

    fun addTodo(taskDescription: String) {
        if (taskDescription.isNotBlank()) {
            val newTodo = Todo(task = taskDescription)
            _todos.update { currentList ->
                currentList + newTodo
            }
        }
    }

    fun removeTodo(todoId: Long) {
        _todos.update { currentList ->
            currentList.filterNot { it.id == todoId }
        }
    }

    fun toggleTodoStatus(todoId: Long) {
        _todos.update { currentList ->
            currentList.map { todo ->
                if (todo.id == todoId) {
                    todo.copy(isCompleted = !todo.isCompleted)
                } else {
                    todo
                }
            }
        }
    }
}
