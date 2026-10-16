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
 * Progressive Root Container for the Boutique Hotel Stay Configurator (`HotelConfiguratorShell`).
 * Showcases a distinct hospitality visual design (Deep Forest Emerald & Warm Gold) while sharing
 * the same A2UI v0.9 progressive `bodySlot` / `footerSlot` architecture.
 */
class HotelConfiguratorShellComponent : A2uiComponent {
  private val hotelNameProp =
    A2uiProperty.dynamicString(
      key = "hotelName",
      required = true,
      description = "Hotel property name.",
    )
  private val locationProp =
    A2uiProperty.dynamicString(
      key = "location",
      required = false,
      description = "Neighborhood or city label.",
    )
  private val checkInDateProp =
    A2uiProperty.dynamicString(
      key = "checkInDate",
      required = false,
      description = "Check-in date label (e.g., OCT 27).",
    )
  private val checkOutDateProp =
    A2uiProperty.dynamicString(
      key = "checkOutDate",
      required = false,
      description = "Check-out date label (e.g., OCT 31).",
    )
  private val nightsProp =
    A2uiProperty.dynamicNumber(key = "nights", required = false, description = "Number of nights.")
  private val statusBadgeProp =
    A2uiProperty.dynamicString(
      key = "statusBadge",
      required = false,
      description = "Live status pill text.",
    )
  private val bodySlotProp =
    A2uiProperty.componentId(
      key = "bodySlot",
      required = false,
      description = "Component ID for the main hotel configurator body slot.",
    )
  private val footerSlotProp =
    A2uiProperty.componentId(
      key = "footerSlot",
      required = false,
      description = "Component ID for the live stay rate footer slot.",
    )

  override val name: String = "HotelConfiguratorShell"
  override val description: String =
    "Progressive boutique hotel stay configurator shell with check-in timeline and child slots."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      hotelNameProp,
      locationProp,
      checkInDateProp,
      checkOutDateProp,
      nightsProp,
      statusBadgeProp,
      bodySlotProp,
      footerSlotProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(hotelNameProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val hotelName = properties.bind(hotelNameProp) ?: "The Redwood Grove Hotel"
    val rawLocation = properties.bind(locationProp) ?: "San Francisco • 4-Star Boutique"
    val location =
      rawLocation.replace(Regex("(?i)${Regex.escape(hotelName)}\\s*[•·-]?\\s*"), "").trim().let {
        cleaned ->
        if (cleaned.isEmpty() || cleaned.equals("4-Star Boutique", ignoreCase = true)) {
          "San Francisco • 4-Star Boutique"
        } else {
          cleaned
        }
      }
    val checkInDate = properties.bind(checkInDateProp) ?: "OCT 27"
    val checkOutDate = properties.bind(checkOutDateProp) ?: "OCT 31"
    val nights = properties.bind(nightsProp)?.toInt() ?: 4
    val bodySlotId = properties[bodySlotProp]
    val footerSlotId = properties[footerSlotProp]

    Column(
      modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(JetWarmCream)
    ) {
      // Hospitality Hero Banner
      Box(
        modifier =
          Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f).padding(end = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier.size(28.dp).background(JetForestEmerald, CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Hotel,
                  contentDescription = null,
                  tint = JetGold,
                  modifier = Modifier.size(15.dp),
                )
              }
              Column {
                Text(
                  text = hotelName.uppercase(),
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Medium,
                      fontSize = 11.sp,
                      lineHeight = 15.sp,
                    ),
                  color = JetDeepNavy,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = location,
                  style =
                    MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                  color = JetMutedSlate,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          // Stay Dates White Card (borderless nights pill + 17.sp CHECK-IN / CHECK-OUT matching
          
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.background(JetMintSoft, RoundedCornerShape(30.dp))
                  .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "$nights NIGHTS STAY",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                  ),
                color = JetForestEmerald,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "CHECK-IN",
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Normal,
                      fontSize = 9.sp,
                      lineHeight = 13.sp,
                    ),
                  color = JetDeepNavy,
                )
                Text(
                  text = checkInDate.uppercase(),
                  style =
                    MaterialTheme.typography.titleMedium.copy(
                      fontFamily = SekuyaFontFamily,
                      fontSize = 17.sp,
                      lineHeight = 21.sp,
                      fontWeight = FontWeight.Normal,
                    ),
                  color = JetForestEmerald,
                  maxLines = 1,
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "CHECK-OUT",
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Normal,
                      fontSize = 9.sp,
                      lineHeight = 13.sp,
                    ),
                  color = JetDeepNavy,
                )
                Text(
                  text = checkOutDate.uppercase(),
                  style =
                    MaterialTheme.typography.titleMedium.copy(
                      fontFamily = SekuyaFontFamily,
                      fontSize = 17.sp,
                      lineHeight = 21.sp,
                      fontWeight = FontWeight.Normal,
                    ),
                  color = JetForestEmerald,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }

      if (!bodySlotId.isNullOrEmpty()) {
        val bodyState = observeA2uiComponentState(id = bodySlotId)
        Box(
          modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
        ) {
          AnimatedContent(
            targetState = bodyState,
            transitionSpec = {
              (fadeIn(animationSpec = tween(320)) +
                  scaleIn(
                    initialScale = 0.97f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  ))
                .togetherWith(fadeOut(animationSpec = tween(180)))
                .using(SizeTransform(clip = false))
            },
            label = "HotelBodySlotTransition",
          ) { state ->
            when (state) {
              is A2uiComponentState.Loading -> FlightSlotShimmerSkeleton(isFooter = false)
              is A2uiComponentState.Error ->
                Text(
                  text = "Unable to load suite options.",
                  color = JetTangelo,
                  style = MaterialTheme.typography.bodySmall,
                )
              is A2uiComponentState.Success -> A2uiComponent(state.component)
            }
          }
        }
      }

      if (!footerSlotId.isNullOrEmpty()) {
        val footerState = observeA2uiComponentState(id = footerSlotId)
        Box(
          modifier =
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp)
        ) {
          AnimatedContent(
            targetState = footerState,
            transitionSpec = {
              (fadeIn(animationSpec = tween(280)) + slideInVertically { it / 3 })
                .togetherWith(fadeOut(animationSpec = tween(150)))
                .using(SizeTransform(clip = false))
            },
            label = "HotelFooterSlotTransition",
          ) { state ->
            when (state) {
              is A2uiComponentState.Loading -> FlightSlotShimmerSkeleton(isFooter = true)
              is A2uiComponentState.Error -> Spacer(modifier = Modifier.height(4.dp))
              is A2uiComponentState.Success -> A2uiComponent(state.component)
            }
          }
        }
      }
    }
  }
}

