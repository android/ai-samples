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
 * Act 2 & Act 3 Sticky Reactive Fare Summary Footer: Subscribes to `/booking/baseFare`,
 * `/booking/seat`, `/booking/seatUpcharge`, `/booking/priorityBoarding`, and `/booking/extraBag`
 * via `properties.bind(...)`. Computes the live total on the client with zero network round-trips
 * and animates digit changes using `AnimatedContent`.
 */
class LiveFareSummaryFooterComponent : A2uiComponent {
  private val baseFareProp =
    A2uiProperty.dynamicNumber(
      key = "baseFare",
      required = true,
      description = "Base flight fare in USD.",
    )
  private val selectedSeatProp =
    A2uiProperty.dynamicString(
      key = "selectedSeat",
      required = false,
      description = "Currently selected seat designator.",
    )
  private val seatUpchargeProp =
    A2uiProperty.dynamicNumber(
      key = "seatUpcharge",
      required = false,
      description = "Upgrade fee for the selected seat.",
    )
  private val priorityBoardingProp =
    A2uiProperty.dynamicBoolean(
      key = "priorityBoarding",
      required = false,
      description = "Whether Priority Boarding (+$25) is selected.",
    )
  private val extraBagProp =
    A2uiProperty.dynamicBoolean(
      key = "extraBag",
      required = false,
      description = "Whether Extra Checked Bag (+$45) is selected.",
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
      description = "Action dispatched when confirming the complete flight configuration.",
    )

  override val name: String = "LiveFareSummaryFooter"
  override val description: String =
    "Reactive live fare calculator bar bound to seat and perk selections in the A2uiDataModel."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      baseFareProp,
      selectedSeatProp,
      seatUpchargeProp,
      priorityBoardingProp,
      extraBagProp,
      confirmBtnTextProp,
      actionProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(baseFareProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val baseFare = properties.bind(baseFareProp)?.toInt() ?: 640
    val selectedSeat = properties.bind(selectedSeatProp) ?: "1A"
    val seatUpcharge = properties.bind(seatUpchargeProp)?.toInt() ?: 0
    val priorityBoarding = properties.bind(priorityBoardingProp) ?: false
    val extraBag = properties.bind(extraBagProp) ?: false
    val confirmBtnText = properties.bind(confirmBtnTextProp) ?: "Issue Pass"
    val action = properties[actionProp]

    val totalFare =
      baseFare + seatUpcharge + (if (priorityBoarding) 25 else 0) + (if (extraBag) 45 else 0)

    val breakdownParts = buildList {
      add("Seat $selectedSeat" + if (seatUpcharge > 0) " (+$$seatUpcharge)" else "")
      if (priorityBoarding) add("Priority (+$25)")
      if (extraBag) add("+1 Bag (+$45)")
    }

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(JetDeepNavy)
          .border(BorderStroke(1.dp, JetSkyCyan.copy(alpha = 0.5f)), RoundedCornerShape(22.dp))
          .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "TOTAL FARE",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                fontSize = 9.sp,
              ),
            color = JetSkyCyan,
          )

          Text(
            text = breakdownParts.joinToString(" • "),
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
              ),
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        AnimatedContent(
          targetState = totalFare,
          transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                  slideOutVertically { height -> -height } + fadeOut()
                )
              } else {
                (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                  slideOutVertically { height -> height } + fadeOut()
                )
              }
              .using(SizeTransform(clip = false))
          },
          label = "RollingTotalFare",
        ) { targetTotal ->
          Text(
            text = "$$targetTotal",
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontFamily = SekuyaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 32.sp,
              ),
            color = Color.White,
            maxLines = 1,
          )
        }
      }

      Button(
        onClick = {
          val context =
            mutableMapOf<String, Any>(
              "type" to "confirm_seat",
              "selectionType" to "confirm_flight_config",
              "selectedSeat" to selectedSeat,
              "seat" to selectedSeat,
              "totalFare" to totalFare,
              "priorityBoarding" to priorityBoarding,
              "extraBag" to extraBag,
            )
          dispatchMergedAction(action, "confirm_flight_config", context)
        },
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(containerColor = JetSkyCyan, contentColor = JetDeepNavy),
        modifier = Modifier.fillMaxWidth().height(46.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
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
          )
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

