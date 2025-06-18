/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.android.ai.samples.genai_summarization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.ai.samples.geminimultimodal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAISummarizationScreen(viewModel: GenAISummarizationViewModel = hiltViewModel()) {
    val sampleTextOptions = stringArrayResource(R.array.summarization_sample_text)

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val summarizationResult = viewModel.summarizationGenerated.collectAsState()
    var textInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ), title = {
                    Text(text = stringResource(id = R.string.genai_summarization_title_bar))
                })
        }) { innerPadding ->

        Column(
            Modifier
                .padding(12.dp)
                .padding(innerPadding)
        ) {
            // Text input box
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text(stringResource(id = R.string.genai_summarization_text_input_label)) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(.8f)
            )

            // Summarize button
            Button(
                onClick = {
                    showBottomSheet = true
                    viewModel.summarize(textInput, context)
                }, modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(id = R.string.genai_summarization_summarize_btn)
                )
            }

            // Extra options buttons
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                OutlinedButton(
                    onClick = { textInput = sampleTextOptions.random() },
                    Modifier.padding(5.dp)
                ) {
                    Text(
                        stringResource(id = R.string.genai_summarization_add_text_btn)
                    )
                }
                OutlinedButton(
                    onClick = { textInput = "" },
                    Modifier.padding(5.dp)
                ) {
                    Text(
                        stringResource(id = R.string.genai_summarization_reset_btn)
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.clearGeneratedSummary()
                }, sheetState = sheetState
            ) {
                Text(
                    text = summarizationResult.value,
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 24.dp,
                        start = 24.dp,
                        end = 24.dp
                    )
                )
            }
        }
    }
}