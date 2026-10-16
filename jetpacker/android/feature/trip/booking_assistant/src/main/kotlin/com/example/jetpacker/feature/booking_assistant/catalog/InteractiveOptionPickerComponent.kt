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


/**
 * An interactive option picker that allows selecting a single option from a list and confirming
 * (used for Dining party size, Activities, or fallback flight selection).
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
    listOf(
      titleProp,
      categoryProp,
      statusProp,
      promptProp,
      optionsProp,
      selectedOptionProp,
      confirmBtnTextProp,
      actionProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(promptProp) != null && properties.bind(optionsProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val title = properties.bind(titleProp)
    val category = properties.bind(categoryProp) ?: "Dining"
    val status = properties.bind(statusProp) ?: "ACTION REQUIRED"
    val prompt = properties.bind(promptProp) ?: "Select an option:"
    val options = properties.bind(optionsProp) ?: emptyList()
    val boundSelected = properties.bind(selectedOptionProp)
    val updateSelected = properties.bindUpdater(selectedOptionProp)
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Confirm Selection"
    val action = properties[actionProp]

    var selected by
      rememberSaveable(boundSelected) { mutableStateOf(boundSelected ?: options.firstOrNull()) }

    val categoryIcon =
      when {
        category.contains("Hotel", ignoreCase = true) -> Icons.Rounded.Hotel
        category.contains("Activity", ignoreCase = true) -> Icons.Rounded.ConfirmationNumber
        category.contains("Dining", ignoreCase = true) -> Icons.Rounded.Restaurant
        else -> Icons.Rounded.Flight
      }

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(JetWarmCream.copy(alpha = 0.65f))
          .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (!title.isNullOrEmpty()) {
          Text(
            text = title,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
              ),
            color = JetDeepNavy,
            modifier = Modifier.weight(1f),
          )
        }
        Box(
          modifier =
            Modifier.background(JetTangeloSoft, RoundedCornerShape(50))
              .border(BorderStroke(1.dp, JetTangelo.copy(alpha = 0.35f)), RoundedCornerShape(50))
              .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = status.uppercase(),
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp,
              ),
            color = JetTangelo,
          )
        }
      }

      Text(
        text = prompt,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        color = JetSlate,
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
          val isSelected = option == selected
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) JetCobaltSoft else Color.White)
                .border(
                  BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) JetRoyalCobalt else Color(0xFFD1D5DB),
                  ),
                  RoundedCornerShape(16.dp),
                )
                .clickable {
                  selected = option
                  updateSelected?.invoke(option)
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = null,
              tint = if (isSelected) JetRoyalCobalt else JetSlate,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = option,
              style =
                MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 15.sp,
                ),
              color = JetDeepNavy,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      Button(
        onClick = {
          val context = mutableMapOf<String, Any>("type" to "confirm_option")
          selected?.let { context["selectedOption"] = it }
          dispatchMergedAction(action, "confirm_option", context)
        },
        enabled = selected != null,
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

