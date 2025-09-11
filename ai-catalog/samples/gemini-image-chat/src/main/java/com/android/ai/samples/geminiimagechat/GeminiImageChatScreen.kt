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
package com.android.ai.samples.geminiimagechat

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.util.loadBitmapWithCorrectOrientation
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.android.ai.uicomponent.TextInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiImageChatScreen(viewModel: GeminiImageChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let {
            imageUri = it
        }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    GeminiImageChatScreen(
        uiState = uiState,
        onSendMessage = { message, bitmap ->
            coroutineScope.launch {
                val finalBitmap = bitmap ?: imageUri?.let {
                    withContext(Dispatchers.IO) {
                        loadBitmapWithCorrectOrientation(context, it)
                    }
                }
                viewModel.sendMessage(message, finalBitmap)
                imageUri = null
            }
        },
        onDismissError = viewModel::dismissError,
        onImagePickerClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GeminiImageChatScreen(
    uiState: GeminiImageChatUiState,
    onSendMessage: (String, Bitmap?) -> Unit,
    onDismissError: () -> Unit,
    onImagePickerClick: () -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.gemini_image_chat_title),
                sampleDescription = stringResource(R.string.gemini_image_chat_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/ai-catalog/samples/gemini-image-chat",
                onBackClick = { backDispatcher?.onBackPressed() },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            MessageList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        text = { Text(text = state.errorMessage ?: stringResource(R.string.something_went_wrong)) },
                        confirmButton = {
                            Button(onClick = onDismissError) {
                                Text(text = stringResource(R.string.dismiss_button))
                            }
                        },
                    )
                }
                else -> {}
            }

            val textFieldState = rememberTextFieldState()
            TextInput(
                state = textFieldState,
                placeholder = stringResource(R.string.gemini_image_chat_input_placeholder),
                primaryButton = {
                    GenerateButton(
                        text = "",
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_send),
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp),
                        enabled = uiState.geminiMessageState !is GeminiMessageState.Generating,
                        onClick = {
                            onSendMessage(textFieldState.text.toString(), null)
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                        },
                    )
                },
                secondaryButton = {
                    SecondaryButton(
                        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                        onClick = onImagePickerClick,
                        text = ""
                    )
                },
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(bottom = 54.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    ) {
        items(items = messages, key = { it.timestamp }) { message ->
            MessageBubble(
                message = message,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

private val roundCornerShapeSend = RoundedCornerShape(
    topStart = 40.dp,
    topEnd = 4.dp,
    bottomStart = 40.dp,
    bottomEnd = 40.dp,
)

private val roundCornerShapeReceive = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 40.dp,
    bottomStart = 40.dp,
    bottomEnd = 40.dp,
)

@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    Row {
        if (message.isIncoming) {
            Icon(
                painterResource(com.android.ai.uicomponent.R.drawable.ic_spark),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = if (message.isIncoming) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 300.dp)
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
                Column {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = message.text,
                    )
                    message.image?.let { it: Bitmap ->
                        Image(
                            modifier = Modifier.padding(16.dp),
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GeminiImageChatScreenPreview() {
    AISampleCatalogTheme {
        GeminiImageChatScreen(
            uiState = GeminiImageChatUiState(
                messages = listOf(
                    ChatMessage("Hi there!", 124, true),
                    ChatMessage("I’m super sleepy today...", 123, false),
                ),
            ),
            onSendMessage = { _, _ -> },
            onDismissError = {},
            onImagePickerClick = {},
        )
    }
}
