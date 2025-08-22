package com.example.firebase_ai

import com.example.annotations.GenerableProvider
import kotlin.jvm.java
import java.util.ServiceLoader
import kotlin.getValue
import kotlin.reflect.KClass

// The cache now stores instances of the public, dependency-free interface.
@PublishedApi
internal val generableProviders: Map<KClass<*>, GenerableProvider> by lazy {
    ServiceLoader.load(GenerableProvider::class.java, GenerableProvider::class.java.classLoader)
        .associateBy { it.targetClass }
}

