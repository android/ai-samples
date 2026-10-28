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
 * Step 2 of Boutique Hotel Flow (`HotelStayCustomizer`): Interactive Check-In Arrival Window
 * selector + Floor & Vibe selector + Live Front Desk VIP server patch banner + hybrid Material 3
 * CheckBox add-ons slot.
 */
class HotelStayCustomizerComponent : A2uiComponent {
  private val selectedRoomNameProp =
    A2uiProperty.dynamicString(
      key = "selectedRoomName",
      required = false,
      description = "Selected suite name.",
    )
  private val checkInTimeProp =
    A2uiProperty.dynamicString(
      key = "checkInTime",
      required = false,
      description = "Two-way bound check-in time window.",
    )
  private val floorPrefProp =
    A2uiProperty.dynamicString(
      key = "floorPreference",
      required = false,
      description = "Two-way bound floor & vibe preference.",
    )
  private val liveAlertProp =
    A2uiProperty.dynamicString(
      key = "liveAlert",
      required = false,
      description = "Live server patch notification from hotel front desk.",
    )
  private val childrenProp =
    A2uiProperty.childList(
      key = "children",
      required = false,
      description = "Child A2UI CheckBox components for stay add-ons.",
    )

  override val name: String = "HotelStayCustomizer"
  override val description: String =
    "Interactive hotel stay customizer with arrival time pills, floor vibe selector, and add-on slots."
  override val properties: List<A2uiProperty<*>> =
    listOf(selectedRoomNameProp, checkInTimeProp, floorPrefProp, liveAlertProp, childrenProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean = true

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val roomName = properties.bind(selectedRoomNameProp) ?: "Redwood Canopy King Suite"
    val boundCheckIn = properties.bind(checkInTimeProp) ?: "11:00 AM Early"
    val updateCheckIn = properties.bindUpdater(checkInTimeProp)
    val boundFloor = properties.bind(floorPrefProp) ?: "High Floor • Quiet"
    val updateFloor = properties.bindUpdater(floorPrefProp)
    val liveAlert = properties.bind(liveAlertProp).orEmpty()
    val childRefs = properties.bindChildReferences(childrenProp).orEmpty()

    var selectedCheckIn by rememberSaveable(boundCheckIn) { mutableStateOf(boundCheckIn) }
    var selectedFloor by rememberSaveable(boundFloor) { mutableStateOf(boundFloor) }

    val checkInWindows = listOf("11:00 AM Early", "3:00 PM Standard", "7:00 PM Evening")
    val floorVibes = listOf("High Floor • Quiet", "Redwood View", "Poolside Access")

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color.White)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Selected Suite Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "STAY CUSTOMIZATION",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
              ),
            color = JetDeepNavy,
          )
          Text(
            text = roomName,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
              ),
            color = JetDeepNavy,
          )
        }
      }

      // Live Server Data Patch Banner (when front desk unlocks VIP perk)
      AnimatedVisibility(
        visible = liveAlert.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(JetMintSoft)
              .border(BorderStroke(1.dp, JetEmeraldAccent), RoundedCornerShape(8.dp))
              .padding(horizontal = 12.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            tint = JetForestEmerald,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = liveAlert,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
              ),
            color = JetForestEmerald,
          )
        }
      }

      // 1. Check-In Arrival Window Picker
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "ARRIVAL & CHECK-IN WINDOW",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              lineHeight = 16.sp,
            ),
          color = JetMutedSlate,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          checkInWindows.forEach { window ->
            val isChosen = window == selectedCheckIn
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(9.dp))
                  .background(if (isChosen) JetForestEmerald else JetCardCream)
                  .border(
                    BorderStroke(1.dp, if (isChosen) JetForestEmerald else Color(0xFFD5D9DF)),
                    RoundedCornerShape(9.dp),
                  )
                  .clickable {
                    selectedCheckIn = window
                    updateCheckIn?.invoke(window)
                  }
                  .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = window,
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.5.sp,
                  ),
                color = if (isChosen) Color.White else JetDeepNavy,
              )
            }
          }
        }
      }

      // 2. Floor & Vibe Preference Picker
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "FLOOR & ROOM VIBE",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              lineHeight = 16.sp,
            ),
          color = JetMutedSlate,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          floorVibes.forEach { vibe ->
            val isChosen = vibe == selectedFloor
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(9.dp))
                  .background(if (isChosen) JetMintSoft else JetCardCream)
                  .border(
                    BorderStroke(
                      if (isChosen) 1.5.dp else 1.dp,
                      if (isChosen) JetForestEmerald else Color(0xFFD5D9DF),
                    ),
                    RoundedCornerShape(9.dp),
                  )
                  .clickable {
                    selectedFloor = vibe
                    updateFloor?.invoke(vibe)
                  }
                  .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = vibe,
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.5.sp,
                  ),
                color = if (isChosen) JetForestEmerald else JetDeepNavy,
              )
            }
          }
        }
      }

      // 3. Hybrid Catalog Standard Material 3 CheckBox Children (Breakfast + Late Checkout)
      if (childRefs.isNotEmpty()) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(JetCardCream)
              .border(BorderStroke(1.dp, Color(0xFFE9ECEF)), RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "HOSPITALITY PACKAGES",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
              ),
            color = JetForestEmerald,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp),
          )
          childRefs.forEach { ref ->
            key(ref.id, ref.baseDataPath) {
              when (val childState = observeA2uiComponentState(id = ref.id)) {
                is A2uiComponentState.Loading -> FlightSlotShimmerSkeleton(isFooter = true)
                is A2uiComponentState.Error -> Spacer(modifier = Modifier.height(2.dp))
                is A2uiComponentState.Success -> A2uiComponent(childState.component)
              }
            }
          }
        }
      }
    }
  }
}

