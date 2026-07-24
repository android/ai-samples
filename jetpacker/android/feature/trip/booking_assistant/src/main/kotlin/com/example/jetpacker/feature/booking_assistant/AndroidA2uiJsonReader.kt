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

package com.example.jetpacker.feature.booking_assistant

import android.util.JsonReader
import android.util.JsonToken
import androidx.a2ui.model.processor.A2uiJsonReader
import androidx.a2ui.model.processor.A2uiJsonToken
import java.io.StringReader

class AndroidA2uiJsonReader(json: String) : A2uiJsonReader {
    private val reader = JsonReader(StringReader(json))

    override fun peek(): A2uiJsonToken {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> A2uiJsonToken.BEGIN_ARRAY
            JsonToken.END_ARRAY -> A2uiJsonToken.END_ARRAY
            JsonToken.BEGIN_OBJECT -> A2uiJsonToken.BEGIN_OBJECT
            JsonToken.END_OBJECT -> A2uiJsonToken.END_OBJECT
            JsonToken.NAME -> A2uiJsonToken.NAME
            JsonToken.STRING -> A2uiJsonToken.STRING
            JsonToken.NUMBER -> A2uiJsonToken.NUMBER
            JsonToken.BOOLEAN -> A2uiJsonToken.BOOLEAN
            JsonToken.NULL -> A2uiJsonToken.NULL
            JsonToken.END_DOCUMENT -> A2uiJsonToken.END_DOCUMENT
            null -> A2uiJsonToken.END_DOCUMENT
        }
    }

    override fun beginObject() = reader.beginObject()
    override fun endObject() = reader.endObject()
    override fun beginArray() = reader.beginArray()
    override fun endArray() = reader.endArray()
    override fun hasNext(): Boolean = reader.hasNext()
    override fun nextName(): String = reader.nextName()
    override fun nextString(): String = reader.nextString()
    override fun nextBoolean(): Boolean = reader.nextBoolean()
    override fun nextDouble(): Double = reader.nextDouble()
    override fun nextInt(): Int = reader.nextInt()
    override fun nextLong(): Long = reader.nextLong()
    override fun nextNull() = reader.nextNull()
    override fun skipValue() = reader.skipValue()
    override fun close() = reader.close()
}
