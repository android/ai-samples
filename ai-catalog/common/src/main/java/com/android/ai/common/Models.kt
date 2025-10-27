package com.android.ai.common

sealed class Model(val id: String, val name: String) {
    data object GeminiFlash : Model("gemini-2.5-flash", "Gemini 2.5 Flash")
    data object GeminiFlashImage : Model("gemini-2.5-flash-image-preview", "Gemini 2.5 Flash Image")
    data object GeminiFlashLive : Model("gemini-2.0-flash-live-preview-04-09", "Gemini 2.0 Flash Live")
}
