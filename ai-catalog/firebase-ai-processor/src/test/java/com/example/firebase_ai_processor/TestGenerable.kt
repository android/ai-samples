package com.example.firebase_ai_processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.io.File

class TestGenerable {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun MyTest() {
        val source = SourceFile.new(
            "Chapter.kt",
            """
                package com.example.firebase_ai
                
                import com.example.annotations.Generable
                import com.example.annotations.Guide

                @Generable("Self-contained section of the video")
                data class Chapter(
                    @Guide(description = "Chapter start in milliseconds")
                    val timestamp: Long,
                    val title: String,
                )
            """,
        )

        val compilation = KotlinCompilation().apply {
            kspWithCompilation = true
            configureKsp(true) {
                symbolProcessorProviders.addAll(listOf(GenerableProcessorProvider()))
            }
            sources = listOf(source)
            inheritClassPath = true

            // Set a predictable output directory
            workingDir = File("build/ksp-test-outputs")
                .apply { mkdirs() } // Ensure the directory exists
        }

        val result = compilation.compile()

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        // Test diagnostic output of compiler

//        assertThat(result.messages).contains("My annotation processor was called")

        // Load compiled classes and inspect generated code through reflection

        // 1. Load the generated class with its FULLY QUALIFIED name
        val generatedClassName = "com.example.firebase_ai.ChapterSchemaKt"
        val generatedClass = result.classLoader.loadClass(generatedClassName)

//        // 2. Get the static 'chapterSchema' field from the class
//        val field = generatedClass.getDeclaredField("chapterSchema")
//        field.isAccessible = true
//
//        // 3. Get the actual Schema object instance from the static field
//        val schemaInstance = field.get(null) as Schema
//
//        // 4. Now, perform assertions on the Schema object
//        assertNotNull(schemaInstance)
//        assertThat(schemaInstance.description).isEqualTo("Self-contained section of the video")
//
//        val properties = schemaInstance.properties ?: emptyMap()
//        assertThat(properties).hasSize(2)
//        assertThat(properties["timestamp"]).isNotNull
//        assertThat(properties["timestamp"]?.description).isEqualTo("Chapter start in milliseconds")
//        assertThat(properties["title"]).isNotNull
//        assertThat(properties["title"]?.description).isNull()
    }
}