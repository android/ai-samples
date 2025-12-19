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
package com.android.ai.samples.geminiliveagent.ui

sealed interface AgentScreenUiState {
    data object Initial : AgentScreenUiState

    data class Success(
        val liveSessionState: LiveSessionState,
        val inputTranscription: String,
        val outputTranscription: String,
    ) : AgentScreenUiState

    data class Error(
        val liveSessionState: LiveSessionState,
    ) : AgentScreenUiState
}

sealed interface LiveSessionState {
    data object NotReady : LiveSessionState
    data object Ready : LiveSessionState
    data object Running : LiveSessionState
    data object Error : LiveSessionState
}
