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

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.ChatMessage
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.MessageList
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.TextInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatbotScreen(viewModel: GeminiChatbotViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GeminiChatbotScreen(
        uiState = uiState,
        onSendMessage = {
            viewModel.sendMessage(it)
        },
        onDismissError = viewModel::dismissError,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun GeminiChatbotScreen(uiState: GeminiChatbotUiState, onSendMessage: (String) -> Unit, onDismissError: () -> Unit) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.geminichatbot_title),
                sampleDescription = stringResource(R.string.geminichatbot_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-chatbot",
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                onBackClick = { backDispatcher?.onBackPressed() },
                topAppBarState = topAppBarState,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            MessageList(
                modifier = Modifier
                    .widthIn(max = 646.dp)
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp),
                messages = uiState.messages,
            )

            when (val state = uiState.geminiMessageState) {
                is GeminiMessageState.Generating -> {
                    ContainedLoadingIndicator(
                        modifier = Modifier.size(60.dp)
                            .align(Alignment.Center),
                        indicatorColor = MaterialTheme.colorScheme.tertiary,
                    )
                }

                is GeminiMessageState.Error -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text(text = stringResource(R.string.error)) },
                        text = { Text(text = state.errorMessage) },
                        confirmButton = {
                            Button(onClick = onDismissError) {
                                Text(text = stringResource(R.string.dismiss_button))
                            }
                        },
                    )
                }
                else -> { /* No additional UI for waiting state */ }
            }

            val textFieldState = rememberTextFieldState()
            TextInput(
                state = textFieldState,
                placeholder = stringResource(R.string.geminichatbot_input_placeholder),
                primaryButton = {
                    GenerateButton(
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_send),
                        modifier = Modifier
                            .width(72.dp)
                            .height(55.dp)
                            .padding(4.dp),
                        enabled = uiState.geminiMessageState !is GeminiMessageState.Generating,
                        onClick = {
                            onSendMessage(textFieldState.text.toString())
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                        },
                    )
                },
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 646.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GeminiChatbotScreenPreview() {
    AISampleCatalogTheme {
        GeminiChatbotScreen(
            uiState = GeminiChatbotUiState(
                messages = listOf(
                    ChatMessage(
                        "Hi there!",
                        timestamp = 124,
                        isIncoming = true,
                    ),
                    ChatMessage(
                        "I’m super sleepy today, what coffee drink has the most caffeine, but not too much. Also something hot.",
                        timestamp = 123,
                        isIncoming = false,
                    ),
                ),
            ),
            onSendMessage = {},
            onDismissError = {},
        )
    }
}
