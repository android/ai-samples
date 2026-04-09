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
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import com.android.ai.samples.geminilivetodo.R
import com.android.ai.samples.geminilivetodo.data.Todo
import com.android.ai.uicomponent.R as UiComponentR
import kotlin.math.min

private val DefaultListItemHeight = 64.dp
private const val MaxItemsInList = 4
private val IconSize = 30.dp
private const val TAG = "GlimmerTodoScreen"
private const val MIC_CONTROL_ID = 111

@Composable
fun GlimmerTodoScreen(
    modifier: Modifier = Modifier,
    viewModel: TodoScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity
    val onExit = { activity?.finish() }

    LaunchedEffect(uiState) {
        if (uiState is TodoScreenUiState.Success) {
            val isMicOn = (uiState as TodoScreenUiState.Success).isMicOn
            Log.i(TAG, "Glimmer UI MIC STATUS: ${if (isMicOn) "Running" else "Ready"}")
        }
    }

    GlimmerTheme {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = modifier
                .fillMaxSize()
                .background(GlimmerTheme.colors.background)
        ) {

            GlimmerScreenContent(
                uiState = uiState,
                onToggleItem = viewModel::toggleTodoStatus,
                onExit = { onExit() }
            )
        }
    }
}

@Composable
private fun GlimmerScreenContent(
    uiState: TodoScreenUiState,
    onToggleItem: (Int) -> Unit,
    onExit: () -> Unit
) {
    when (uiState) {
        is TodoScreenUiState.Initial -> {
            Text(text = stringResource(R.string.loading_todo_list))
        }
        is TodoScreenUiState.Success -> {
            TodoListView(
                todoItems = uiState.todoItems,
                isMicOn = uiState.isMicOn,
                onToggleItem = onToggleItem,
                onExit = onExit
            )
        }
        is TodoScreenUiState.Error -> {
            Text(text = stringResource(R.string.error_loading_list))
        }
    }
}

@Composable
private fun TodoListView(
    todoItems: List<Todo>,
    isMicOn: Boolean,
    onToggleItem: (Int) -> Unit,
    onExit: () -> Unit
) {

    val totalItems = todoItems.size + 2

    val listHeight = (min(totalItems, MaxItemsInList) * DefaultListItemHeight.value +
            min(totalItems - 1, MaxItemsInList) * 12f)

    VerticalList(
        modifier = Modifier.height(listHeight.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        item {
            GlimmerMicControlItem(
                isMicOn = isMicOn,
                onToggle = { onToggleItem(MIC_CONTROL_ID) }
            )
        }

        items(todoItems.size, key = { index -> todoItems[index].id }) { index ->
            GlimmerTodoItem(
                task = todoItems[index],
                onToggle = onToggleItem
            )
        }

        item {
            ListItem(
                onClick = onExit,
                leadingIcon = {
                    Image(
                        painter = painterResource(id = UiComponentR.drawable.ic_close),
                        contentDescription = stringResource(R.string.exit_app),
                        modifier = Modifier.size(IconSize)
                    )
                }
            ) {
                Text(text = stringResource(R.string.exit_app))
            }
        }
    }
}

@Composable
private fun GlimmerMicControlItem(
    isMicOn: Boolean,
    onToggle: () -> Unit
) {
    val icon = if (isMicOn) UiComponentR.drawable.ic_mic_off else UiComponentR.drawable.ic_ai_mic


    val displayTask = if (isMicOn) {
        stringResource(R.string.mic_on_label)
    } else {
        stringResource(R.string.mic_off_label)
    }

    val contentDesc = if (isMicOn) {
        stringResource(R.string.mic_status_on)
    } else {
        stringResource(R.string.mic_status_off)
    }

    ListItem(
        onClick = onToggle,
        leadingIcon = {
            Image(
                painter = painterResource(id = icon),
                contentDescription = contentDesc,
                modifier = Modifier.size(IconSize)
            )
        }
    ) {
        Text(text = displayTask)
    }
}

@Composable
private fun GlimmerTodoItem(
    task: Todo,
    onToggle: (Int) -> Unit
) {
    val icon = if (task.isCompleted) UiComponentR.drawable.ic_check else UiComponentR.drawable.ic_circle

    ListItem(
        onClick = { onToggle(task.id) },
        leadingIcon = {
            Image(
                painter = painterResource(id = icon),
                contentDescription = if (task.isCompleted)
                    stringResource(R.string.status_completed)
                else
                    stringResource(R.string.status_pending),
                modifier = Modifier.size(IconSize)
            )
        }
    ) {
        Text(
            text = task.task,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
        )
    }
}

@Preview(device = "id:ai_glasses_device")
@Composable
private fun GlimmerTodoScreenPreview() {
    val mockTodoItems = listOf(
        Todo(id = 1, task = "Buy groceries", isCompleted = false),
        Todo(id = 2, task = "Finish the report", isCompleted = true),
        Todo(id = 3, task = "Call mom", isCompleted = false)
    )

    GlimmerTheme {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .background(GlimmerTheme.colors.background)
        ) {
            GlimmerScreenContent(
                uiState = TodoScreenUiState.Success(
                    todoItems = mockTodoItems,
                    isMicOn = true,
                    liveSessionState = LiveSessionState.Running
                ),
                onToggleItem = { _ -> },
                onExit = {}
            )
        }
    }
}