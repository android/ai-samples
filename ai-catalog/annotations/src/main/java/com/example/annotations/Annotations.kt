package com.example.annotations

import kotlin.reflect.KClass

/**
 * Annotates a data class to trigger KSP code generation.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class Generable(val description: String = "")

/**
 * Annotates a property to provide a detailed description.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Guide(val description: String = "")

/**
 * A dependency-free interface used by the ServiceLoader for discovery.
 * KSP-generated objects will implement this to announce which class they are for.
 */
interface GenerableProvider {
    /** The class type this provider is for. */
    val targetClass: KClass<*>
}