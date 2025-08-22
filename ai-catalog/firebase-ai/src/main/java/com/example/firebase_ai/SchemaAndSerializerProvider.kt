package com.example.firebase_ai

import com.google.firebase.ai.type.Schema
import kotlinx.serialization.KSerializer

/**
 * An internal interface that the SDK uses to get the schema and serializer.
 * The KSP-generated provider object will be cast to this at runtime.
 */
interface SchemaAndSerializerProvider<T> {
    fun schema(): Schema
    fun serializer(): KSerializer<T>
}