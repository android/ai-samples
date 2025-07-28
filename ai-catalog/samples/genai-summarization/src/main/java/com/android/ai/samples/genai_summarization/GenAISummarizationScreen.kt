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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminimultimodal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAISummarizationScreen(viewModel: GenAISummarizationViewModel = hiltViewModel()) {
    val sampleTextOptions = stringArrayResource(R.array.summarization_sample_text)

    val sheetState = rememberModalBottomSheetState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                title = {
                    Text(text = stringResource(id = R.string.genai_summarization_title_bar))
                },
            )
        },
    ) { innerPadding ->

        Column(
            Modifier
                .padding(12.dp)
                .padding(innerPadding),
        ) {
            // Text input box
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text(stringResource(id = R.string.genai_summarization_text_input_label)) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(.8f),
            )

            // Summarize button
            Button(
                onClick = {
                    viewModel.summarize(textInput)
                },
                enabled = textInput.isNotEmpty(),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(id = R.string.genai_summarization_summarize_btn),
                )
            }

            // Extra options buttons
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                OutlinedButton(
                    onClick = { textInput = sampleTextOptions.random() },
                    Modifier.padding(5.dp),
                ) {
                    Text(
                        stringResource(id = R.string.genai_summarization_add_text_btn),
                    )
                }
                OutlinedButton(
                    onClick = { textInput = "" },
                    Modifier.padding(5.dp),
                ) {
                    Text(
                        stringResource(id = R.string.genai_summarization_reset_btn),
                    )
                }
            }
        }

        if (uiState !is GenAISummarizationUiState.Initial) {
            val bottomSheetText = when (val state = uiState) {
                is GenAISummarizationUiState.DownloadingFeature -> stringResource(
                    id = R.string.summarization_downloading,
                    state.bytesDownloaded,
                    state.bytesToDownload,
                )
                is GenAISummarizationUiState.Error -> stringResource(state.errorMessageStringRes)
                is GenAISummarizationUiState.Generating -> state.generatedOutput
                GenAISummarizationUiState.Initial -> ""
                is GenAISummarizationUiState.Success -> state.generatedOutput
                GenAISummarizationUiState.CheckingFeatureStatus -> stringResource(id = R.string.summarization_checking_feature_status)
            }
            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.clearGeneratedSummary()
                },
                sheetState = sheetState,
            ) {
                Text(
                    text = bottomSheetText,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 24.dp,
                        start = 24.dp,
                        end = 24.dp,
                    ),
                )
            }
        }
    }
}
