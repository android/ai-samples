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
package com.android.ai.catalog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.ai.catalog.R

val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    headlineSmall =  TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(500),
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(500),
        letterSpacing = 0.1.sp,
    ),
    labelLarge =  TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(700),
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(700),
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(700),
        letterSpacing = 0.5.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFamily = FontFamily(Font(R.font.space_grotesk)),
        fontWeight = FontWeight(400),
        letterSpacing = 0.4.sp,
    )
)
