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
 * Step 2 Sticky Footer (`HotelLiveSummaryFooter`): Reactively computes total stay cost
 * (`nightlyRate * nights + breakfast + lateCheckout`) at 60fps directly from the local A2UI
 * DataModel without server round-trips.
 */
class HotelLiveSummaryFooterComponent : A2uiComponent {
  private val nightlyRateProp =
    A2uiProperty.dynamicNumber(
      key = "nightlyRate",
      required = true,
      description = "Nightly suite rate.",
    )
  private val nightsProp =
    A2uiProperty.dynamicNumber(key = "nights", required = false, description = "Number of nights.")
  private val checkInTimeProp =
    A2uiProperty.dynamicString(
      key = "checkInTime",
      required = false,
      description = "Bound arrival check-in time.",
    )
  private val floorPrefProp =
    A2uiProperty.dynamicString(
      key = "floorPreference",
      required = false,
      description = "Bound floor preference.",
    )
  private val breakfastProp =
    A2uiProperty.dynamicBoolean(
      key = "breakfastIncluded",
      required = false,
      description = "Whether daily breakfast (+$28/nt) is enabled.",
    )
  private val lateCheckoutProp =
    A2uiProperty.dynamicBoolean(
      key = "lateCheckout",
      required = false,
      description = "Whether 4 PM late checkout (+$45) is enabled.",
    )
  private val confirmBtnTextProp =
    A2uiProperty.dynamicString(
      key = "confirmBtnText",
      required = false,
      description = "Confirm button text.",
    )
  private val actionProp =
    A2uiProperty.action(
      key = "action",
      required = false,
      description = "Action dispatched when confirming stay.",
    )

  override val name: String = "HotelLiveSummaryFooter"
  override val description: String =
    "Reactive stay total footer that updates instantaneously as hotel options are toggled."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      nightlyRateProp,
      nightsProp,
      checkInTimeProp,
      floorPrefProp,
      breakfastProp,
      lateCheckoutProp,
      confirmBtnTextProp,
      actionProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(nightlyRateProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val nightlyRate = properties.bind(nightlyRateProp)?.toInt() ?: 240
    val nights = properties.bind(nightsProp)?.toInt() ?: 4
    val checkInTime = properties.bind(checkInTimeProp) ?: "11:00 AM Early"
    val floorPref = properties.bind(floorPrefProp) ?: "High Floor • Quiet"
    val breakfast = properties.bind(breakfastProp) ?: false
    val lateCheckout = properties.bind(lateCheckoutProp) ?: false
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Confirm Suite & Issue Key"
    val action = properties[actionProp]

    val baseStay = nightlyRate * nights
    val breakfastTotal = if (breakfast) 28 * nights else 0
    val lateTotal = if (lateCheckout) 45 else 0
    val totalStayPrice = baseStay + breakfastTotal + lateTotal

    val summaryParts = buildList {
      add("$nights Nights ($$nightlyRate/nt)")
      add(checkInTime)
      if (breakfast) add("Breakfast (+$$breakfastTotal)")
      if (lateCheckout) add("4PM Checkout (+$45)")
    }

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(JetForestEmerald)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "STAY TOTAL",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
              ),
            color = JetGoldLabel,
          )
          Text(
            text = summaryParts.joinToString(" • "),
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
              ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Text(
          text = "$$totalStayPrice",
          style =
            MaterialTheme.typography.headlineSmall.copy(
              fontFamily = SekuyaFontFamily,
              fontSize = 24.sp,
              lineHeight = 32.sp,
              fontWeight = FontWeight.Normal,
            ),
          color = Color.White,
        )
      }

      Button(
        onClick = {
          val context =
            mutableMapOf<String, Any>(
              "type" to "confirm_hotel_config",
              "selectionType" to "confirm_hotel_config",
              "checkInTime" to checkInTime,
              "floorPreference" to floorPref,
              "breakfastIncluded" to breakfast,
              "lateCheckout" to lateCheckout,
              "totalFare" to totalStayPrice,
            )
          dispatchMergedAction(action, "confirm_hotel_config", context)
        },
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = JetGold, contentColor = JetDeepNavy),
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
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

