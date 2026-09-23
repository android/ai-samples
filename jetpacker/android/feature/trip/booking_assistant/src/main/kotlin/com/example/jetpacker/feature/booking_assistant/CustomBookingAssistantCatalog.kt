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

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val BOOKING_ASSISTANT_CATALOG_ID: String =
  "https://example.com/catalogs/booking_assistant/v1/catalog.json"

/**
 * Returns the custom A2UI catalog for Jetpacker's Booking Assistant.
 */
fun bookingAssistantCatalog(): A2uiCatalog {
  return A2uiCatalog(
    catalogId = BOOKING_ASSISTANT_CATALOG_ID,
    components = listOf(
      InteractiveOptionPickerComponent(),
      SeatSelectionPickerComponent(),
      BookingStatusComponent(),
      BookingConfirmationComponent(),
    ),
  )
}

/**
 * An interactive option picker that allows selecting a single option from a list and confirming.
 */
class InteractiveOptionPickerComponent : A2uiComponent {
  private val titleProp =
    A2uiProperty.dynamicString(
      key = "title",
      required = false,
      description = "The title of the booking item.",
    )

  private val categoryProp =
    A2uiProperty.dynamicString(
      key = "category",
      required = false,
      description = "The booking category.",
    )

  private val statusProp =
    A2uiProperty.dynamicString(
      key = "status",
      required = false,
      description = "Status chip label (e.g. CONFIRMATION REQUIRED).",
    )

  private val promptProp =
    A2uiProperty.dynamicString(
      key = "prompt",
      required = true,
      description = "The prompt or instruction to display to the user.",
    )

  private val optionsProp =
    A2uiProperty.dynamicStringList(
      key = "options",
      required = true,
      description = "The list of selectable options.",
    )

  private val selectedOptionProp =
    A2uiProperty.dynamicString(
      key = "selectedOption",
      required = false,
      description = "The currently selected option string.",
    )

  private val confirmBtnTextProp =
    A2uiProperty.dynamicString(
      key = "confirmBtnText",
      required = false,
      description = "The label for the confirmation button.",
    )

  private val actionProp =
    A2uiProperty.action(
      key = "action",
      required = false,
      description = "Custom action payload dispatched when the confirm button is clicked.",
    )

  override val name: String = "InteractiveOptionPicker"
  override val description: String =
    "An interactive option picker allowing users to select an option and confirm."
  override val properties: List<A2uiProperty<*>> =
    listOf(titleProp, categoryProp, statusProp, promptProp, optionsProp, selectedOptionProp, confirmBtnTextProp, actionProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(promptProp) != null && properties.bind(optionsProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(
    properties: A2uiComponentProperties,
    modifier: Modifier,
  ) {
    val title = properties.bind(titleProp)
    val category = properties.bind(categoryProp) ?: "Flight"
    val status = properties.bind(statusProp) ?: "CONFIRMATION REQUIRED"
    val prompt = properties.bind(promptProp) ?: "Flight Departure"
    val options = properties.bind(optionsProp) ?: emptyList()
    val initialSelected = properties.bind(selectedOptionProp)
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Confirm Selection"
    val action = properties[actionProp]

    var selected by rememberSaveable(initialSelected) {
      mutableStateOf(initialSelected ?: options.firstOrNull())
    }

    val categoryIcon = when {
      category.contains("Hotel", ignoreCase = true) -> Icons.Rounded.Hotel
      category.contains("Activity", ignoreCase = true) -> Icons.Rounded.ConfirmationNumber
      category.contains("Dining", ignoreCase = true) -> Icons.Rounded.Restaurant
      else -> Icons.Rounded.Flight
    }

    Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (!title.isNullOrEmpty()) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
          ),
          color = Color(0xFF1E293B),
        )
      }

      Box(
        modifier = Modifier
          .background(Color(0xFFFDE8E8), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Text(
          text = status.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            fontSize = 11.sp,
          ),
          color = Color(0xFF8A1F11),
        )
      }

      Text(
        text = prompt,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
        ),
        color = Color(0xFF1E293B),
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
          val isSelected = option == selected
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(
                  BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) Color(0xFF94A3B8) else Color(0xFFD1D5DB),
                  ),
                  RoundedCornerShape(16.dp),
                )
                .clickable { selected = option }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = null,
              tint = Color(0xFF374151),
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = option,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 15.sp,
              ),
              color = Color(0xFF1F2937),
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          val context = mutableMapOf<String, Any>(
            "type" to "confirm_option",
          )
          if (selected != null) {
            context["selectedOption"] = selected!!
          }
          val eventPayload = if (action != null && action.containsKey("event")) {
            val originalEvent = (action["event"] as? Map<*, *>) ?: emptyMap<String, Any>()
            val originalContext = (originalEvent["context"] as? Map<*, *>) ?: emptyMap<String, Any>()
            val mergedContext = originalContext.toMutableMap()
            mergedContext.putAll(context)
            mapOf(
              "event" to mapOf(
                "name" to (originalEvent["name"] as? String ?: "confirm_option"),
                "context" to mergedContext,
              )
            )
          } else {
            mapOf(
              "event" to mapOf(
                "name" to "confirm_option",
                "context" to context,
              )
            )
          }
          dispatchAction(eventPayload)
        },
        enabled = selected != null,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFE5E2DA),
          contentColor = Color(0xFF78716C),
          disabledContainerColor = Color(0xFFE5E2DA).copy(alpha = 0.5f),
          disabledContentColor = Color(0xFF78716C).copy(alpha = 0.5f),
        ),
      ) {
        Text(
          text = confirmBtnText,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
        )
      }
    }
  }
}

/**
 * A seat selection picker for flights or travel segments.
 */
class SeatSelectionPickerComponent : A2uiComponent {
  private val titleProp =
    A2uiProperty.dynamicString(
      key = "title",
      required = false,
      description = "The title of the booking item.",
    )

  private val categoryProp =
    A2uiProperty.dynamicString(
      key = "category",
      required = false,
      description = "Booking category.",
    )

  private val statusProp =
    A2uiProperty.dynamicString(
      key = "status",
      required = false,
      description = "Status chip label (e.g. ACTION REQUIRED).",
    )

  private val promptProp =
    A2uiProperty.dynamicString(
      key = "prompt",
      required = false,
      description = "The prompt for seat selection.",
    )

  private val optionsProp =
    A2uiProperty.dynamicStringList(
      key = "options",
      required = true,
      description = "Available seats list.",
    )

  private val selectedSeatProp =
    A2uiProperty.dynamicString(
      key = "selectedSeat",
      required = false,
      description = "Currently selected seat.",
    )

  private val confirmBtnTextProp =
    A2uiProperty.dynamicString(
      key = "confirmBtnText",
      required = false,
      description = "Button label for confirmation.",
    )

  private val actionProp =
    A2uiProperty.action(
      key = "action",
      required = false,
      description = "Action payload dispatched when confirming seat selection.",
    )

  override val name: String = "SeatSelectionPicker"
  override val description: String =
    "A seat selection picker displaying seat options and a confirmation button."
  override val properties: List<A2uiProperty<*>> =
    listOf(titleProp, categoryProp, statusProp, promptProp, optionsProp, selectedSeatProp, confirmBtnTextProp, actionProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(optionsProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(
    properties: A2uiComponentProperties,
    modifier: Modifier,
  ) {
    val title = properties.bind(titleProp)
    val status = properties.bind(statusProp) ?: "ACTION REQUIRED"
    val prompt = properties.bind(promptProp) ?: "Seat Selection"
    val options = properties.bind(optionsProp) ?: listOf("1A", "1B", "1C", "2A", "2B", "2C")
    val initialSeat = properties.bind(selectedSeatProp)
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Confirm Selection"
    val action = properties[actionProp]

    var selectedSeat by rememberSaveable(initialSeat) { mutableStateOf(initialSeat) }

    Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (!title.isNullOrEmpty()) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
          ),
          color = Color(0xFF1E293B),
        )
      }

      Box(
        modifier = Modifier
          .background(Color(0xFFFDE8E8), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Text(
          text = status.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            fontSize = 11.sp,
          ),
          color = Color(0xFF8A1F11),
        )
      }

      Text(
        text = prompt,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
        ),
        color = Color(0xFF1E293B),
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { rowSeats ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            rowSeats.forEach { seat ->
              val isSelected = seat == selectedSeat
              OutlinedButton(
                onClick = { selectedSeat = seat },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors =
                  if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = Color(0xFF1E293B),
                      contentColor = Color.White,
                    )
                  } else {
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = Color.White,
                      contentColor = Color(0xFF1E293B),
                    )
                  },
                border =
                  if (isSelected) null
                  else BorderStroke(1.dp, Color(0xFFD1D5DB)),
              ) {
                Text(
                  text = seat,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  ),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          val context = mutableMapOf<String, Any>(
            "type" to "confirm_seat",
          )
          if (selectedSeat != null) {
            context["selectedSeat"] = selectedSeat!!
          }
          val eventPayload = if (action != null && action.containsKey("event")) {
            val originalEvent = (action["event"] as? Map<*, *>) ?: emptyMap<String, Any>()
            val originalContext = (originalEvent["context"] as? Map<*, *>) ?: emptyMap<String, Any>()
            val mergedContext = originalContext.toMutableMap()
            mergedContext.putAll(context)
            mapOf(
              "event" to mapOf(
                "name" to (originalEvent["name"] as? String ?: "confirm_seat"),
                "context" to mergedContext,
              )
            )
          } else {
            mapOf(
              "event" to mapOf(
                "name" to "confirm_seat",
                "context" to context,
              )
            )
          }
          dispatchAction(eventPayload)
        },
        enabled = selectedSeat != null,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFE5E2DA),
          contentColor = Color(0xFF78716C),
          disabledContainerColor = Color(0xFFE5E2DA).copy(alpha = 0.5f),
          disabledContentColor = Color(0xFF78716C).copy(alpha = 0.5f),
        ),
      ) {
        Text(
          text = confirmBtnText,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
        )
      }
    }
  }
}

/**
 * Displays booking confirmation status, title, details, and status badge.
 */
class BookingStatusComponent : A2uiComponent {
  private val categoryProp =
    A2uiProperty.dynamicString(
      key = "category",
      required = false,
      description = "Booking category: Flight, Hotel, Activity, Dining.",
    )

  private val titleProp =
    A2uiProperty.dynamicString(
      key = "title",
      required = true,
      description = "Title of the booking item.",
    )

  private val descriptionProp =
    A2uiProperty.dynamicString(
      key = "description",
      required = false,
      description = "Booking description or update details.",
    )

  private val statusProp =
    A2uiProperty.dynamicString(
      key = "status",
      required = true,
      description = "Status string: Confirmed, Action Required, Processing, Complete, Queued.",
    )

  override val name: String = "BookingStatus"
  override val description: String =
    "Displays the booking status with icon, badge, title, and descriptive message."
  override val properties: List<A2uiProperty<*>> =
    listOf(categoryProp, titleProp, descriptionProp, statusProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(titleProp) != null && properties.bind(statusProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(
    properties: A2uiComponentProperties,
    modifier: Modifier,
  ) {
    val category = properties.bind(categoryProp) ?: "Booking"
    val title = properties.bind(titleProp) ?: "Travel Booking"
    val description = properties.bind(descriptionProp)
    val status = properties.bind(statusProp) ?: "Processing"

    val (bg, txt) =
      when (status.uppercase()) {
        "COMPLETE",
        "CONFIRMED" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "CONFIRMATION REQUIRED",
        "ACTION REQUIRED" -> Color(0xFFFDE8E8) to Color(0xFF8A1F11)
        "RUNNING",
        "PROCESSING...",
        "PROCESSING" -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
      }

    Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
        ),
        color = Color(0xFF1E293B),
      )

      Box(
        modifier =
          Modifier.background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = status.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            fontSize = 11.sp,
          ),
          color = txt,
        )
      }

      if (!description.isNullOrEmpty()) {
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
          ),
          color = Color(0xFF475569),
        )
      }
    }
  }
}

/**
 * A booking confirmation card that allows confirming a reservation or ticket.
 */
class BookingConfirmationComponent : A2uiComponent {
  private val categoryProp =
    A2uiProperty.dynamicString(
      key = "category",
      required = false,
      description = "Booking category: Flight, Hotel, Activity, Dining.",
    )

  private val titleProp =
    A2uiProperty.dynamicString(
      key = "title",
      required = true,
      description = "Title of the booking item.",
    )

  private val statusProp =
    A2uiProperty.dynamicString(
      key = "status",
      required = false,
      description = "Status badge text.",
    )

  private val promptProp =
    A2uiProperty.dynamicString(
      key = "prompt",
      required = true,
      description = "Justification or details explaining the booking selection.",
    )

  private val confirmBtnTextProp =
    A2uiProperty.dynamicString(
      key = "confirmBtnText",
      required = false,
      description = "Label for the confirmation button.",
    )

  private val actionProp =
    A2uiProperty.action(
      key = "action",
      required = false,
      description = "Action payload dispatched when confirmed.",
    )

  override val name: String = "BookingConfirmation"
  override val description: String =
    "A confirmation card with justification details and a confirm button."
  override val properties: List<A2uiProperty<*>> =
    listOf(categoryProp, titleProp, statusProp, promptProp, confirmBtnTextProp, actionProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(titleProp) != null && properties.bind(promptProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(
    properties: A2uiComponentProperties,
    modifier: Modifier,
  ) {
    val category = properties.bind(categoryProp) ?: "Booking"
    val title = properties.bind(titleProp) ?: "Reservation"
    val status = properties.bind(statusProp) ?: "CONFIRMATION REQUIRED"
    val prompt = properties.bind(promptProp) ?: "Please confirm reservation."
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Confirm Reservation"
    val action = properties[actionProp]

    Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
        ),
        color = Color(0xFF1E293B),
      )

      Box(
        modifier =
          Modifier.background(Color(0xFFFDE8E8), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = status.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            fontSize = 11.sp,
          ),
          color = Color(0xFF8A1F11),
        )
      }

      Text(
        text = prompt,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = 14.sp,
          lineHeight = 20.sp,
        ),
        color = Color(0xFF334155),
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          val context = mutableMapOf<String, Any>(
            "type" to "confirm_booking",
          )
          val eventPayload = if (action != null && action.containsKey("event")) {
            val originalEvent = (action["event"] as? Map<*, *>) ?: emptyMap<String, Any>()
            val originalContext = (originalEvent["context"] as? Map<*, *>) ?: emptyMap<String, Any>()
            val mergedContext = originalContext.toMutableMap()
            mergedContext.putAll(context)
            mapOf(
              "event" to mapOf(
                "name" to (originalEvent["name"] as? String ?: "confirm_booking"),
                "context" to mergedContext,
              )
            )
          } else {
            mapOf(
              "event" to mapOf(
                "name" to "confirm_booking",
                "context" to context,
              )
            )
          }
          dispatchAction(eventPayload)
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFE5E2DA),
          contentColor = Color(0xFF78716C),
        ),
      ) {
        Text(
          text = confirmBtnText,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
        )
      }
    }
  }
}

