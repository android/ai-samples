package com.android.ai.common

sealed class Model(val id: String, val name: String) {
    data object GeminiFlash : Model("gemini-2.5-flash", "Gemini 2.5 Flash")
    data object GeminiFlashImage : Model("gemini-2.5-flash-image", "Gemini 2.5 Flash Image")
    data object GeminiFlashLive : Model("gemini-2.0-flash-live-preview-04-09", "Gemini 2.0 Flash Live")
    data object Imagen : Model("imagen-4.0-generate-001", "Imagen 4.0")
    data object ImagenEditing : Model("imagen-3.0-capability-001", "Imagen 3.0 Editing")
}
