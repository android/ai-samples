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

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.android.ai.samples.geminilivetodo.GlassesActivity
import com.android.ai.samples.geminilivetodo.R
import com.android.ai.samples.geminilivetodo.data.Todo
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.android.ai.uicomponent.TextInput

private const val TAG = "TodoScreenLaunch"

private val GlassesConnectedGreen = Color(0xFF34A853)

@OptIn(ExperimentalProjectedApi::class)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun launchGlassesExperience(activity: Activity) {
    Log.d(TAG, "Attempting to launch GlassesActivity on connected device...")

    try {
        val projectedContext = ProjectedContext.createProjectedDeviceContext(activity)
        val options = ProjectedContext.createProjectedActivityOptions(projectedContext)
        val intent = Intent(activity, GlassesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(intent, options.toBundle())
        Log.i(TAG, "Successfully sent launch intent to the projected device.")

    } catch (e: IllegalStateException) {
        Log.e(TAG, "Projected device not ready: ${e.message}")
    } catch (e: Exception) {
        Log.e(TAG, "Error during launch: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalProjectedApi::class)
@Composable
fun TodoScreen(viewModel: TodoScreenViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activity = LocalActivity.current as Activity
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isGlassesConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        ProjectedContext.isProjectedDeviceConnected(context, scope.coroutineContext)
            .collectAsStateWithLifecycle(initialValue = false).value
    } else {
        false
    }

    LaunchedEffect(Unit) {
        viewModel.initializeGeminiLive(activity)
    }

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.gemini_live_title),
                sampleDescription = stringResource(R.string.gemini_live_subtitle),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-live-todo",
                topAppBarState = topAppBarState,
                scrollBehavior = scrollBehavior,
                onBackClick = { backDispatcher?.onBackPressed() },
            )
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
            when (val state = uiState) {
                is TodoScreenUiState.Initial -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is TodoScreenUiState.Success -> {
                    val itemsToRender = state.todoItems
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 646.dp)
                            .align(Alignment.CenterHorizontally)
                            .weight(1f),
                    ) {
                        itemsIndexed(itemsToRender, key = { _, item -> item.id }) { index, item ->
                            TodoItem(
                                task = item,
                                onToggle = { viewModel.toggleTodoStatus(item.id) },
                                onDelete = { viewModel.removeTodo(item.id) },
                            )
                            if (index != itemsToRender.size - 1) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
                is TodoScreenUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.error_message),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 646.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            launchGlassesExperience(activity)
                        },
                        expanded = true,
                        containerColor = if (isGlassesConnected) {
                            GlassesConnectedGreen
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = Color.Black,
                        icon = {
                            Icon(
                                painter = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_glasses),
                                contentDescription = null,
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        text = {
                            Text(
                                text = if (isGlassesConnected) "Open on Glasses" else "Connect Glasses",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            val textFieldState = rememberTextFieldState()
            val textInputEnabled = remember { mutableStateOf(true) }

            if (uiState is TodoScreenUiState.Success) {
                val state = uiState as TodoScreenUiState.Success
                when {
                    state.liveSessionState is LiveSessionState.Running -> {
                        val listeningMessage = stringResource(R.string.listening)
                        LaunchedEffect(Unit) {
                            textFieldState.setTextAndPlaceCursorAtEnd(listeningMessage)
                            textInputEnabled.value = false
                        }
                    }
                    else -> {
                        LaunchedEffect(Unit) {
                            textFieldState.clearText()
                            textInputEnabled.value = true
                        }
                    }
                }
            }

            TextInput(
                state = textFieldState,
                enabled = textInputEnabled.value,
                placeholder = stringResource(R.string.new_task_placeholder),
                primaryButton = {
                    GenerateButton(
                        text = "",
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_mic),
                        modifier = Modifier
                            .width(72.dp)
                            .height(55.dp)
                            .padding(4.dp),
                        onClick = {
                            viewModel.toggleLiveSession(activity)
                        },
                    )
                },
                secondaryButton = {
                    SecondaryButton(
                        text = "",
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_add),
                        modifier = Modifier
                            .width(48.dp)
                            .height(55.dp)
                            .padding(4.dp),
                    ) {
                        viewModel.addTodo(textFieldState.text.toString())
                        textFieldState.clearText()
                    }
                },
                modifier = Modifier
                    .widthIn(max = 646.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
fun TodoItem(task: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
        )
        Text(
            text = task.task,
            style = if (task.isCompleted) {
                MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
            } else {
                MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.None)
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(10.dp),
                )
                .size(32.dp),
        ) {
            Icon(
                painterResource(com.android.ai.uicomponent.R.drawable.ic_delete),
                modifier = Modifier.size(20.dp),
                contentDescription = "Delete",
            )
        }
    }
}