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
package com.android.ai.samples.genai_writing_assistance

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.samples.geminimultimodal.R
import com.android.ai.theme.AISampleCatalogTheme
import com.android.ai.theme.surfaceContainerHighestLight
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.SecondaryButton
import com.android.ai.uicomponent.UndoButton
import com.google.mlkit.genai.rewriting.RewriterOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAIWritingAssistanceScreen(viewModel: GenAIWritingAssistanceViewModel = hiltViewModel()) {
    var showRewriteOptionsDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val proofreadSampleTextOptions = stringArrayResource(R.array.proofread_sample_text)
    val rewriteSampleTextOptions = stringArrayResource(R.array.rewrite_sample_text)

    var textInput by rememberSaveable { mutableStateOf("") }

    GenAIWritingAssistanceContent(
        uiState = uiState,
        textInput = textInput,
        onTextInputChanged = { textInput = it },
        onProofreadClicked = { viewModel.proofread(textInput) },
        onRewriteClicked = {
            showRewriteOptionsDialog = true
        },
        onClearClicked = {
            viewModel.clearGeneratedText()
            textInput = ""
        },
        onAddProofreadTextClicked = { textInput = proofreadSampleTextOptions.random() },
        onAddRewriteTextClicked = { textInput = rewriteSampleTextOptions.random() },
    )

    if (showRewriteOptionsDialog) {
        RewriteOptionsDialog(
            onConfirm = { rewriteStyleSelected ->
                showRewriteOptionsDialog = false
                viewModel.rewrite(
                    textInput,
                    rewriteStyleSelected.rewriteStyle,
                    context,
                )
            },
            onDismissRequest = {
                showRewriteOptionsDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenAIWritingAssistanceContent(
    uiState: GenAIWritingAssistanceUiState,
    textInput: String,
    onTextInputChanged: (String) -> Unit,
    onProofreadClicked: () -> Unit,
    onRewriteClicked: () -> Unit,
    onClearClicked: () -> Unit,
    onAddProofreadTextClicked: () -> Unit,
    onAddRewriteTextClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.genai_writing_assistance_title_bar),
                sampleDescription = stringResource(R.string.genai_writing_assistance_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/genai-writing-assistance",
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
            Column(
                Modifier
                    .padding(top = 16.dp)
                    .imePadding()
                    .widthIn(max = 646.dp)
                    .fillMaxHeight(),
            ) {
                when (val state = uiState) {
                    GenAIWritingAssistanceUiState.CheckingFeatureStatus ->
                        // TODO: Replace with loading animation
                        DisplayedText(
                            textToDisplay = stringResource(id = R.string.checking_feature_status),
                            isStatusText = true,
                        )

                    is GenAIWritingAssistanceUiState.DownloadingFeature ->
                        DisplayedText(
                            stringResource(
                                id = R.string.genai_writing_assistance_downloading,
                                state.bytesDownloaded,
                                state.bytesToDownload,
                            ),
                            isStatusText = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                    is GenAIWritingAssistanceUiState.Error ->
                        DisplayedText(stringResource(state.errorMessageStringRes), isStatusText = true)

                    GenAIWritingAssistanceUiState.Initial -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {

                            TextField(
                                placeholder = { Text(stringResource(R.string.genai_writing_assistance_text_input_label)) },
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
                                    text = stringResource(R.string.genai_writing_assistance_proofread_sample_text_btn),
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_add_text),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    onClick = onAddProofreadTextClicked,
                                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                                )
                                SecondaryButton(
                                    text = stringResource(R.string.genai_writing_assistance_rewrite_sample_text_btn),
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_add_text),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    onClick = onAddRewriteTextClicked,
                                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                                )
                            } else {
                                GenerateButton(
                                    text = stringResource(R.string.genai_writing_assistance_proofread_btn),
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_text),
                                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                                    onClick = onProofreadClicked,
                                )
                                GenerateButton(
                                    text = stringResource(R.string.genai_writing_assistance_rewrite_btn),
                                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_text),
                                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                                    onClick = onRewriteClicked,
                                )
                            }
                        }
                    }

                    is GenAIWritingAssistanceUiState.Generating ->
                        // TODO: Replace with loading animation
                        DisplayedText(stringResource(R.string.genai_writing_assistance_generating), modifier = Modifier.fillMaxWidth())

                    is GenAIWritingAssistanceUiState.Success -> {
                        DisplayedText(state.generatedOutput, modifier = modifier.weight(1f).fillMaxWidth())

                        UndoButton(
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                            onClick = onClearClicked,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RewriteOptionsDialog(onConfirm: (rewriteStyle: RewriteStyle) -> Unit, onDismissRequest: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            val radioOptions = RewriteStyle.entries
            val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
            Column(modifier.selectableGroup()) {
                radioOptions.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (option == selectedOption),
                                onClick = { onOptionSelected(option) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null, // null recommended for accessibility with screen readers
                        )
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(id = R.string.genai_writing_assistance_dismiss_btn))
                    }
                    TextButton(
                        onClick = {
                            onConfirm(selectedOption)
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(id = R.string.genai_writing_assistance_confirm_btn))
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
fun GenAIWritingAssistanceContentPreview_Initial_EmptyText() {
    AISampleCatalogTheme {
        GenAIWritingAssistanceContent(
            uiState = GenAIWritingAssistanceUiState.Initial,
            textInput = "",
            onTextInputChanged = {},
            onProofreadClicked = {},
            onRewriteClicked = {},
            onClearClicked = {},
            onAddProofreadTextClicked = {},
            onAddRewriteTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAIWritingAssistanceContentPreview_Initial_WithTextText() {
    AISampleCatalogTheme {
        GenAIWritingAssistanceContent(
            uiState = GenAIWritingAssistanceUiState.Initial,
            textInput = stringResource(R.string.genai_proofread_sample_text_1),
            onTextInputChanged = {},
            onProofreadClicked = {},
            onRewriteClicked = {},
            onClearClicked = {},
            onAddProofreadTextClicked = {},
            onAddRewriteTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAIWritingAssistanceContentPreview_CheckingFeatureStatus() {
    AISampleCatalogTheme {
        GenAIWritingAssistanceContent(
            uiState = GenAIWritingAssistanceUiState.CheckingFeatureStatus,
            textInput = "",
            onTextInputChanged = {},
            onProofreadClicked = {},
            onRewriteClicked = {},
            onClearClicked = {},
            onAddProofreadTextClicked = {},
            onAddRewriteTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAIWritingAssistanceContentPreview_Error() {
    AISampleCatalogTheme {
        GenAIWritingAssistanceContent(
            uiState = GenAIWritingAssistanceUiState.Error(R.string.feature_check_fail),
            textInput = "",
            onTextInputChanged = {},
            onProofreadClicked = {},
            onRewriteClicked = {},
            onClearClicked = {},
            onAddProofreadTextClicked = {},
            onAddRewriteTextClicked = {},
        )
    }
}

@Preview
@Composable
fun GenAIWritingAssistanceContentPreview_Success() {
    AISampleCatalogTheme {
        GenAIWritingAssistanceContent(
            uiState = GenAIWritingAssistanceUiState.Success(
                "A fluffy golden retriever, wearing tiny spectacles, diligently typed lines of code",
            ),
            textInput = "",
            onTextInputChanged = {},
            onProofreadClicked = {},
            onRewriteClicked = {},
            onClearClicked = {},
            onAddProofreadTextClicked = {},
            onAddRewriteTextClicked = {},
        )
    }
}

enum class RewriteStyle(
    val rewriteStyle: Int,
    val displayName: String,
) {
    ELABORATE(RewriterOptions.OutputType.ELABORATE, "Elaborate"),
    EMOJIFY(RewriterOptions.OutputType.EMOJIFY, "Emojify"),
    SHORTEN(RewriterOptions.OutputType.SHORTEN, "Shorten"),
    FRIENDLY(RewriterOptions.OutputType.FRIENDLY, "Friendly"),
    PROFESSIONAL(RewriterOptions.OutputType.PROFESSIONAL, "Professional"),
    REPHRASE(RewriterOptions.OutputType.REPHRASE, "Rephrase"),
}
