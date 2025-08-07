/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ai.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary50,
    secondary = Secondary40,
    tertiary = Tertiary80,
    error = Error80,

    onPrimary = Primary10,
    onSecondary = Secondary5,
    onTertiary = Tertiary30,
    onError = Error20,

    primaryContainer = Primary20,
    secondaryContainer = Secondary20,
    tertiaryContainer = Tertiary35,
    errorContainer = Error40,

    onPrimaryContainer = Primary50,
    onSecondaryContainer = Secondary50,
    onTertiaryContainer = Tertiary90,
    onErrorContainer = Error90,

    surfaceContainerHigh = SurfaceContainerHigh,
    onSurface = Neutral98,
    onSurfaceVariant = NeutralVariant,
    outline = Neutral50,
)

@Composable
fun AISampleCatalogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
