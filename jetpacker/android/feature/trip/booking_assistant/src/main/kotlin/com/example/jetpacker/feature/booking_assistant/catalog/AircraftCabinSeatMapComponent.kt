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
 * Act 2 & Act 3 Interactive Aircraft Fuselage & Cabin Seat Map:
 * - Draws a curved aircraft fuselage nose cone, cockpit glazing, cabin windows, and swept wings
 *   using Jetpack Compose `Canvas`.
 * - Supports two-way data binding (`bindUpdater`) to `/booking/seat` and `/booking/seatUpcharge`.
 * - Detects surgical server-pushed `updateDataModel` changes to `/cabin/occupiedSeats` and pulses
 *   newly taken seats in Tangelo Coral without rebuilding the component hierarchy.
 */
class AircraftCabinSeatMapComponent : A2uiComponent {
  private val aircraftProp =
    A2uiProperty.dynamicString(
      key = "aircraft",
      required = false,
      description = "Aircraft model and cabin deck title.",
    )
  private val occupiedSeatsProp =
    A2uiProperty.dynamicStringList(
      key = "occupiedSeats",
      required = false,
      description = "List of occupied seat designators.",
    )
  private val liveAlertProp =
    A2uiProperty.dynamicString(
      key = "liveAlert",
      required = false,
      description = "Live surgical server update banner text.",
    )
  private val selectedSeatProp =
    A2uiProperty.dynamicString(
      key = "selectedSeat",
      required = false,
      description = "Two-way bound currently selected seat.",
    )
  private val seatUpchargeProp =
    A2uiProperty.dynamicNumber(
      key = "seatUpcharge",
      required = false,
      description = "Two-way bound seat tier upgrade fee in USD.",
    )

  override val name: String = "AircraftCabinSeatMap"
  override val description: String =
    "Interactive aircraft fuselage cabin seat map with live seat availability patching."
  override val properties: List<A2uiProperty<*>> =
    listOf(aircraftProp, occupiedSeatsProp, liveAlertProp, selectedSeatProp, seatUpchargeProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean = true

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val aircraft = properties.bind(aircraftProp) ?: "Widebody Front Cabin"
    val occupiedSeats = properties.bind(occupiedSeatsProp) ?: listOf("1B", "3B")
    val liveAlert = properties.bind(liveAlertProp).orEmpty()
    val boundSeat = properties.bind(selectedSeatProp)
    val updateSeat = properties.bindUpdater(selectedSeatProp)
    val updateUpcharge = properties.bindUpdater(seatUpchargeProp)

    var fallbackSeat by rememberSaveable { mutableStateOf("1A") }
    val selectedSeat = boundSeat ?: fallbackSeat

    // Track surgical server additions to occupiedSeats so we can highlight newly booked seats
    var initialOccupiedSnapshot by remember { mutableStateOf<Set<String>?>(null) }
    var newlyBookedSeats by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(occupiedSeats) {
      val currentSet = occupiedSeats.toSet()
      val baseline = initialOccupiedSnapshot
      if (baseline == null) {
        initialOccupiedSnapshot = currentSet
      } else {
        val added = currentSet - baseline
        if (added.isNotEmpty()) {
          newlyBookedSeats = newlyBookedSeats + added
        }
      }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // Section Title & Legend Stacked Cleanly for Mobile Width
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AirlineSeatReclineExtra,
            contentDescription = null,
            tint = JetDeepNavy,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = aircraft.uppercase(),
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = SekuyaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
              ),
            color = JetDeepNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CabinLegendDot(color = JetDeepNavy, label = "Yours")
          CabinLegendDot(color = JetRoyalCobalt, label = "Suite +$85")
          CabinLegendDot(color = JetMutedSlate.copy(alpha = 0.45f), label = "Taken")
        }
      }

      // Live Surgical Server Patch Alert Banner
      AnimatedVisibility(
        visible = liveAlert.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(JetTangeloSoft)
              .border(BorderStroke(1.dp, JetTangelo), RoundedCornerShape(14.dp))
              .padding(horizontal = 12.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.background(JetTangelo, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "LIVE",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = SekuyaFontFamily,
                  fontSize = 9.sp,
                ),
              color = Color.White,
            )
          }
          Text(
            text = liveAlert,
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
              ),
            color = JetDeepNavy,
          )
        }
      }

      // Custom Compose Canvas Aircraft Fuselage + Interactive Seat Deck
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        // Aircraft Nose Cone, Cockpit Glazing, Fuselage Walls & Swept Wings Backdrop
        Canvas(modifier = Modifier.matchParentSize()) {
          val w = size.width
          val h = size.height
          val fuselageLeft = w * 0.03f
          val fuselageRight = w * 0.97f

          // Swept wings on left & right sides
          val leftWing =
            Path().apply {
              moveTo(fuselageLeft, h * 0.38f)
              lineTo(0f, h * 0.52f)
              lineTo(0f, h * 0.68f)
              lineTo(fuselageLeft, h * 0.62f)
              close()
            }
          val rightWing =
            Path().apply {
              moveTo(fuselageRight, h * 0.38f)
              lineTo(w, h * 0.52f)
              lineTo(w, h * 0.68f)
              lineTo(fuselageRight, h * 0.62f)
              close()
            }
          drawPath(leftWing, color = JetSkyCyan.copy(alpha = 0.25f))
          drawPath(rightWing, color = JetSkyCyan.copy(alpha = 0.25f))

          // Fuselage outer shell
          val fuselagePath =
            Path().apply {
              moveTo(fuselageLeft, h)
              lineTo(fuselageLeft, h * 0.18f)
              quadraticTo(w * 0.5f, -h * 0.05f, fuselageRight, h * 0.18f)
              lineTo(fuselageRight, h)
              close()
            }
          drawPath(
            path = fuselagePath,
            brush =
              Brush.verticalGradient(
                colors = listOf(JetSkyCyanSoft.copy(alpha = 0.55f), JetWarmCream.copy(alpha = 0.5f))
              ),
          )
          drawPath(
            path = fuselagePath,
            color = JetDeepNavy.copy(alpha = 0.22f),
            style = Stroke(width = 2.dp.toPx()),
          )

          // Cockpit windshield arc
          val cockpitPath =
            Path().apply {
              moveTo(w * 0.36f, h * 0.08f)
              quadraticTo(w * 0.5f, h * 0.035f, w * 0.64f, h * 0.08f)
            }
          drawPath(
            path = cockpitPath,
            color = JetDeepNavy.copy(alpha = 0.35f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
          )

          // Cabin window indicator ticks along fuselage walls
          val rowYPositions = listOf(0.32f, 0.50f, 0.71f, 0.88f)
          rowYPositions.forEach { frac ->
            val y = h * frac
            drawRoundRect(
              color = JetSkyCyan,
              topLeft = Offset(fuselageLeft + 3.dp.toPx(), y - 8.dp.toPx()),
              size = Size(4.dp.toPx(), 16.dp.toPx()),
              cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
              color = JetSkyCyan,
              topLeft = Offset(fuselageRight - 7.dp.toPx(), y - 8.dp.toPx()),
              size = Size(4.dp.toPx(), 16.dp.toPx()),
              cornerRadius = CornerRadius(4f, 4f),
            )
          }
        }

        // Interactive Cabin Seat Grid
        Column(
          modifier = Modifier.fillMaxWidth(0.90f).padding(top = 30.dp, bottom = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Column Headers: A (Win)   B (Aisle)   |   C (Aisle)   D (Win)
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("A • WIN", style = cabinColumnHeaderStyle(), color = JetSlate)
            Text("B", style = cabinColumnHeaderStyle(), color = JetSlate)
            Text("AISLE", style = cabinColumnHeaderStyle(), color = JetMutedSlate)
            Text("C", style = cabinColumnHeaderStyle(), color = JetSlate)
            Text("WIN • D", style = cabinColumnHeaderStyle(), color = JetSlate)
          }

          // Rows 1 & 2: Business Class Suites (+$85)
          listOf(1, 2).forEach { rowNum ->
            CabinSeatRow(
              rowNumber = rowNum,
              isBusinessSuite = true,
              upcharge = 85,
              selectedSeat = selectedSeat,
              occupiedSeats = occupiedSeats,
              newlyBookedSeats = newlyBookedSeats,
              onSelectSeat = { seatId, fee ->
                fallbackSeat = seatId
                updateSeat?.invoke(seatId)
                updateUpcharge?.invoke(fee)
              },
            )
          }

          // Cabin Class Divider
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFCBD5E1)))
            Text(
              text = "COMFORT+ CABIN (+$35 / INCL)",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp,
                  letterSpacing = 0.5.sp,
                ),
              color = JetRoyalCobalt,
              maxLines = 1,
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFCBD5E1)))
          }

          // Rows 3 & 4: Comfort+ & Main Cabin (+$35 / $0)
          listOf(3, 4).forEach { rowNum ->
            val fee = if (rowNum == 3) 35 else 0
            CabinSeatRow(
              rowNumber = rowNum,
              isBusinessSuite = false,
              upcharge = fee,
              selectedSeat = selectedSeat,
              occupiedSeats = occupiedSeats,
              newlyBookedSeats = newlyBookedSeats,
              onSelectSeat = { seatId, seatFee ->
                fallbackSeat = seatId
                updateSeat?.invoke(seatId)
                updateUpcharge?.invoke(seatFee)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CabinSeatRow(
  rowNumber: Int,
  isBusinessSuite: Boolean,
  upcharge: Int,
  selectedSeat: String,
  occupiedSeats: List<String>,
  newlyBookedSeats: Set<String>,
  onSelectSeat: (String, Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf("A", "B").forEach { col ->
        val seatId = "$rowNumber$col"
        CabinSeatTile(
          seatId = seatId,
          isBusinessSuite = isBusinessSuite,
          upcharge = upcharge,
          isSelected = selectedSeat.equals(seatId, ignoreCase = true),
          isOccupied =
            occupiedSeats.any { it.equals(seatId, ignoreCase = true) } &&
              !selectedSeat.equals(seatId, ignoreCase = true),
          isNewlyBooked = newlyBookedSeats.any { it.equals(seatId, ignoreCase = true) },
          onClick = { onSelectSeat(seatId, upcharge) },
        )
      }
    }

    // Center Aisle Row Indicator
    Box(
      modifier =
        Modifier.width(22.dp)
          .height(22.dp)
          .background(JetWarmCream, CircleShape)
          .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = rowNumber.toString(),
        style =
          MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        color = JetSlate,
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf("C", "D").forEach { col ->
        val seatId = "$rowNumber$col"
        CabinSeatTile(
          seatId = seatId,
          isBusinessSuite = isBusinessSuite,
          upcharge = upcharge,
          isSelected = selectedSeat.equals(seatId, ignoreCase = true),
          isOccupied =
            occupiedSeats.any { it.equals(seatId, ignoreCase = true) } &&
              !selectedSeat.equals(seatId, ignoreCase = true),
          isNewlyBooked = newlyBookedSeats.any { it.equals(seatId, ignoreCase = true) },
          onClick = { onSelectSeat(seatId, upcharge) },
        )
      }
    }
  }
}

@Composable
private fun CabinSeatTile(
  seatId: String,
  isBusinessSuite: Boolean,
  upcharge: Int,
  isSelected: Boolean,
  isOccupied: Boolean,
  isNewlyBooked: Boolean,
  onClick: () -> Unit,
) {
  val scale by
    animateFloatAsState(
      targetValue =
        when {
          isSelected -> 1.08f
          isNewlyBooked -> 1.05f
          else -> 1.0f
        },
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      label = "SeatScale",
    )

  val bgColor by
    animateColorAsState(
      targetValue =
        when {
          isSelected -> JetDeepNavy
          isNewlyBooked -> JetTangeloSoft
          isOccupied -> Color(0xFFE2E8F0)
          isBusinessSuite -> Color.White
          else -> JetWarmCream
        },
      label = "SeatBg",
    )

  val borderColor by
    animateColorAsState(
      targetValue =
        when {
          isSelected -> JetSkyCyan
          isNewlyBooked -> JetTangelo
          isOccupied -> Color(0xFFCBD5E1)
          isBusinessSuite -> JetRoyalCobalt
          else -> Color(0xFF94A3B8)
        },
      label = "SeatBorder",
    )

  val tileHeight = if (isBusinessSuite) 48.dp else 44.dp
  val shape =
    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

  Box(
    modifier =
      Modifier.width(44.dp)
        .height(tileHeight)
        .scale(scale)
        .clip(shape)
        .background(bgColor)
        .border(BorderStroke(if (isSelected || isNewlyBooked) 2.dp else 1.2.dp, borderColor), shape)
        .clickable(enabled = !isOccupied, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    // Headrest pillow bar at top of seat
    Box(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .padding(top = 3.dp)
          .width(26.dp)
          .height(4.dp)
          .clip(RoundedCornerShape(50))
          .background(
            when {
              isSelected -> JetSkyCyan
              isNewlyBooked -> JetTangelo
              isOccupied -> Color(0xFF94A3B8)
              isBusinessSuite -> JetRoyalCobalt.copy(alpha = 0.45f)
              else -> JetMutedSlate.copy(alpha = 0.45f)
            }
          )
    )

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(top = 4.dp),
    ) {
      Text(
        text = if (isOccupied && !isNewlyBooked) "✕" else seatId,
        style =
          MaterialTheme.typography.labelMedium.copy(
            fontFamily = SekuyaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
          ),
        color =
          when {
            isSelected -> Color.White
            isNewlyBooked -> JetTangelo
            isOccupied -> JetMutedSlate
            else -> JetDeepNavy
          },
      )
      Text(
        text =
          when {
            isSelected -> "YOURS"
            isNewlyBooked -> "TAKEN"
            isOccupied -> "BOOKED"
            upcharge > 0 -> "+$$upcharge"
            else -> "INCL"
          },
        style =
          MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp),
        color =
          when {
            isSelected -> JetSkyCyan
            isNewlyBooked -> JetTangelo
            isOccupied -> JetMutedSlate
            upcharge > 0 -> JetRoyalCobalt
            else -> JetSlate
          },
      )
    }
  }
}

@Composable
private fun CabinLegendDot(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
    Text(
      text = label,
      style =
        MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
      color = JetSlate,
    )
  }
}

@Composable
private fun cabinColumnHeaderStyle() =
  MaterialTheme.typography.labelSmall.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 9.sp,
    letterSpacing = 0.4.sp,
  )

