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
package com.android.ai.samples.genai_summarization

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminimultimodal.R
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.theme.surfaceContainerHighestLight
import com.android.ai.uicomponent.BackButton
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAISummarizationScreen(viewModel: GenAISummarizationViewModel = hiltViewModel()) {
    val sampleTextOptions = stringArrayResource(R.array.summarization_sample_text)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var textInput by rememberSaveable { mutableStateOf("") }

    GenAISummarizationContent(
        uiState = uiState,
        textInput = textInput,
        onTextInputChanged = { textInput = it },
        onSummarizeClicked = { viewModel.summarize(textInput) },
        onClearClicked = {
            viewModel.clearGeneratedSummary()
            textInput = ""
        },
        onAddSampleTextClicked = { textInput = sampleTextOptions.random() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAISummarizationContent(
    uiState: GenAISummarizationUiState,
    textInput: String,
    onTextInputChanged: (String) -> Unit,
    onSummarizeClicked: () -> Unit,
    onClearClicked: () -> Unit,
    onAddSampleTextClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.genai_summarization_title_bar),
                sampleDescription = stringResource(R.string.genai_summarization_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/genai-summarization",
                onBackClick = { backDispatcher?.onBackPressed() },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clip(RoundedCornerShape(40.dp))
                .background(color = surfaceContainerHighestLight)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(top = 16.dp)
                    .imePadding()
                    .widthIn(max = 646.dp)
                    .fillMaxHeight(),
            ) {
                when (val state = uiState) {
                    GenAISummarizationUiState.CheckingFeatureStatus ->
                        // TODO: Replace with loading animation
                        DisplayedText(
                            textToDisplay = stringResource(id = R.string.summarization_checking_feature_status),
                            isStatusText = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    is GenAISummarizationUiState.DownloadingFeature ->
                        DisplayedText(
                            stringResource(
                                id = R.string.summarization_downloading,
                                state.bytesDownloaded,
                                state.bytesToDownload,
                            ),
                            isStatusText = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    is GenAISummarizationUiState.Error ->
                        DisplayedText(
                            state.errorMessage,
                            isStatusText = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 646.dp)
                                .align(Alignment.CenterHorizontally),
                        )

                    GenAISummarizationUiState.Initial -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 646.dp)
                                .weight(1f)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            TextField(
                                placeholder = { Text(stringResource(R.string.genai_summarization_text_input_label)) },
                                value = textInput, onValueChange = onTextInputChanged,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .weight(1f),
                            )

                            if (textInput.isEmpty()) {
                                SecondaryButton(
                                    text = stringResource(R.string.genai_summarization_add_text_btn),
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_add_text),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    onClick = onAddSampleTextClicked,
                                )
                            } else {
                                GenerateButton(
                                    text = stringResource(R.string.genai_summarization_summarize_btn),
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_text),
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                                    onClick = onSummarizeClicked,
                                )
                            }
                        }
                    }

                    is GenAISummarizationUiState.Generating ->
                        DisplayedText(
                            state.generatedOutput,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    is GenAISummarizationUiState.Success -> {
                        DisplayedText(state.generatedOutput, modifier = modifier.weight(1f).fillMaxWidth())

                        BackButton(
                            modifier = Modifier
                                .padding(start = 8.dp, top = 8.dp),
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            onClick = onClearClicked,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayedText(textToDisplay: String, modifier: Modifier = Modifier, isStatusText: Boolean = false) {
    Text(
        textToDisplay, modifier = modifier.padding(8.dp),
        fontSize = if (!isStatusText) {
            16.sp
        } else {
            24.sp
        },
    )
}

@Preview
@Composable
fun GenAISummarizationContentPreview_Initial_EmptyText() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.Initial,
            textInput = "",
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAISummarizationContentPreview_Initial_WithText() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.Initial,
            textInput = stringResource(R.string.summarization_sample_text_1),
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAISummarizationContentPreview_CheckingFeatureStatus() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.CheckingFeatureStatus,
            textInput = "",
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAISummarizationContentPreview_DownloadingFeature() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.DownloadingFeature(100, 50),
            textInput = "",
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAISummarizationContentPreview_Error() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.Error(stringResource(R.string.summarization_generation_error)),
            textInput = "",
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAISummarizationContentPreview_Generating() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.Generating("Generating summary..."),
            textInput = "This is a sample text to summarize.",
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAISummarizationContentPreview_Success() {
    AISampleCatalogTheme {
        GenAISummarizationContent(
            uiState = GenAISummarizationUiState.Success("This is the generated summary."),
            textInput = "This is a sample text to summarize.",
            onTextInputChanged = {},
            onSummarizeClicked = {},
            onClearClicked = {},
            onAddSampleTextClicked = {},
        )
    }
}
