/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ai.samples.geminilivetodo.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.glimmer.onIndirectPointerGesture
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch

private const val TAG = "AudioExperience"

@OptIn(PublicPreviewAPI::class)
@Composable
fun AudioExperience(viewModel: TodoScreenViewModel) {
    val activity = LocalActivity.current as Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isMicOn = (uiState as? TodoScreenUiState.Success)?.isMicOn ?: false
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onIndirectPointerGesture(
                enabled = true,
                onSwipeForward = { /* onSwipeForward */ },
                onClick = {
                    val wasOn = isMicOn
                    Log.d(TAG, "Screen clicked - currently isMicOn: $wasOn")
                    if (wasOn) {
                        // Turn OFF
                        viewModel.toggleLiveSession(activity)
                    } else {
                        viewModel.toggleLiveSession(activity)
                        scope.launch {
                            // Give Gemini Live a moment to open the channel
                            kotlinx.coroutines.delay(500)
                            viewModel.session?.send(content { text("Turning Microphone On") })
                        }
                    }
                },
            )
            .focusTarget()
    )
}
