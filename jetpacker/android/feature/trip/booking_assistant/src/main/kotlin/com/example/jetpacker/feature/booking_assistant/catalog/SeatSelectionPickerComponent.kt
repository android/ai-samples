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

package com.example.jetpacker.feature.booking_assistant.catalog

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AirlineSeatReclineExtra
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jetpacker.core.ui.R as CoreUiR
import com.example.jetpacker.core.ui.SekuyaFontFamily
import kotlin.math.atan2


/** A seat selection picker for flights or travel segments. */
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
    listOf(
      titleProp,
      categoryProp,
      statusProp,
      promptProp,
      optionsProp,
      selectedSeatProp,
      confirmBtnTextProp,
      actionProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(optionsProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val title = properties.bind(titleProp)
    val status = properties.bind(statusProp) ?: "ACTION REQUIRED"
    val prompt = properties.bind(promptProp) ?: "Seat Selection"
    val options = properties.bind(optionsProp) ?: listOf("1A", "1B", "2A", "2B")
    val initialSeat = properties.bind(selectedSeatProp)
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Confirm Selection"
    val action = properties[actionProp]

    var selectedSeat by rememberSaveable(initialSeat) { mutableStateOf(initialSeat) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (!title.isNullOrEmpty()) {
        Text(
          text = title,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
            ),
          color = JetDeepNavy,
        )
      }

      Box(
        modifier =
          Modifier.background(JetTangeloSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = status.uppercase(),
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 11.sp,
            ),
          color = JetTangelo,
        )
      }

      Text(
        text = prompt,
        style =
          MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        color = JetDeepNavy,
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
                      containerColor = JetDeepNavy,
                      contentColor = Color.White,
                    )
                  } else {
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = Color.White,
                      contentColor = JetDeepNavy,
                    )
                  },
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFD1D5DB)),
              ) {
                Text(
                  text = seat,
                  style =
                    MaterialTheme.typography.bodyMedium.copy(
                      fontFamily = SekuyaFontFamily,
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
          val context = mutableMapOf<String, Any>("type" to "confirm_seat")
          selectedSeat?.let {
            context["selectedSeat"] = it
            context["seat"] = it
          }
          dispatchMergedAction(action, "confirm_seat", context)
        },
        enabled = selectedSeat != null,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(containerColor = JetDeepNavy, contentColor = Color.White),
      ) {
        Text(
          text = confirmBtnText,
          style =
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        )
      }
    }
  }
}

