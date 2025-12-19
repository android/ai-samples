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

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminilivetodo.R
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.android.ai.uicomponent.TextInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(viewModel: AgentScreenViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val activity = LocalActivity.current as Activity

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
                sampleName = "Gemini Live Agent",
                sampleDescription = "Interact with a voice agent",
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/gemini-live-todo",
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
            when (uiState) {
                is AgentScreenUiState.Initial -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AgentScreenUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 646.dp)
                            .fillMaxHeight()
                            .align(Alignment.CenterHorizontally)
                            .weight(1f),
                    ) {
                        Text(
                            text = (uiState as AgentScreenUiState.Success).inputTranscription,
                            modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()),
                        )

                        Text(
                            text = (uiState as AgentScreenUiState.Success).outputTranscription,
                            modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                }
                is AgentScreenUiState.Error -> {
                    // Show error UI
                }
            }

            val textFieldState = rememberTextFieldState()
            val textInputEnabled = remember { mutableStateOf(true) }
            if (uiState is AgentScreenUiState.Success) {
                when {
                    (uiState as AgentScreenUiState.Success).liveSessionState is LiveSessionState.Running -> {
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
//                        viewModel.addTodo(textFieldState.text.toString())
//                        textFieldState.clearText()
                    }
                },
                modifier = Modifier
                    .widthIn(max = 646.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }
    }
}