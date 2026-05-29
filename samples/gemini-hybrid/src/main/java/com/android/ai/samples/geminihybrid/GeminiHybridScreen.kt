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
@file:OptIn(PublicPreviewAPI::class, ExperimentalMaterial3ExpressiveApi::class)

package com.android.ai.samples.geminihybrid

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.theme.surfaceContainerHighestLight
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.UndoButton
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.type.PublicPreviewAPI


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiHybridScreen(viewModel: GeminiHybridViewModel = hiltViewModel()) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    AISampleCatalogTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SampleDetailTopAppBar(
                    sampleName = stringResource(R.string.gemini_hybrid_title),
                    sampleDescription = stringResource(R.string.gemini_hybrid_description),
                    sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/gemini-hybrid",
                    onBackClick = { backDispatcher?.onBackPressed() },
                )
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(40.dp))
                    .background(color = surfaceContainerHighestLight)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                val scrollState = rememberScrollState()

                Column(
                    Modifier
                        .padding(top = 16.dp)
                        .imePadding()
                        .widthIn(max = 646.dp)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                ) {
                    Text(
                        text = stringResource(R.string.gemini_hotel_review),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(8.dp),
                    )

                    val status = uiState.status
                    when {
                        status is GeminiStatus.Initial -> {
                            InitialReviewUi(
                                tags = viewModel.tags,
                                selectedTags = uiState.selectedTags,
                                onTagToggle = viewModel::toggleTag,
                                selectedMode = uiState.selectedMode,
                                onModeSelected = viewModel::setInferenceMode,
                                onGenerate = {
                                    val tagStrings =
                                        uiState.selectedTags.map { ContextCompat.getString(context, it) }
                                    viewModel.generateReview(tagStrings)
                                },
                            )
                        }

                        status is GeminiStatus.Generating && !status.isTranslation -> {
                            GeneratingUi(status)
                        }

                        status is GeminiStatus.Error -> {
                            ErrorUi(status.message, onReset = viewModel::reset)
                        }

                        else -> {
                            SuccessReviewUi(
                                reviewText = uiState.reviewText,
                                reviewInferenceStatus = uiState.reviewInferenceStatus,
                                onReviewTextChanged = viewModel::updateReviewText,
                                languageKeys = viewModel.languageMap.keys.toList(),
                                languageMap = viewModel.languageMap,
                                selectedLanguage = uiState.selectedLanguage,
                                onLanguageSelected = viewModel::setSelectedLanguage,
                                onTranslate = {
                                    viewModel.translate(
                                        uiState.reviewText,
                                        uiState.selectedLanguage
                                    )
                                },
                                onReset = viewModel::reset,
                                generationStatus = status,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InitialReviewUi(
    tags: List<Int>,
    selectedTags: List<Int>,
    onTagToggle: (Int) -> Unit,
    selectedMode: InferenceMode,
    onModeSelected: (InferenceMode) -> Unit,
    onGenerate: () -> Unit,
) {
    Text(
        stringResource(R.string.select_topics_for_your_review),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleMedium,
    )
    FlowRow(
        modifier = Modifier
            .padding(8.dp),
    ) {
        tags.forEach { tagResId ->
            val isSelected = selectedTags.contains(tagResId)
            OutlinedToggleButton(
                checked = isSelected,
                onCheckedChange = { onTagToggle(tagResId) },
                colors = ToggleButtonDefaults.outlinedToggleButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                Text(
                    stringResource(tagResId),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    Spacer(Modifier.height(50.dp))
    InferenceModeDropdown(
        selectedMode = selectedMode,
        onModeSelected = onModeSelected,
    )

    GenerateButton(
        text = stringResource(R.string.gemini_hybrid_generate_btn),
        icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_text),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 8.dp, end = 8.dp),
        enabled = selectedTags.isNotEmpty(),
        onClick = onGenerate,
    )
}

@Composable
fun GeneratingUi(status: GeminiStatus.Generating) {
    val statusText = if (status.isCloud) {
        stringResource(R.string.gemini_hybrid_status_generating_cloud)
    } else {
        stringResource(R.string.gemini_hybrid_status_generating_on_device)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        StatusText(statusText)
        if (status.partialOutput.isNotEmpty()) {
            OutputText(status.partialOutput)
        }
    }
}

@Composable
fun SuccessReviewUi(
    reviewText: String,
    reviewInferenceStatus: Int?,
    onReviewTextChanged: (String) -> Unit,
    languageKeys: List<String>,
    languageMap: Map<String, Int>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onTranslate: () -> Unit,
    onReset: () -> Unit,
    generationStatus: GeminiStatus,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        reviewInferenceStatus?.let {
            StatusText(stringResource(it))
        }
        OutlinedTextField(
            value = reviewText,
            onValueChange = onReviewTextChanged,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .heightIn(max = 200.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
            LanguageDropdown(
                languageKeys = languageKeys,
                languageMap = languageMap,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
            )
        }

        GenerateButton(
            text = stringResource(R.string.gemini_hybrid_translate_btn),
            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_text),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
            enabled = reviewText.isNotBlank() && generationStatus !is GeminiStatus.Generating,
            onClick = onTranslate,
        )

        Spacer(modifier = Modifier.height(20.dp))
        when (generationStatus) {
            is GeminiStatus.Generating -> {
                if (generationStatus.isTranslation) {
                    val statusText = if (generationStatus.isCloud) {
                        stringResource(R.string.gemini_hybrid_status_generating_cloud)
                    } else {
                        stringResource(R.string.gemini_hybrid_status_generating_on_device)
                    }
                    StatusText(statusText)
                    if (generationStatus.partialOutput.isNotEmpty()) {
                        OutputText(generationStatus.partialOutput)
                    }
                }
            }

            is GeminiStatus.Success -> {
                if (generationStatus.isTranslation) {
                    val inferenceStatus = if (generationStatus.isCloud) {
                        R.string.gemini_hybrid_generated_cloud
                    } else {
                        R.string.gemini_hybrid_generated_on_device
                    }

                    StatusText(stringResource(inferenceStatus))
                    OutputText(generationStatus.output)
                }
            }

            else -> {}
        }

        UndoButton(
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            onClick = onReset,
        )
    }
}

@Composable
fun ErrorUi(message: String, onReset: () -> Unit) {
    Column {
        StatusText(message)
        UndoButton(
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            onClick = onReset,
        )
    }
}

@Composable
fun LanguageDropdown(
    languageKeys: List<String>,
    languageMap: Map<String, Int>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Text(stringResource(languageMap[selectedLanguage] ?: R.string.gemini_hybrid_lang_korean).uppercase())
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                    )
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            languageKeys.forEach { key ->
                DropdownMenuItem(
                    text = { Text(stringResource(languageMap[key]!!)) },
                    onClick = {
                        onLanguageSelected(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@PublicPreviewAPI
@Composable
fun InferenceModeDropdown(
    selectedMode: InferenceMode,
    onModeSelected: (InferenceMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf(
        InferenceMode.ONLY_ON_DEVICE to stringResource(R.string.gemini_hybrid_mode_only_on_device),
        InferenceMode.ONLY_IN_CLOUD to stringResource(R.string.gemini_hybrid_mode_only_cloud),
        InferenceMode.PREFER_ON_DEVICE to stringResource(R.string.gemini_hybrid_mode_prefer_on_device),
        InferenceMode.PREFER_IN_CLOUD to stringResource(R.string.gemini_hybrid_mode_prefer_cloud),
    )
    val selectedText = modes.find { it.first == selectedMode }?.second ?: ""

    Box(modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Text(selectedText)
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                    )
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            modes.forEach { (mode, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(8.dp),
    )
}

@Composable
fun OutputText(text: String, modifier: Modifier = Modifier) {
    TextField(
        value = text,
        onValueChange = {},
        readOnly = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
    )
}
