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
package com.android.ai.samples.imagenediting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh // Icon for Inpaint/Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.ai.samples.imagenediting.R

@Composable
fun GenerationInput(
    uiState: ImagenEditingUIState, // <-- New parameter: Current UI State
    onGenerateClick: (String) -> Unit,
    onInpaintClick: (prompt: String) -> Unit, // <-- Changed: Now only takes prompt
    enabled: Boolean, // General enabled state (e.g., not loading)
    modifier: Modifier = Modifier,
) {
    val placeholder = stringResource(R.string.editing_placeholder_prompt_entry)
    // Keep prompt text in this component, ImagenScreen will get it via callback
    var promptTextField by rememberSaveable { mutableStateOf(placeholder) }

    // Determine if the inpaint button should be specifically enabled
    val canInpaint = uiState is ImagenEditingUIState.ImageMasked && enabled

    // Determine if the generate button should be enabled
    val canGenerate = uiState !is ImagenEditingUIState.ImageMasked && enabled

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        TextField(
            value = promptTextField,
            onValueChange = { promptTextField = it },
            label = { Text(stringResource(R.string.editing_prompt_label)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    // Decide which action to take on "Send"
                    if (uiState is ImagenEditingUIState.ImageMasked) {
                        if (canInpaint) onInpaintClick(promptTextField)
                    } else {
                        if (canGenerate) onGenerateClick(promptTextField)
                    }
                },
            ),
        )

        // Show Generate button if not in ImageMasked state
        if (uiState !is ImagenEditingUIState.ImageMasked) {
            Button(
                onClick = {
                    onGenerateClick(promptTextField)
                },
                enabled = canGenerate,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null, // Decorative
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.editing_generate_button))
            }
        }

        // Show Inpaint button if in ImageMasked state
        if (uiState is ImagenEditingUIState.ImageMasked) {
            Button(
                onClick = {
                    onInpaintClick(promptTextField)
                },
                enabled = canInpaint,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.AutoFixHigh, // Using a different icon for inpainting
                    contentDescription = null, // Decorative
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.editing_inpaint_button)) // Add this string
            }
        }
    }
}
