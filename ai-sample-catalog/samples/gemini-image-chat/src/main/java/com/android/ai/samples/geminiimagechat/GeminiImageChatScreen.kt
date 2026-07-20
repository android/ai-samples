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

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.ai.samples.util.loadBitmapWithCorrectOrientation
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.uicomponent.ChatMessage
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.MessageList
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
        onSendMessage = { message ->
            coroutineScope.launch {
                val finalBitmap = imageUri?.let {
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
        imageUri = imageUri,
    ) {
        imageUri = null
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun GeminiImageChatScreen(
    uiState: GeminiImageChatUiState,
    onSendMessage: (String) -> Unit,
    onDismissError: () -> Unit,
    onImagePickerClick: () -> Unit,
    imageUri: Uri? = null,
    onImageClicked: () -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages) {
        coroutineScope.launch {
            lazyListState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.gemini_image_chat_title),
                sampleDescription = stringResource(R.string.gemini_image_chat_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/gemini-image-chat",
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
                listState = lazyListState,
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
                secondaryButton = {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.clickable(
                                onClick = onImageClicked,
                            ).width(50.dp)
                                .height(55.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                    } else {
                        SecondaryButton(
                            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_img),
                            modifier = Modifier
                                .width(48.dp)
                                .height(55.dp)
                                .padding(4.dp),
                            onClick = onImagePickerClick,
                        )
                    }
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
private fun GeminiImageChatScreenPreview() {
    AISampleCatalogTheme {
        GeminiImageChatScreen(
            uiState = GeminiImageChatUiState(
                messages = listOf(
                    ChatMessage("Hi there!", 124, true),
                    ChatMessage("I’m super sleepy today...", 123, false),
                ),
            ),
            onSendMessage = { _ -> },
            onDismissError = {},
            onImagePickerClick = {},
            onImageClicked = {},
        )
    }
}
