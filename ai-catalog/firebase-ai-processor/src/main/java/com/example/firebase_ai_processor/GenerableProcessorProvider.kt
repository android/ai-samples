package com.example.firebase_ai_processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class GenerableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return GenerableProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}