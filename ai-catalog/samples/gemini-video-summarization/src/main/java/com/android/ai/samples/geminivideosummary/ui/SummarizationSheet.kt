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
package com.android.ai.samples.geminivideosummary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.ai.samples.geminivideosummary.viewmodel.SummarizationState
import com.android.ai.samples.geminivideosummary.viewmodel.TtsState
import com.android.ai.samples.geminivideosummary.viewmodel.VideoSummarizationState
import com.android.ai.theme.extendedColorScheme
import com.android.ai.uicomponent.UndoButton
import com.google.com.android.ai.samples.geminivideosummary.R
import java.util.Locale

private val accentOptions = listOf(
    Locale.UK,
    Locale.US,
    Locale.CANADA,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarizationSheet(
    uiState: VideoSummarizationState,
    modifier: Modifier = Modifier,
    onTtsStateChanged: (TtsState) -> Unit,
    onTtsInitializationResult: (Boolean, String?) -> Unit,
    onRedo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val summarizationState = uiState.summarizationState as? SummarizationState.Success ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            TextToSpeechControls(
                title = uiState.selectedVideo?.titleResId,
                ttsState = summarizationState.ttsState,
                speechText = summarizationState.summarizedText,
                onTtsStateChange = onTtsStateChanged,
                onInitializationResult = onTtsInitializationResult,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutputTextDisplay(
                summarizationState.summarizedText,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                Modifier
                    .wrapContentHeight(),
            ) {
                Text(
                    text = stringResource(R.string.text_generated_with_gemini),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .background(
                            color = extendedColorScheme.geminiProFlash,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                )
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(1.dp),
                )
                UndoButton(
                    onClick = onRedo,
                )
            }
        }
    }
}
