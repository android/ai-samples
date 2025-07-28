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

import android.content.Context
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.ai.samples.geminivideosummary.viewmodel.TtsState
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
    ttsState: TtsState,
    speechText: String,
    selectedAccent: Locale,
    accentOptions: List<Locale>,
    onTtsStateChange: (TtsState) -> Unit,
    onAccentSelected: (Locale) -> Unit,
    onInitializationResult: (Boolean, String?) -> Unit,
) {
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isAccentDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(key1 = true) {
        textToSpeech = initializeTextToSpeech(context, onInitializationResult)
        onDispose {
            textToSpeech?.shutdown()
        }
    }

    LaunchedEffect(speechText, selectedAccent) {
        textToSpeech?.stop()
        onTtsStateChange(TtsState.Idle)
    }

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
                    modifier = Modifier.clickable { isAccentDropdownExpanded = !isAccentDropdownExpanded },
                )
            },
            modifier = Modifier
                .clickable { isAccentDropdownExpanded = !isAccentDropdownExpanded }
                .padding(end = 8.dp)
                .weight(1f),
        )

        DropdownMenu(
            expanded = isAccentDropdownExpanded,
            onDismissRequest = { isAccentDropdownExpanded = false },
        ) {
            accentOptions.forEach { accent ->
                DropdownMenuItem(
                    text = { Text(accent.displayLanguage) },
                    onClick = {
                        onAccentSelected(accent)
                        isAccentDropdownExpanded = false
                    },
                )
            }
        }

        if (ttsState == TtsState.Idle || ttsState == TtsState.Paused) {
            Button(
                onClick = {
                    handleSpeakButtonClick(
                        textToSpeech, speechText, selectedAccent, onTtsStateChange,
                    )
                },
            ) {
                Text(text = stringResource(R.string.text_listen_to_ai_output))
            }
        }

        if (ttsState == TtsState.Playing) {
            Button(
                onClick = {
                    textToSpeech?.stop()
                    onTtsStateChange(TtsState.Paused)
                },
            ) {
                Text(text = stringResource(R.string.pause))
            }
        }
    }
}

private fun initializeTextToSpeech(context: Context, onResult: (Boolean, String?) -> Unit): TextToSpeech {
    return TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onResult(true, null)
        } else {
            val reason = when (status) {
                TextToSpeech.ERROR -> R.string.tts_generic_error
                TextToSpeech.ERROR_SYNTHESIS -> R.string.tts_synthesis_error
                TextToSpeech.ERROR_SERVICE -> R.string.tts_service_error
                TextToSpeech.ERROR_OUTPUT -> R.string.tts_output_error
                TextToSpeech.ERROR_NETWORK -> R.string.tts_network_error
                TextToSpeech.ERROR_NETWORK_TIMEOUT -> R.string.tts_network_timeout_error
                TextToSpeech.ERROR_NOT_INSTALLED_YET -> R.string.tts_not_installed_error
                else -> R.string.tts_unknown_error
            }
            val errorMessage = context.getString(R.string.tts_error_message, context.getString(reason))
            Log.e("TextToSpeech", errorMessage)
            onResult(false, errorMessage)
        }
    }
}

private fun handleSpeakButtonClick(
    textToSpeech: TextToSpeech?,
    textForSpeech: String,
    selectedAccent: Locale,
    onTtsStateChange: (TtsState) -> Unit,
) {
    // Check if the voice and language is supported
    val result = textToSpeech?.voice?.locale?.let {
        textToSpeech.setLanguage(selectedAccent)
    }
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.e("TextToSpeech", "Language not supported")
    } else {
        // Start speaking
        textToSpeech?.speak(
            textForSpeech, TextToSpeech.QUEUE_FLUSH, null, null,
        )
        onTtsStateChange(TtsState.Playing)
    }
}
