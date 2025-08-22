package com.example.firebase_ai_processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.example.annotations.Generable
import com.example.annotations.Guide
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class GenerableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private val generatedProviders = mutableSetOf<String>()

    private val schemaClassName = ClassName("com.google.firebase.ai.type", "Schema")
    private val kSerializerClassName = ClassName("kotlinx.serialization", "KSerializer")
    private val generableProviderClassName = ClassName("com.example.annotations", "GenerableProvider")
    private val internalProviderClassName =
        ClassName("com.example.firebase_ai", "SchemaAndSerializerProvider")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val generableSymbols = resolver.getSymbolsWithAnnotation(Generable::class.qualifiedName!!)
        val (validSymbols, invalidSymbols) = generableSymbols.partition { it.validate() }

        validSymbols
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDeclaration ->
                try {
                    handleClass(classDeclaration)
                } catch (e: Exception) {
                    logger.error("Error processing symbol: ${e.message}", classDeclaration)
                }
            }

        return invalidSymbols
    }

    override fun finish() {
        if (generatedProviders.isEmpty()) {
            return
        }

        val resourceFile = "META-INF/services/${generableProviderClassName.canonicalName}"

        // Create the file once.
        codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = "",
            fileName = resourceFile,
            extensionName = ""
        ).use { outputStream ->
            // Join all provider names with a newline and write them all at once.
            outputStream.write(generatedProviders.joinToString("\n").toByteArray())
        }
    }

    private fun handleClass(classDeclaration: KSClassDeclaration) {
        val generableAnnotation = classDeclaration.annotations.first {
            it.shortName.asString() == Generable::class.simpleName
        }
        val description = generableAnnotation.arguments
            .first { it.name?.asString() == "description" }
            .value as? String ?: ""

        val providerName = "${classDeclaration.simpleName.asString()}_Provider"
        val classTypeName = classDeclaration.toClassName()

        val providerFile = FileSpec.builder(classDeclaration.packageName.asString(), providerName)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S, %S", "unused", "UNCHECKED_CAST").build()
            )
            .addImport("kotlin.collections", "mapOf")
            .addImport("kotlinx.serialization", "serializer")
            .buildProviderObject(
                providerName = providerName,
                targetTypeName = classTypeName,
                schemaCode = buildSchemaCodeForClass(classDeclaration, description)
            )

        providerFile.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        generatedProviders.add("${classDeclaration.packageName.asString()}.$providerName")
    }

    // THIS IS THE DEFINITIVELY CORRECTED FUNCTION
    private fun buildSchemaCodeForClass(classDeclaration: KSClassDeclaration, description: String): String {
        val propertiesCode = classDeclaration.primaryConstructor?.parameters?.map { param ->
            val guideAnnotation = param.annotations.firstOrNull {
                it.shortName.asString() == Guide::class.simpleName
            }
            // FIX: Get the first argument by position, not by name.
            val propDesc = guideAnnotation?.arguments?.firstOrNull()?.value as? String

            val schemaTypeMethodName = when (val typeName = param.type.resolve().toClassName().canonicalName) {
                "kotlin.String" -> "string"
                "kotlin.Int" -> "integer"
                "kotlin.Long" -> "long"
                "kotlin.Float" -> "float"
                "kotlin.Double" -> "double"
                "kotlin.Boolean" -> "boolean"
                else -> {
                    logger.warn("Unsupported property type '$typeName'. Falling back to Schema.string().", param)
                    "string"
                }
            }

            val paramName = param.name!!.asString()

            if (propDesc != null) {
                CodeBlock.of("%S to %T.%N(%S)", paramName, schemaClassName, schemaTypeMethodName, propDesc)
            } else {
                CodeBlock.of("%S to %T.%N()", paramName, schemaClassName, schemaTypeMethodName)
            }
        }?.joinToString(separator = ",\n") ?: ""

        return CodeBlock.of(
            "%T.obj(\n  description = %S,\n  properties = mapOf(\n%L\n  )\n)",
            schemaClassName, description, propertiesCode
        ).toString()
    }

    private fun FileSpec.Builder.buildProviderObject(
        providerName: String,
        targetTypeName: TypeName,
        schemaCode: String,
    ): FileSpec {
        val providerClass = TypeSpec.classBuilder(providerName)
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(generableProviderClassName)
            .addSuperinterface(
                internalProviderClassName.parameterizedBy(targetTypeName)
            )
            .addProperty(
                PropertySpec.builder("targetClass", KClass::class.asTypeName().parameterizedBy(targetTypeName))
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T::class", targetTypeName)
                    .build()
            )
            .addFunction(
                FunSpec.builder("schema")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(schemaClassName)
                    .addStatement("return %L", schemaCode)
                    .build()
            )
            .addFunction(
                FunSpec.builder("serializer")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(kSerializerClassName.parameterizedBy(targetTypeName))
                    .addStatement("return serializer<%T>()", targetTypeName)
                    .build()
            )
            .build()

        this.addType(providerClass)
        return this.build()
    }

}
//package com.example.firebase_ai_processor
//
//import com.google.devtools.ksp.processing.*
//import com.google.devtools.ksp.symbol.*
//import com.google.devtools.ksp.validate
//import com.squareup.kotlinpoet.*
//import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
//import com.squareup.kotlinpoet.ksp.toClassName
//import com.example.annotations.Generable
//import com.example.annotations.Guide
//import com.squareup.kotlinpoet.ksp.writeTo
//import kotlin.reflect.KClass
//
//class GenerableProcessor(
//    private val codeGenerator: CodeGenerator,
//    private val logger: KSPLogger,
//) : SymbolProcessor {
//
//    private val schemaClassName = ClassName("com.google.firebase.ai.type", "Schema")
//    private val kSerializerClassName = ClassName("kotlinx.serialization", "KSerializer")
//    private val generableProviderClassName = ClassName("com.example.annotations", "GenerableProvider")
//    private val internalProviderClassName =
//        ClassName("com.example.firebase_ai", "SchemaAndSerializerProvider")
//
//    override fun process(resolver: Resolver): List<KSAnnotated> {
//        // Find symbols using the fully qualified name of the annotation.
//        val generableSymbols = resolver.getSymbolsWithAnnotation(Generable::class.qualifiedName!!)
//        val (validSymbols, invalidSymbols) = generableSymbols.partition { it.validate() }
//
//        validSymbols
//            .filterIsInstance<KSClassDeclaration>()
//            .forEach { classDeclaration ->
//                try {
//                    handleClass(classDeclaration)
//                } catch (e: Exception) {
//                    logger.error("Error processing symbol: ${e.message}", classDeclaration)
//                }
//            }
//
//        return invalidSymbols
//    }
//
//    private fun handleClass(classDeclaration: KSClassDeclaration) {
//        // Manually find the @Generable annotation on the class.
//        val generableAnnotation = classDeclaration.annotations.first {
//            it.shortName.asString() == Generable::class.simpleName
//        }
//        // Get the 'description' argument from the annotation.
//        val description = generableAnnotation.arguments
//            .first { it.name?.asString() == "description" }
//            .value as? String ?: ""
//
//        val providerName = "${classDeclaration.simpleName.asString()}_Provider"
//        val classTypeName = classDeclaration.toClassName()
//
//        val providerFile = FileSpec.builder(classDeclaration.packageName.asString(), providerName)
//            .addAnnotation(
//                AnnotationSpec.builder(Suppress::class)
//                    .addMember("%S, %S", "unused", "UNCHECKED_CAST").build()
//            )
//            .addImport("kotlin.collections", "mapOf")
//            .addImport("kotlinx.serialization", "serializer")
//            .buildProviderObject(
//                providerName = providerName,
//                targetTypeName = classTypeName,
//                schemaCode = buildSchemaCodeForClass(classDeclaration, description)
//            )
//
//        providerFile.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
//        registerService(providerName, classDeclaration.packageName.asString())
//    }
//    private fun buildSchemaCodeForClass(classDeclaration: KSClassDeclaration, description: String): String {
//        val propertiesCode = classDeclaration.primaryConstructor?.parameters?.map { param ->
//            // Find the @Guide annotation and get its description
//            val guideAnnotation = param.annotations.firstOrNull {
//                it.shortName.asString() == Guide::class.simpleName
//            }
//            val propDesc = guideAnnotation?.arguments
//                ?.firstOrNull { it.name?.asString() == "description" }
//                ?.value as? String
//
//            // Determine the schema method name (e.g., "long", "string")
//            val schemaTypeMethodName = when (val typeName = param.type.resolve().toClassName().canonicalName) {
//                "kotlin.String" -> "string"
//                "kotlin.Int" -> "integer"
//                "kotlin.Long" -> "long"
//                "kotlin.Float" -> "float"
//                "kotlin.Double" -> "double"
//                "kotlin.Boolean" -> "boolean"
//                else -> {
//                    logger.warn("Unsupported property type '$typeName'. Falling back to Schema.string().", param)
//                    "string"
//                }
//            }
//
//            val paramName = param.name!!.asString()
//
//            // This is the new, robust logic.
//            // We use %N for the method name, which is the correct KotlinPoet placeholder.
//            if (propDesc != null) {
//                CodeBlock.of("%S to %T.%N(%S)", paramName, schemaClassName, schemaTypeMethodName, propDesc)
//            } else {
//                CodeBlock.of("%S to %T.%N()", paramName, schemaClassName, schemaTypeMethodName)
//            }
//        }?.joinToString(separator = ",\n") ?: "" // Join the list of CodeBlocks into a single string
//
//        return CodeBlock.of(
//            "%T.obj(\n  description = %S,\n  properties = mapOf(\n%L\n  )\n)",
//            schemaClassName, description, propertiesCode
//        ).toString()
//    }
////    private fun buildSchemaCodeForClass(classDeclaration: KSClassDeclaration, description: String): String {
////        val propertiesCode = classDeclaration.primaryConstructor?.parameters?.joinToString(",\n") { param ->
////            // Find the @Guide annotation and get its description
////            val guideAnnotation = param.annotations.firstOrNull {
////                it.shortName.asString() == Guide::class.simpleName
////            }
////            val propDesc = guideAnnotation?.arguments
////                ?.firstOrNull { it.name?.asString() == "description" }
////                ?.value as? String
////
////            // Determine the schema method name (e.g., "long", "string")
////            val schemaTypeMethodName = when (val typeName = param.type.resolve().toClassName().canonicalName) {
////                "kotlin.String" -> "string"
////                "kotlin.Int" -> "integer"
////                "kotlin.Long" -> "long"
////                "kotlin.Float" -> "float"
////                "kotlin.Double" -> "double"
////                "kotlin.Boolean" -> "boolean"
////                else -> {
////                    logger.warn("Unsupported property type '$typeName'. Falling back to Schema.string().", param)
////                    "string"
////                }
////            }
////
////            val paramName = param.name!!.asString()
////
////            // This is the new, simplified, and correct logic
////            if (propDesc != null) {
////                // If a description exists, include it in the Schema call
////                CodeBlock.of("      %S to %T.$schemaTypeMethodName(%S)", paramName, schemaClassName, propDesc).toString()
////            } else {
////                // Otherwise, call the method without arguments
////                CodeBlock.of("      %S to %T.$schemaTypeMethodName()", paramName, schemaClassName).toString()
////            }
////        } ?: ""
////
////        return CodeBlock.of(
////            "%T.obj(\n  description = %S,\n  properties = mapOf(\n%L\n  )\n)",
////            schemaClassName, description, propertiesCode
////        ).toString()
////    }
////    private fun buildSchemaCodeForClass(classDeclaration: KSClassDeclaration, description: String): String {
////        val propertiesCode = classDeclaration.primaryConstructor?.parameters?.joinToString(",\n") { param ->
////            // Manually find the @Guide annotation on the property.
////            val guideAnnotation = param.annotations.firstOrNull {
////                it.shortName.asString() == Guide::class.simpleName
////            }
////            // Get the 'description' argument from the annotation, if it exists.
////            val propDesc = guideAnnotation?.arguments
////                ?.firstOrNull { it.name?.asString() == "description" }
////                ?.value as? String
////
////            val schemaType = when (val typeName = param.type.resolve().toClassName().canonicalName) {
////                "kotlin.String" -> "string"
////                "kotlin.Int" -> "integer"
////                "kotlin.Long" -> "long"
////                "kotlin.Float" -> "float"
////                "kotlin.Double" -> "double"
////                "kotlin.Boolean" -> "boolean"
////                else -> {
////                    logger.warn("Unsupported property type '$typeName'. Falling back to Schema.string().", param)
////                    "string"
////                }
////            }
////
////            if (propDesc != null) {
////                "      %S to %T.$schemaType(%S)"
////            } else {
////                "      %S to %T.$schemaType()"
////            }.let { format ->
////                val args = if (propDesc != null) {
////                    listOf(param.name?.asString(), schemaClassName, propDesc)
////                } else {
////                    listOf(param.name?.asString(), schemaClassName)
////                }
////                CodeBlock.of(format, *args.toTypedArray()).toString()
////            }
////        } ?: ""
////
////        return CodeBlock.of(
////            "%T.obj(\n  description = %S,\n  properties = mapOf(\n%L\n  )\n)",
////            schemaClassName, description, propertiesCode
////        ).toString()
////    }
//
//    private fun FileSpec.Builder.buildProviderObject(
//        providerName: String,
//        targetTypeName: TypeName,
//        schemaCode: String,
//    ): FileSpec {
//        val providerObject = TypeSpec.classBuilder(providerName)
//            .addModifiers(KModifier.INTERNAL)
//            .addSuperinterface(generableProviderClassName)
//            .addSuperinterface(
//                internalProviderClassName.parameterizedBy(targetTypeName)
//            )
//            .addProperty(
//                PropertySpec.builder("targetClass", KClass::class.asTypeName().parameterizedBy(targetTypeName))
//                    .addModifiers(KModifier.OVERRIDE)
//                    .initializer("%T::class", targetTypeName)
//                    .build()
//            )
//            .addFunction(
//                FunSpec.builder("schema")
//                    .addModifiers(KModifier.OVERRIDE)
//                    .returns(schemaClassName)
//                    .addStatement("return %L", schemaCode)
//                    .build()
//            )
//            .addFunction(
//                FunSpec.builder("serializer")
//                    .addModifiers(KModifier.OVERRIDE)
//                    .returns(kSerializerClassName.parameterizedBy(targetTypeName))
//                    .addStatement("return serializer<%T>()", targetTypeName)
//                    .build()
//            )
//            .build()
//
//        this.addType(providerObject)
//        return this.build()
//    }
//
//    private fun registerService(providerName: String, packageName: String) {
//        val resourceFile = "META-INF/services/${generableProviderClassName.canonicalName}"
//        val serviceContent = "$packageName.$providerName\n"
//        codeGenerator.createNewFile(
//            dependencies = Dependencies(false),
//            packageName = "",
//            fileName = resourceFile,
//            extensionName = ""
//        ).use { it.write(serviceContent.toByteArray()) }
//    }
//}