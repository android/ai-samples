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
import androidx.annotation.RequiresPermission
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminilivetodo.R
import com.android.ai.samples.geminilivetodo.data.Todo
import kotlin.collections.reversed

/**
 * The main screen for the To-do list application.
 * This composable is stateful, connecting to the ViewModel to manage UI state and events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
@Composable
fun TodoScreen(viewModel: TodoScreenViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.initializeGeminiLive()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text(stringResource(R.string.gemini_live_title)) },
            )
        },
        floatingActionButton = {
            if (uiState is TodoScreenUiState.Success) {
                val successState = uiState as TodoScreenUiState.Success
                val micIcon = when {
                    !successState.isLiveSessionReady -> Icons.Filled.MicOff
                    successState.isLiveSessionRunning -> Icons.Filled.Mic
                    else -> Icons.Filled.MicNone
                }

                val containerColor = if (successState.isLiveSessionRunning) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "mic_color_transition")
                    infiniteTransition.animateColor(
                        initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "mic_color",
                    ).value
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }

                FloatingActionButton(
                    onClick = { if (successState.isLiveSessionReady) viewModel.toggleLiveSession() },
                    containerColor = containerColor,
                ) {
                    Icon(micIcon, stringResource(R.string.interact_with_todolist_by_voice))
                }
            } else if (uiState is TodoScreenUiState.Error) {
                val isDialogDisplayed = remember { mutableStateOf(true) }
                if (isDialogDisplayed.value) {
                    AlertDialog(
                        onDismissRequest = { isDialogDisplayed.value = false },
                        title = { Text(text = stringResource(R.string.error_title)) },
                        text = { Text(text = stringResource(R.string.error_message)) },
                        confirmButton = {
                            Button(onClick = { isDialogDisplayed.value = false }) {
                                Text(text = stringResource(R.string.dismiss_button))
                            }
                        },
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .imePadding()
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.new_task_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            viewModel.addTodo(text)
                            text = ""
                        }
                    },
                ) {
                    Text(stringResource(R.string.add_button))
                }
            }

            when (uiState) {
                is TodoScreenUiState.Initial -> {
                    // Show a loading indicator or initial state
                }
                is TodoScreenUiState.Success -> {
                    val todos = (uiState as TodoScreenUiState.Success).todos
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(todos.reversed(), key = { it.id }) { todo ->
                            TodoItem(
                                task = todo,
                                onToggle = { viewModel.toggleTodoStatus(todo.id) },
                                onDelete = { viewModel.removeTodo(todo.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
                is TodoScreenUiState.Error -> {
                    val todos = (uiState as TodoScreenUiState.Error).todos
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(todos.reversed(), key = { it.id }) { todo ->
                            TodoItem(
                                task = todo,
                                onToggle = { viewModel.toggleTodoStatus(todo.id) },
                                onDelete = { viewModel.removeTodo(todo.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItem(task: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    val defaultBackgroundColor = Color.Transparent
    val backgroundColor = remember { Animatable(defaultBackgroundColor) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .background(backgroundColor.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
        )
        Text(
            text = task.task,
            style = if (task.isCompleted) {
                TextStyle(fontSize = 16.sp, textDecoration = TextDecoration.LineThrough)
            } else {
                TextStyle(fontSize = 16.sp, textDecoration = TextDecoration.None)
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
            )
        }
    }
}
