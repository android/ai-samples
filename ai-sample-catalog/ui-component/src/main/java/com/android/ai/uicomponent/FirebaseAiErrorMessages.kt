/*
 * Copyright 2026 The Android Open Source Project
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
package com.android.ai.uicomponent

import android.util.Log

/**
 * Maps Firebase AI Logic failures to actionable messages for sample UIs.
 *
 * Cloud samples currently surface [Throwable.message] directly, which is often a raw
 * 403 ("GenerateContent are blocked") or quota error. Those strings do not explain
 * API key restrictions or billing requirements.
 *
 * Use [toUserFacingAiErrorMessage] with a log tag from sample ViewModels so the raw
 * exception remains in Logcat for developers.
 */
object FirebaseAiErrorMessages {
    const val API_BLOCKED =
        "Requests to Firebase AI Logic are blocked. Enable the Firebase AI Logic API " +
            "and include it in this app's Android API key restrictions in Google Cloud Console."
    const val BILLING_REQUIRED =
        "This model or feature requires a billed Firebase project (Blaze plan). " +
            "Image generation models such as Nano Banana typically are not available on the free Spark plan."
    const val PROMPT_BLOCKED =
        "The model blocked this request. Try a different prompt or image. " +
            "If this keeps happening, review the safety settings for the sample."
    const val GENERIC = "Something went wrong, try again"
}

fun Throwable.toUserFacingAiErrorMessage(logTag: String): String {
    Log.e(logTag, "Firebase AI request failed", this)
    return toUserFacingAiErrorMessage()
}

fun Throwable.toUserFacingAiErrorMessage(): String {
    val errorText = collectErrorText(this)
    val classNames = collectClassNames(this)

    return when {
        classNames.any { it.contains("promptblocked") } -> FirebaseAiErrorMessages.PROMPT_BLOCKED
        isApiAccessBlocked(errorText) -> FirebaseAiErrorMessages.API_BLOCKED
        isBillingOrQuota(errorText) -> FirebaseAiErrorMessages.BILLING_REQUIRED
        !message.isNullOrBlank() -> message.orEmpty()
        else -> FirebaseAiErrorMessages.GENERIC
    }
}

private fun collectErrorText(throwable: Throwable): String {
    return generateSequence(throwable) { it.cause }
        .take(5)
        .joinToString(separator = " ") { "${it.javaClass.simpleName} ${it.message.orEmpty()}" }
        .lowercase()
}

private fun collectClassNames(throwable: Throwable): List<String> {
    return generateSequence(throwable) { it.cause }
        .take(5)
        .map { it.javaClass.simpleName.lowercase() }
        .toList()
}

private fun isApiAccessBlocked(errorText: String): Boolean {
    return errorText.contains("firebasevertexai") ||
        errorText.contains("are blocked") ||
        (errorText.contains("requests to this api") && errorText.contains("blocked"))
}

private fun isBillingOrQuota(errorText: String): Boolean {
    return errorText.contains("billing") ||
        errorText.contains("quota") ||
        errorText.contains("resource exhausted") ||
        errorText.contains("429")
}
