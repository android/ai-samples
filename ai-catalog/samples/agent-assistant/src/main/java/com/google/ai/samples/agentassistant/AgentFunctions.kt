package com.google.ai.samples.agentassistant

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema

val getSamplesFunctionDeclaration = FunctionDeclaration(
    name = "get_samples",
    description = "list all available samples in the app.",
    parameters = emptyMap()
)

val navigateToSampleFunctionDeclaration = FunctionDeclaration(
    name = "navigate_to_sample",
    description = "navigate to a sample in the app.",
    parameters = mapOf(
        "sample_route" to Schema.string(),
    )
)
