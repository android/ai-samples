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
 * Act 1 Interactive Flight Option Selector: Displays the itinerary-aware AI reasoning banner and
 * interactive flight departure cards. Uses two-way data bindings (`bindUpdater`) so selecting a
 * flight option immediately updates `/booking/selectedFlightId`, `/booking/selectedFlightTime`, and
 * `/booking/baseFare` in the surface's reactive `A2uiDataModel`.
 */
class FlightOptionSelectorComponent : A2uiComponent {
  private val aiReasonProp =
    A2uiProperty.dynamicString(
      key = "aiReason",
      required = false,
      description = "Itinerary-aware explanation generated by Gemini.",
    )
  private val optionsProp =
    A2uiProperty.dynamicStringList(
      key = "options",
      required = true,
      description = "Pipe-delimited options: flightNum|timeRange|aircraftInfo|price|badge",
    )
  private val selectedFlightIdProp =
    A2uiProperty.dynamicString(
      key = "selectedFlightId",
      required = false,
      description = "Two-way bound selected flight number.",
    )
  private val selectedFlightTimeProp =
    A2uiProperty.dynamicString(
      key = "selectedFlightTime",
      required = false,
      description = "Two-way bound selected flight time range.",
    )
  private val baseFareProp =
    A2uiProperty.dynamicNumber(
      key = "baseFare",
      required = false,
      description = "Two-way bound base fare in USD.",
    )
  private val confirmBtnTextProp =
    A2uiProperty.dynamicString(
      key = "confirmBtnText",
      required = false,
      description = "Button label to lock in departure time.",
    )
  private val actionProp =
    A2uiProperty.action(
      key = "action",
      required = false,
      description = "Action dispatched when confirming flight departure.",
    )

  override val name: String = "FlightOptionSelector"
  override val description: String =
    "Selects a flight departure option with AI itinerary matching and two-way fare binding."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      aiReasonProp,
      optionsProp,
      selectedFlightIdProp,
      selectedFlightTimeProp,
      baseFareProp,
      confirmBtnTextProp,
      actionProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(optionsProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val aiReason = properties.bind(aiReasonProp)
    val rawOptions = properties.bind(optionsProp) ?: emptyList()
    val boundFlightId = properties.bind(selectedFlightIdProp)
    val boundFlightTime = properties.bind(selectedFlightTimeProp)
    val updateFlightId = properties.bindUpdater(selectedFlightIdProp)
    val updateFlightTime = properties.bindUpdater(selectedFlightTimeProp)
    val updateBaseFare = properties.bindUpdater(baseFareProp)
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Continue to Cabin & Seats"
    val action = properties[actionProp]

    val parsedOptions =
      remember(rawOptions) { rawOptions.mapIndexed { index, raw -> parseFlightOption(raw, index) } }

    var localSelectedId by
      rememberSaveable(boundFlightId) {
        mutableStateOf(boundFlightId ?: parsedOptions.firstOrNull()?.flightId.orEmpty())
      }
    val activeSelectedId = boundFlightId ?: localSelectedId

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      if (!aiReason.isNullOrEmpty()) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(JetTangeloSoft)
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = JetTangelo,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = aiReason,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = JetDeepNavy,
          )
        }
      }

      Text(
        text = "CHOOSE DEPARTURE FLIGHT",
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = SekuyaFontFamily,
            fontSize = 13.sp,
            letterSpacing = 0.7.sp,
          ),
        color = JetDeepNavy,
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parsedOptions.forEach { item ->
          val isSelected =
            item.flightId.equals(activeSelectedId, ignoreCase = true) ||
              item.timeRange.equals(boundFlightTime, ignoreCase = true)
          val borderColor by
            animateColorAsState(
              targetValue = if (isSelected) JetRoyalCobalt else Color(0xFFE9ECEF),
              label = "FlightCardBorder",
            )
          val containerColor by
            animateColorAsState(
              targetValue = if (isSelected) JetCobaltSoft else Color.White,
              label = "FlightCardBg",
            )

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .border(
                  BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                  RoundedCornerShape(16.dp),
                )
                .clickable {
                  localSelectedId = item.flightId
                  updateFlightId?.invoke(item.flightId)
                  updateFlightTime?.invoke(item.timeRange)
                  updateBaseFare?.invoke(item.price)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(
              modifier = Modifier.weight(1f).padding(end = 10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (item.badge.isNotEmpty()) {
                val isFeaturedBadge =
                  item.badge.contains("TOP PICK", ignoreCase = true) ||
                    item.badge.contains("RECOMMENDED", ignoreCase = true) ||
                    item.badge.contains("BEST", ignoreCase = true)
                Box(
                  modifier =
                    Modifier.background(
                        if (isFeaturedBadge) JetCoralOrange else JetSkyCyan,
                        RoundedCornerShape(50),
                      )
                      .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = item.badge,
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                      ),
                    color = if (isFeaturedBadge) Color.White else JetDeepNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Text(
                text = item.timeRange,
                style =
                  MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp,
                  ),
                color = JetDeepNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${item.flightId} • ${item.aircraftInfo}",
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                  ),
                color = JetMutedSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "$${item.price}",
                style =
                  MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SekuyaFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                  ),
                color = if (isSelected) JetRoyalCobalt else JetDeepNavy,
                maxLines = 1,
              )
              Text(
                text = if (isSelected) "Selected" else "Roundtrip",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                  ),
                color = if (isSelected) JetRoyalCobalt else JetMutedSlate,
                maxLines = 1,
              )
            }
          }
        }
      }

      Button(
        onClick = {
          val chosen =
            parsedOptions.find { it.flightId.equals(activeSelectedId, ignoreCase = true) }
              ?: parsedOptions.firstOrNull()
          val selectedLabel =
            if (chosen != null) "${chosen.flightId} (${chosen.timeRange})" else activeSelectedId
          val context =
            mutableMapOf<String, Any>(
              "type" to "confirm_option",
              "selectionType" to "time",
              "selectedOption" to selectedLabel,
            )
          if (chosen != null) {
            context["selectedFlightId"] = chosen.flightId
            context["selectedFlightTime"] = chosen.timeRange
            context["baseFare"] = chosen.price
          }
          dispatchMergedAction(action, "confirm_option", context)
        },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(containerColor = JetDeepNavy, contentColor = Color.White),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = confirmBtnText,
            style =
              MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
              ),
          )
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = JetSkyCyan,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

private data class ParsedFlightOption(
  val flightId: String,
  val timeRange: String,
  val aircraftInfo: String,
  val price: Int,
  val badge: String,
)

private fun parseFlightOption(raw: String, index: Int): ParsedFlightOption {
  val parts = raw.split("|").map { it.trim() }
  if (parts.size >= 4) {
    val rawBadge =
      parts
        .getOrNull(4)
        .orEmpty()
        .replace(Regex("(?i)\\bAI\\s+PICK\\b"), "TOP PICK")
        .replace(Regex("(?i)\\bAI\\s+RECOMMENDED\\b"), "TOP PICK")
        .replace(Regex("(?i)\\bAI\\s+"), "")
    return ParsedFlightOption(
      flightId = parts[0],
      timeRange = parts[1],
      aircraftInfo = parts[2],
      price = parts[3].filter { it.isDigit() }.toIntOrNull() ?: 640,
      badge = rawBadge,
    )
  }
  return ParsedFlightOption(
    flightId = "FL ${101 + index}",
    timeRange = raw,
    aircraftInfo = "Widebody Nonstop",
    price = if (index == 0) 640 else 590,
    badge = if (index == 0) "TOP PICK" else "",
  )
}

