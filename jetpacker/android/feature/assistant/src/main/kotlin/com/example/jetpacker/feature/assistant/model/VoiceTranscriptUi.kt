/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetpacker.feature.assistant.model

import com.example.jetpacker.core.speech.VoiceInputState

/**
 * The subset of [VoiceInputState] that the assistant UI actually renders.
 *
 * [VoiceInputState] also carries high-frequency fields such as `audioLevel`, which update on every
 * microphone sample. Projecting down to only the fields the UI reads means the resulting
 * [kotlinx.coroutines.flow.StateFlow] only emits (and the UI only recomposes) when something
 * visible changes.
 *
 * If the assistant UI starts reading another field from [VoiceInputState], add it here.
 */
data class VoiceTranscriptUi(
  val isListening: Boolean = false,
  val partialTranscription: String = "",
  val transcription: String = "",
)

internal fun VoiceInputState.toVoiceTranscriptUi(): VoiceTranscriptUi =
  VoiceTranscriptUi(
    isListening = isListening,
    partialTranscription = partialTranscription,
    transcription = transcription,
  )
