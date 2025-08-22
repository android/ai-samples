package com.example.firebase_ai

import com.google.firebase.ai.type.Candidate
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.PromptFeedback
import com.google.firebase.ai.type.UsageMetadata

/**
 * A wrapper for [GenerateContentResponse] that includes a typed, parsed result.
 *
 * @param T The type of the parsed content.
 * @property typedContent The response content parsed as an object of type [T], or null on failure.
 * @property rawResponse The original, unmodified [GenerateContentResponse] from the API.
 */
class TypedGenerateContentResponse<T>(
    val typedContent: T?,
    val rawResponse: GenerateContentResponse
) {
    // --- Delegating properties for convenience ---

    val candidates: List<Candidate>
        get() = rawResponse.candidates

    val promptFeedback: PromptFeedback?
        get() = rawResponse.promptFeedback

    val usageMetadata: UsageMetadata?
        get() = rawResponse.usageMetadata

    val text: String?
        get() = rawResponse.text

    val functionCalls: List<FunctionCallPart>
        get() = rawResponse.functionCalls
}