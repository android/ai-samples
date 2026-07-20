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

package com.example.jetpacker.core.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.genai.common.GenAiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInputManagerTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var voiceInputManager: VoiceInputManager

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    // We explicitly DO NOT initialize MlKitContext here.
    // This simulates an environment where speech recognition services fail to bind,
    // allowing us to verify the manager's robust initialization error handling.
    voiceInputManager = VoiceInputManager()
  }

  @Test
  fun whenSpeechRecognizerIsNull_handleListenToggleSetsError() = runTest {
    voiceInputManager.setPermissionGranted(true)
    voiceInputManager.handleListenToggle(onPermissionRequired = {})
    advanceUntilIdle()

    val state = voiceInputManager.uiState.value
    assertFalse(state.isListening)
    assertTrue(state.isError)
    assertTrue(state.showDialog)
    assertEquals("Speech recognizer could not be initialized.", state.statusText)
  }

  @Test
  fun cancelListening_clearsErrorState() = runTest {
    voiceInputManager.setPermissionGranted(true)
    voiceInputManager.handleListenToggle(onPermissionRequired = {})
    advanceUntilIdle()

    var state = voiceInputManager.uiState.value
    assertTrue(state.isError)

    voiceInputManager.cancelListening()
    state = voiceInputManager.uiState.value

    assertFalse(state.isListening)
    assertFalse(state.isError)
    assertFalse(state.showDialog)
  }

  @Test
  fun userFriendlyErrorMapping_translatesExceptionsCorrectly() {
    val method =
      VoiceInputManager::class
        .java
        .getDeclaredMethod("getUserFriendlyErrorMessage", Throwable::class.java)
    method.isAccessible = true

    val aiCoreException =
      GenAiException(
        "Speech recognition engine is closed due to internal error: ERROR_TYPE_AICORE_NOT_ENABLED_RUNTIME_LIMITS",
        null,
        0,
      )
    val mappedAiCore = method.invoke(voiceInputManager, aiCoreException) as String
    assertEquals(
      "AI Core speech engine runtime limits exceeded. Please wait a few moments and try again.",
      mappedAiCore,
    )

    val permissionException = Exception("PERMISSION_DENIED: missing RECORD_AUDIO")
    val mappedPermission = method.invoke(voiceInputManager, permissionException) as String
    assertEquals("Microphone permission is required for voice input.", mappedPermission)

    val networkException = Exception("timeout or NETWORK_ERROR during connect")
    val mappedNetwork = method.invoke(voiceInputManager, networkException) as String
    assertEquals("Network error. Please check your connection.", mappedNetwork)

    val genericException = Exception("Some unexpected issue")
    val mappedGeneric = method.invoke(voiceInputManager, genericException) as String
    assertEquals(
      "Speech recognition engine was closed due to an internal error: Some unexpected issue",
      mappedGeneric,
    )
  }
}
