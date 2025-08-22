package com.example.firebase_ai

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Creates and configures a [TypedGenerativeModel] that is specialized to generate
 * content matching the type [T].
 *
 * @param T The @Serializable and @Generable data class this model will be configured for.
 * @param modelName The name of the backend model to use.
 * @return A [TypedGenerativeModel] instance ready to be used.
 */
inline fun <reified T : Any> FirebaseAI.generativeModel(
    modelName: String,
    generationConfig: GenerationConfig? = null
    // TODO: Add other parameters like safetySettings here
): TypedGenerativeModel<T> {

    val runtimeProvider = generableProviders[T::class] as? SchemaAndSerializerProvider<T>

    val schema = getSchemaForType<T>(runtimeProvider)

    val serializer: KSerializer<T> = getSerializerForType<T>(runtimeProvider)

    val newGenerationConfig = generationConfig {
        // TODO: copy from incoming generationConfig
        responseMimeType = "application/json"
        responseSchema = schema
    }

    val baseModel = this.generativeModel(
        modelName = modelName,
        generationConfig = newGenerationConfig
    )

    return TypedGenerativeModel(baseModel, serializer)
}

/**
 * Creates a TypedGenerativeModel pre-configured to return a List of a @Generable type.
 *
 * @param T The @Generable data class for the items in the list.
 * @param modelName The name of the backend model to use.
 * @param listDescription A natural language description of the list's purpose for the AI.
 * @return A [TypedGenerativeModel] instance ready to generate a List<T>.
 */
inline fun <reified T : Any> FirebaseAI.generativeModelForList(
    modelName: String,
    listDescription: String? = null,
    generationConfig: GenerationConfig? = null
): TypedGenerativeModel<List<T>> {
    val runtimeProvider = generableProviders[T::class] as? SchemaAndSerializerProvider<T>

    val itemSchema = getSchemaForType(runtimeProvider)
    val itemSerializer = getSerializerForType(runtimeProvider)

    // 3. Dynamically create the schema and serializer for the List.
    val listSchema = Schema.array(itemSchema, listDescription)
    val listSerializer = ListSerializer(itemSerializer)

    val newGenerationConfig = generationConfig {
        // TODO: copy from incoming generationConfig
        responseMimeType = "application/json"
        responseSchema = listSchema
    }

    val baseModel = this.generativeModel(
        modelName = modelName,
        generationConfig = newGenerationConfig
    )

    return TypedGenerativeModel(baseModel, listSerializer)
}

inline fun <reified T : Any> getSerializerForType(runtimeProvider: SchemaAndSerializerProvider<T>?): KSerializer<T> = when {
    runtimeProvider != null -> runtimeProvider.serializer()
    T::class == Float::class -> Float.serializer()
    T::class == Long::class -> Long.serializer()
    T::class == Int::class -> Int.serializer()
    T::class == String::class -> String.serializer()
    T::class == Boolean::class -> Boolean.serializer()
    else -> error("Could not find a 'Generable' provider for ${T::class.simpleName}.")
} as KSerializer<T>

inline fun <reified T : Any> getSchemaForType(runtimeProvider: SchemaAndSerializerProvider<T>?): Schema = when {
    runtimeProvider != null -> runtimeProvider.schema()
    T::class == Float::class -> Schema.float()
    T::class == Long::class -> Schema.long()
    T::class == Int::class -> Schema.integer()
    T::class == String::class -> Schema.string()
    T::class == Boolean::class -> Schema.boolean()
    else -> error("Could not find a 'Generable' provider for ${T::class.simpleName}.")
    // TODO: handle lists
    // List::class -> Schema.array()
}
