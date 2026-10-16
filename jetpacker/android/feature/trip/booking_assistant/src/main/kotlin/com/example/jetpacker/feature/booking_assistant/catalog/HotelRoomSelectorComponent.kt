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
 * Step 1 of Boutique Hotel Flow (`HotelRoomSelector`): Renders 3 selectable room/suite tiers with
 * amenity highlights, nightly rate, and AI match reason. Format of each string in `options`:
 * `"roomId|roomName|amenities|nightlyRate|badge"`
 */
class HotelRoomSelectorComponent : A2uiComponent {
  private val aiReasonProp =
    A2uiProperty.dynamicString(
      key = "aiReason",
      required = false,
      description = "AI concierge rationale for selecting this property.",
    )
  private val optionsProp =
    A2uiProperty.dynamicStringList(
      key = "options",
      required = true,
      description = "Pipe-delimited suite options: roomId|roomName|amenities|nightlyRate|badge",
    )
  private val selectedRoomIdProp =
    A2uiProperty.dynamicString(
      key = "selectedRoomId",
      required = false,
      description = "Two-way bound selected room ID.",
    )
  private val selectedRoomNameProp =
    A2uiProperty.dynamicString(
      key = "selectedRoomName",
      required = false,
      description = "Two-way bound selected room name.",
    )
  private val nightlyRateProp =
    A2uiProperty.dynamicNumber(
      key = "nightlyRate",
      required = false,
      description = "Two-way bound nightly rate.",
    )
  private val confirmBtnTextProp =
    A2uiProperty.dynamicString(
      key = "confirmBtnText",
      required = false,
      description = "Primary CTA button text.",
    )
  private val actionProp =
    A2uiProperty.action(
      key = "action",
      required = false,
      description = "Action dispatched when room tier is selected.",
    )

  override val name: String = "HotelRoomSelector"
  override val description: String =
    "Suite and room tier selector with amenity chips and live data model binding."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      aiReasonProp,
      optionsProp,
      selectedRoomIdProp,
      selectedRoomNameProp,
      nightlyRateProp,
      confirmBtnTextProp,
      actionProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(optionsProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val aiReason =
      properties.bind(aiReasonProp)
        ?: "Matched for proximity to your itinerary events and high guest tranquility scores."
    val rawOptions = properties.bind(optionsProp) ?: emptyList()
    val boundRoomId = properties.bind(selectedRoomIdProp)
    val updateRoomId = properties.bindUpdater(selectedRoomIdProp)
    val updateRoomName = properties.bindUpdater(selectedRoomNameProp)
    val updateNightlyRate = properties.bindUpdater(nightlyRateProp)
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Customize Stay & Check-In"
    val action = properties[actionProp]

    data class ParsedRoom(
      val id: String,
      val name: String,
      val amenities: String,
      val rate: Int,
      val badge: String,
    )

    val parsedRooms =
      remember(rawOptions) {
        rawOptions.mapIndexed { idx, raw ->
          val parts = raw.split("|")
          if (parts.size >= 5) {
            ParsedRoom(
              id = parts[0].trim(),
              name = parts[1].trim(),
              amenities = parts[2].trim(),
              rate = parts[3].trim().toIntOrNull() ?: 240,
              badge =
                parts[4]
                  .trim()
                  .replace(
                    Regex("(?i)(AI\\s+PICK|TOP\\s+PICK)\\s*[•·-]\\s*5\\s*MIN\\s*TO\\s*VENUE"),
                    "POPULAR • BALCONY VIEW",
                  )
                  .replace(Regex("(?i)\\b5\\s*MIN\\s*TO\\s*VENUE\\b"), "BALCONY VIEW")
                  .replace(Regex("(?i)\\bAI\\s+PICK\\b"), "POPULAR")
                  .replace(Regex("(?i)\\bAI\\s+RECOMMENDED\\b"), "MOST POPULAR")
                  .replace(Regex("(?i)\\bAI\\s+"), ""),
            )
          } else {
            ParsedRoom(
              id = "suite_$idx",
              name = raw,
              amenities = "King Bed • High-Speed Wi-Fi • Courtyard View",
              rate = 240,
              badge = if (idx == 0) "POPULAR • BALCONY VIEW" else "AVAILABLE",
            )
          }
        }
      }

    var localSelectedId by
      rememberSaveable(boundRoomId) {
        mutableStateOf(boundRoomId ?: parsedRooms.firstOrNull()?.id ?: "suite_0")
      }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // AI Rationale Banner
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JetMintSoft)
            .border(BorderStroke(1.dp, JetEmeraldAccent), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = JetForestEmerald,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = aiReason,
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              lineHeight = 15.sp,
            ),
          color = JetForestEmerald,
        )
      }

      parsedRooms.forEach { room ->
        val isSelected = room.id == localSelectedId
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xFFFFFEF8))
              .border(
                BorderStroke(
                  if (isSelected) 2.dp else 1.dp,
                  if (isSelected) JetForestEmerald else Color(0xFFE0E4DE),
                ),
                RoundedCornerShape(16.dp),
              )
              .clickable {
                localSelectedId = room.id
                updateRoomId?.invoke(room.id)
                updateRoomName?.invoke(room.name)
                updateNightlyRate?.invoke(room.rate)
              }
              .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.background(
                    if (isSelected) JetForestEmerald else Color(0xFFE9ECEF),
                    RoundedCornerShape(30.dp),
                  )
                  .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = room.badge.uppercase(),
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                  ),
                color = if (isSelected) JetGold else Color(0xFF677276),
              )
            }

            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$${room.rate}",
                style =
                  MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SekuyaFontFamily,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                  ),
                color = JetForestEmerald,
              )
              Text(
                text = " / night",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                  ),
                color = JetDeepNavy,
              )
            }
          }

          Text(
            text = room.name,
            style =
              MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
              ),
            color = JetDeepNavy,
          )

          Text(
            text = room.amenities,
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.25.sp,
              ),
            color = Color(0xFF48473F),
          )
        }
      }

      val activeRoom = parsedRooms.find { it.id == localSelectedId } ?: parsedRooms.firstOrNull()
      Button(
        onClick = {
          val context =
            mutableMapOf<String, Any>(
              "type" to "select_hotel_room",
              "selectionType" to "hotel_room",
              "selectedRoomId" to (activeRoom?.id ?: "suite_0"),
              "selectedOption" to (activeRoom?.name ?: "Redwood Canopy King Suite"),
              "nightlyRate" to (activeRoom?.rate ?: 240),
            )
          dispatchMergedAction(action, "select_hotel_room", context)
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(containerColor = JetForestEmerald, contentColor = Color.White),
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
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

