/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetpacker.feature.booking_assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentProperties

/**
 * High-level Interactive Option Picker component.
 * Displays a list of custom options as outlined rounded buttons and a confirm button at the bottom.
 */
class InteractiveOptionPickerComponent : A2uiComponent {
    companion object {
        val promptProp = A2uiProperty.string("prompt")
        val optionsProp = A2uiProperty.stringList("options")
        val selectedIdxProp = A2uiProperty.number("selectedIdx")
        val confirmBtnTextProp = A2uiProperty.string("confirmBtnText")
    }

    override val name: String = "InteractiveOptionPicker"
    override val description: String = "Renders custom options and confirm action"
    override val properties: List<A2uiProperty<*>> = listOf(
        promptProp,
        optionsProp,
        selectedIdxProp,
        confirmBtnTextProp
    )

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier
    ) {
        val prompt = properties[promptProp] ?: ""
        val options = properties[optionsProp] ?: emptyList()
        val selectedIdx = properties[selectedIdxProp]?.toInt()
        val confirmBtnText = properties[confirmBtnTextProp]?.let { 
            if (it.isEmpty()) "Confirm Selection" else it 
        } ?: "Confirm Selection"

        Column(modifier = modifier.fillMaxWidth()) {
            if (prompt.isNotEmpty()) {
                Text(
                    text = prompt,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            options.forEachIndexed { idx, option ->
                Button(
                    onClick = {
                        dispatchAction(mapOf("event" to mapOf("name" to "SelectOption_$idx")))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedIdx == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedIdx == idx) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedIdx == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (selectedIdx != null) {
                        dispatchAction(mapOf("event" to mapOf("name" to "ConfirmSelection_$selectedIdx")))
                    }
                },
                enabled = selectedIdx != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = confirmBtnText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * High-level Seat Selection grid component.
 */
class SeatSelectionPickerComponent : A2uiComponent {
    companion object {
        val promptProp = A2uiProperty.string("prompt")
        val seatsProp = A2uiProperty.stringList("seats")
        val selectedSeatProp = A2uiProperty.string("selectedSeat")
    }

    override val name: String = "SeatSelectionPicker"
    override val description: String = "Renders seat choices as a clean selection grid"
    override val properties: List<A2uiProperty<*>> = listOf(
        promptProp,
        seatsProp,
        selectedSeatProp
    )

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier
    ) {
        val prompt = properties[promptProp] ?: ""
        val seats = properties[seatsProp] ?: emptyList()
        val selectedSeat = properties[selectedSeatProp]?.let {
            if (it.isEmpty()) null else it
        }

        Column(modifier = modifier.fillMaxWidth()) {
            if (prompt.isNotEmpty()) {
                Text(
                    text = prompt,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                seats.chunked(2).forEach { rowSeats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSeats.forEach { seat ->
                            Button(
                                onClick = {
                                    dispatchAction(mapOf("event" to mapOf("name" to seat)))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSeat == seat) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (selectedSeat == seat) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selectedSeat == seat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = seat,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-level Booking Status component.
 */
class BookingStatusComponent : A2uiComponent {
    companion object {
        val textProp = A2uiProperty.string("text")
    }

    override val name: String = "BookingStatus"
    override val description: String = "Renders final booking success confirmation"
    override val properties: List<A2uiProperty<*>> = listOf(textProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier
    ) {
        val text = properties[textProp] ?: ""

        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Builds the Custom Catalog for Booking Assistant.
 */
fun bookingAssistantCatalog(): A2uiCatalog {
    return A2uiCatalog(
        catalogId = "https://example.com/catalogs/booking_assistant/v1/catalog.json",
        components = listOf(
            InteractiveOptionPickerComponent(),
            SeatSelectionPickerComponent(),
            BookingStatusComponent()
        ),
        functions = emptyList(),
        themeSchema = null
    )
}
