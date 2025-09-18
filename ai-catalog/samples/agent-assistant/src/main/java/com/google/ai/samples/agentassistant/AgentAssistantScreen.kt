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
package com.google.ai.samples.agentassistant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ai.samples.agentassistant.R
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.MessageList
import com.android.ai.uicomponent.TextInput

@Composable
fun AgentAssistantScreenContent(modifier: Modifier = Modifier, viewModel: GeminiChatbotViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AgentAssistantScreenContent(
        uiState = uiState,
        onDismissError = viewModel::dismissError,
        onSendMessage = viewModel::sendMessage,
        modifier = modifier,
    )
}

@Composable
private fun AgentAssistantScreenContent(
    uiState: GeminiChatbotUiState,
    onDismissError: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        MessageList(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            messages = uiState.messages,
        )

        when (val state = uiState.geminiMessageState) {
            is GeminiMessageState.Generating -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.Center),
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

            else -> {
                /* No additional UI for waiting state */
            }
        }

        val textFieldState = rememberTextFieldState()
        TextInput(
            state = textFieldState,
            placeholder = stringResource(R.string.input_placeholder),
            primaryButton = {
                GenerateButton(
                    text = "",
                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_send),
                    modifier = Modifier
                        .width(72.dp)
                        .height(55.dp)
                        .padding(2.dp),
                    enabled = uiState.geminiMessageState !is GeminiMessageState.Generating,
                    onClick = {
                        onSendMessage(textFieldState.text.toString())
                        textFieldState.setTextAndPlaceCursorAtEnd("")
                    },
                )
            },
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.BottomCenter),
        )
    }
}
