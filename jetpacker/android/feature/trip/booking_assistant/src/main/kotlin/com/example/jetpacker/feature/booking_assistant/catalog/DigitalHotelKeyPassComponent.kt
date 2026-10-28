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
 * Step 3 of Boutique Hotel Flow (`DigitalHotelKeyPass`): Morphs the surface in-place into a
 * confirmed NFC Digital Room Key & Stay Pass with an interactive contactless key modal.
 */
class DigitalHotelKeyPassComponent : A2uiComponent {
  private val hotelNameProp =
    A2uiProperty.dynamicString(key = "hotelName", required = true, description = "Hotel name.")
  private val roomNameProp =
    A2uiProperty.dynamicString(key = "roomName", required = false, description = "Suite tier name.")
  private val roomNumberProp =
    A2uiProperty.dynamicString(
      key = "roomNumber",
      required = false,
      description = "Assigned suite/room number.",
    )
  private val floorPrefProp =
    A2uiProperty.dynamicString(
      key = "floorPreference",
      required = false,
      description = "Assigned floor & vibe.",
    )
  private val checkInDateProp =
    A2uiProperty.dynamicString(
      key = "checkInDate",
      required = false,
      description = "Check-in date.",
    )
  private val checkOutDateProp =
    A2uiProperty.dynamicString(
      key = "checkOutDate",
      required = false,
      description = "Check-out date.",
    )
  private val checkInTimeProp =
    A2uiProperty.dynamicString(
      key = "checkInTime",
      required = false,
      description = "Confirmed check-in window.",
    )
  private val confirmationCodeProp =
    A2uiProperty.dynamicString(
      key = "confirmationCode",
      required = false,
      description = "Hotel reservation confirmation code.",
    )
  private val totalStayPriceProp =
    A2uiProperty.dynamicNumber(
      key = "totalStayPrice",
      required = false,
      description = "Total confirmed stay price.",
    )
  private val breakfastProp =
    A2uiProperty.dynamicBoolean(
      key = "breakfastIncluded",
      required = false,
      description = "Breakfast package included.",
    )
  private val lateCheckoutProp =
    A2uiProperty.dynamicBoolean(
      key = "lateCheckout",
      required = false,
      description = "4 PM late checkout included.",
    )

  override val name: String = "DigitalHotelKeyPass"
  override val description: String =
    "Confirmed hotel reservation pass with interactive NFC digital room key."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      hotelNameProp,
      roomNameProp,
      roomNumberProp,
      floorPrefProp,
      checkInDateProp,
      checkOutDateProp,
      checkInTimeProp,
      confirmationCodeProp,
      totalStayPriceProp,
      breakfastProp,
      lateCheckoutProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(hotelNameProp) != null
  }

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val hotelName = properties.bind(hotelNameProp) ?: "The Redwood Grove Hotel"
    val roomName = properties.bind(roomNameProp) ?: "Redwood Canopy King Suite"
    val roomNumber = properties.bind(roomNumberProp) ?: "SUITE 418"
    val floorPref = properties.bind(floorPrefProp) ?: "High Floor • Quiet"
    val checkInDate = properties.bind(checkInDateProp) ?: "OCT 27"
    val checkOutDate = properties.bind(checkOutDateProp) ?: "OCT 31"
    val checkInTime = properties.bind(checkInTimeProp) ?: "11:00 AM Early"
    val confirmationCode = properties.bind(confirmationCodeProp) ?: "RG-8842"
    val totalStayPrice = properties.bind(totalStayPriceProp)?.toInt() ?: 960
    val breakfast = properties.bind(breakfastProp) ?: false
    val lateCheckout = properties.bind(lateCheckoutProp) ?: false

    var showNfcModal by remember { mutableStateOf(false) }

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(Color.White)
          .border(BorderStroke(1.5.dp, Color(0xFF73C6B6)), RoundedCornerShape(22.dp))
    ) {
      // Top Luxury Key Header
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(Brush.linearGradient(colors = listOf(JetForestEmerald, Color(0xFF258B84))))
            .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = JetGoldLabel,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = "DIGITAL ROOM KEY ISSUED",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                  ),
                color = JetGoldLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Box(
              modifier =
                Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "CNF #$confirmationCode",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                  ),
                color = Color.White,
              )
            }
          }

          Text(
            text = hotelName,
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
              ),
            color = Color.White,
          )
          Text(
            text = "$roomName • $floorPref",
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
              ),
            color = Color.White.copy(alpha = 0.9f),
          )
        }
      }

      // Room Assignment + Dates Grid
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(JetMintSoft)
              .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "ASSIGNED SUITE",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                ),
              color = JetMutedSlate,
            )
            Text(
              text = roomNumber,
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontFamily = SekuyaFontFamily,
                  fontSize = 22.sp,
                  lineHeight = 28.sp,
                  fontWeight = FontWeight.Normal,
                ),
              color = JetForestEmerald,
            )
          }

          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "TOTAL PAID",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                ),
              color = JetMutedSlate,
            )
            Text(
              text = "$$totalStayPrice",
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontFamily = SekuyaFontFamily,
                  fontSize = 22.sp,
                  lineHeight = 28.sp,
                  fontWeight = FontWeight.Normal,
                ),
              color = JetDeepNavy,
            )
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "CHECK-IN",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                ),
              color = JetMutedSlate,
            )
            Text(
              text = "$checkInDate • $checkInTime",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Normal,
                  fontSize = 12.sp,
                  lineHeight = 16.sp,
                ),
              color = JetDeepNavy,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "CHECK-OUT",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                ),
              color = JetMutedSlate,
            )
            Text(
              text = "$checkOutDate • ${if (lateCheckout) "4:00 PM Late" else "11:00 AM"}",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Normal,
                  fontSize = 12.sp,
                  lineHeight = 16.sp,
                ),
              color = JetDeepNavy,
            )
          }
        }

        // Included Perks Chips
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          val perks = buildList {
            add("⚡ Express Mobile Key")
            add("🍷 VIP Welcome Amenity")
            if (breakfast) add("🥐 Artisan Breakfast")
            if (lateCheckout) add("🕓 4 PM Late Checkout")
          }
          perks.forEach { perk ->
            Box(
              modifier =
                Modifier.background(Color(0xFFF1F4F8), RoundedCornerShape(6.dp))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = perk,
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                  ),
                color = JetDeepNavy,
              )
            }
          }
        }

        Button(
          onClick = { showNfcModal = true },
          modifier = Modifier.fillMaxWidth().height(46.dp),
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = JetForestEmerald,
              contentColor = Color.White,
            ),
        ) {
          Icon(
            imageVector = Icons.Rounded.QrCode2,
            contentDescription = null,
            tint = JetGoldLabel,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Open NFC Mobile Room Key",
            style =
              MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
              ),
          )
        }
      }
    }

    if (showNfcModal) {
      Dialog(onDismissRequest = { showNfcModal = false }) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(26.dp))
              .background(JetForestEmerald)
              .padding(horizontal = 24.dp, vertical = 22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "HOLD NEAR DOOR LOCK",
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
              ),
            color = JetGold,
          )
          Text(
            text = roomNumber,
            style =
              MaterialTheme.typography.headlineLarge.copy(
                fontFamily = SekuyaFontFamily,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Normal,
              ),
            color = Color.White,
          )
          Box(
            modifier =
              Modifier.size(101.dp)
                .background(JetHotelKeyCircleBg, CircleShape)
                .border(BorderStroke(2.dp, JetGold), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Hotel,
              contentDescription = null,
              tint = JetGold,
              modifier = Modifier.size(42.dp),
            )
          }
          Text(
            text = "$hotelName\n$roomName",
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
              ),
            color = Color.White,
            textAlign = TextAlign.Center,
          )
          Button(
            onClick = { showNfcModal = false },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(containerColor = JetGold, contentColor = JetDeepNavy),
          ) {
            Text(
              text = "Done",
              style =
                MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 14.sp,
                  lineHeight = 20.sp,
                  letterSpacing = 0.1.sp,
                ),
            )
          }
        }
      }
    }
  }
}
