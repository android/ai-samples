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

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.com.android.ai.samples.geminivideosummary.R
import java.util.Locale

/**
 * Composable function that provides controls for Text-to-Speech functionality.
 *
 * This function displays a UI that allows the user to:
 * - Select a language accent for the Text-to-Speech engine.
 * - Initiate speech synthesis for the provided text.
 * - Pause the ongoing speech.
 */
@Composable
fun TextToSpeechControls(
    isInitialized: Boolean,
    isSpeaking: Boolean,
    isPaused: Boolean,
    textToSpeech: TextToSpeech?,
    speechText: String,
    selectedAccent: Locale,
    accentOptions: List<Locale>,
    onSpeakingStateChange: (speaking: Boolean, paused: Boolean) -> Unit,
    isAccentDropdownExpanded: Boolean,
    onAccentSelected: (Locale) -> Unit,
    onAccentDropdownExpanded: (Boolean) -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = selectedAccent.displayLanguage,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.clickable { onAccentDropdownExpanded(!isAccentDropdownExpanded) },
                )
            },
            modifier = Modifier
                .clickable { onAccentDropdownExpanded(!isAccentDropdownExpanded) }
                .padding(end = 8.dp)
                .weight(1f),
        )
        DropdownMenu(
            expanded = isAccentDropdownExpanded,
            onDismissRequest = { onAccentDropdownExpanded(false) },
        ) {
            accentOptions.forEach { accent ->
                DropdownMenuItem(text = { Text(accent.displayLanguage) }, onClick = {
                    onAccentSelected(accent)
                    onAccentDropdownExpanded(false)
                })
            }
        }

        if (isInitialized && !isSpeaking) {
            Button(onClick = {
                handleSpeakButtonClick(
                    textToSpeech, speechText, selectedAccent, onSpeakingStateChange,
                )
            }) {
                Text(text = stringResource(R.string.text_listen_to_ai_output))
            }
        }

        if (isSpeaking && !isPaused) {
            Button(onClick = {
                textToSpeech?.stop()
                onSpeakingStateChange(false, true)
            }) {
                Text(text = stringResource(R.string.pause))
            }
        }
    }
}

private fun handleSpeakButtonClick(
    textToSpeech: TextToSpeech?,
    textForSpeech: String,
    selectedAccent: Locale,
    onSpeakingStateChange: (speaking: Boolean, paused: Boolean) -> Unit,
) {
    // Check if the voice and language is supported
    val result = textToSpeech?.language?.let {
        textToSpeech.setLanguage(selectedAccent)
    }
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.e("TextToSpeech", "Language not supported")
    } else {
        // Start speaking
        textToSpeech?.speak(
            textForSpeech, TextToSpeech.QUEUE_FLUSH, null, null,
        )
        onSpeakingStateChange(true, false)
    }
}
