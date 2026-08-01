package com.example.jetpacker.feature.booking_assistant

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.agui.core.types.RunAgentInput
import com.agui.core.types.UserMessage
import com.agui.core.types.TextInputContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.UUID

import com.agui.core.types.BaseEvent as AgUiEvent
import com.agui.core.types.RunStartedEvent
import com.agui.core.types.RunFinishedEvent
import com.agui.core.types.RunFinishedSuccessOutcome
import com.agui.core.types.TextMessageStartEvent
import com.agui.core.types.TextMessageContentEvent
import com.agui.core.types.TextMessageEndEvent
import com.agui.core.types.Role

@RunWith(RobolectricTestRunner::class)
class A2uiCatalogTest {
    @Test
    fun testPrintJson() {
        val agUiJson: Json = try {
            val clazz = Class.forName("com.agui.core.types.AgUiJsonKt")
            val method = clazz.getMethod("getAgUiJson")
            method.invoke(null) as Json
        } catch (e: Exception) {
            Json { ignoreUnknownKeys = true }
        }

        val itineraryContent = "{\"events\":[{\"id\":\"event-1\",\"type\":\"CULTURE\",\"title\":\"Versailles Palace Tour\",\"location\":\"Versailles\"}]}"

        val userMessage = UserMessage(
            id = UUID.randomUUID().toString(),
            content = itineraryContent,
            name = "user",
            contentParts = listOf(TextInputContent(itineraryContent))
        )
        
        val input = RunAgentInput(
            "test-thread-id",
            "test-run-id",
            "",
            JsonObject(emptyMap()),
            listOf(userMessage)
        )

        val jsonStr = agUiJson.encodeToString(RunAgentInput.serializer(), input)
        println("SERIALIZED_INPUT_JSON: $jsonStr")
    }

    @Test
    fun testPrintEvents() {
        val agUiJson: Json = try {
            val clazz = Class.forName("com.agui.core.types.AgUiJsonKt")
            val method = clazz.getMethod("getAgUiJson")
            method.invoke(null) as Json
        } catch (e: Exception) {
            Json { ignoreUnknownKeys = true }
        }

        val e1 = RunStartedEvent("threadId", "runId", null, null)
        val e2 = TextMessageStartEvent("messageId", Role.ASSISTANT, null, null)
        val e3 = TextMessageContentEvent("messageId", "delta", null, null)
        val e4 = TextMessageEndEvent("messageId", null, null)
        val e5 = RunFinishedEvent("threadId", "runId", null, RunFinishedSuccessOutcome, null, null)

        println("RunStartedEvent_JSON: " + agUiJson.encodeToString(AgUiEvent.serializer(), e1))
        println("TextMessageStartEvent_JSON: " + agUiJson.encodeToString(AgUiEvent.serializer(), e2))
        println("TextMessageContentEvent_JSON: " + agUiJson.encodeToString(AgUiEvent.serializer(), e3))
        println("TextMessageEndEvent_JSON: " + agUiJson.encodeToString(AgUiEvent.serializer(), e4))
        println("RunFinishedEvent_JSON: " + agUiJson.encodeToString(AgUiEvent.serializer(), e5))
    }
}
