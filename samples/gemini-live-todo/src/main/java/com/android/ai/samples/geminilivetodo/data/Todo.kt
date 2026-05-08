/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ai.samples.geminilivetodo.data

import java.util.UUID.randomUUID

const val MIC_TODO_ID = 111

sealed interface GlassesListItem {
    val id: Int
}

data class Todo(
    override val id: Int = randomUUID().hashCode(),
    val task: String,
    val isCompleted: Boolean = false,
) : GlassesListItem

data class MicControl(
    override val id: Int = MIC_TODO_ID,
    val statusText: String,
    val isMicOn: Boolean,
) : GlassesListItem