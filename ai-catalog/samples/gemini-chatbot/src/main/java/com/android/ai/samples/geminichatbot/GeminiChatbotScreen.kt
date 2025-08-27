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

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.GenerateButton
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
        onDismissError = viewModel::dismissError
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GeminiChatbotScreen(
    uiState: GeminiChatbotUiState,
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

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
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            MessageList(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                messages = uiState.messages
            )

            when (val state = uiState.geminiMessageState) {
                is GeminiMessageState.Generating -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterHorizontally),
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
                        text = "",
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_send),
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp),
                        enabled = uiState.geminiMessageState !is GeminiMessageState.Generating,
                        onClick = {
                            onSendMessage(textFieldState.text.toString())
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                        },
                    )
                },
                modifier = Modifier
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    ) {
        items(items = messages) { message ->
            MessageBubble(
                message = message,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

val roundCornerShapeSend = RoundedCornerShape(
    topStart = 40.dp,
    topEnd = 4.dp,
    bottomStart = 40.dp,
    bottomEnd = 40.dp
)

val roundCornerShapeReceive = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 40.dp,
    bottomStart = 40.dp,
    bottomEnd = 40.dp
)

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (message.isIncoming) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp)
                .border(
                    2.dp,
                    if (message.isIncoming) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = if (message.isIncoming) roundCornerShapeReceive else roundCornerShapeSend,
                )
                .clip(
                    shape = if (message.isIncoming) roundCornerShapeReceive else roundCornerShapeSend,
                ),
            color = if (message.isIncoming) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            Text(
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                text = message.text,
                color = if (message.isIncoming) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyLarge,
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
                    ChatMessage("Hi there!",
                        timestamp = 124,
                        isIncoming = true),
                    ChatMessage("I’m super sleepy today, what coffee drink has the most caffeine, but not too much. Also something hot.",
                        timestamp = 123,
                        isIncoming = false)
                )
            ),
            onSendMessage = {},
            onDismissError = {}
        )
    }
}
