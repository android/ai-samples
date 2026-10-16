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
 * Act 4 Perforated Boarding Pass Card: The confirmed finale surface featuring semicircular ticket
 * punch-out notches, `SekuyaFontFamily` airport codes, 4-column gate/seat telemetry grid, perk
 * chips, and an interactive tap-to-enlarge QR pass (`CoreUiR.drawable.qr`).
 */
class PerforatedBoardingPassComponent : A2uiComponent {
  private val airlineProp =
    A2uiProperty.dynamicString(
      "airline",
      required = false,
      description = "Airline & flight number.",
    )
  private val originCodeProp =
    A2uiProperty.dynamicString("originCode", required = true, description = "Origin IATA code.")
  private val originCityProp =
    A2uiProperty.dynamicString("originCity", required = false, description = "Origin city name.")
  private val destCodeProp =
    A2uiProperty.dynamicString("destCode", required = true, description = "Destination IATA code.")
  private val destCityProp =
    A2uiProperty.dynamicString("destCity", required = false, description = "Destination city name.")
  private val flightTimeProp =
    A2uiProperty.dynamicString("flightTime", required = false, description = "Flight time slot.")
  private val seatProp =
    A2uiProperty.dynamicString("seat", required = true, description = "Confirmed seat designator.")
  private val gateProp =
    A2uiProperty.dynamicString("gate", required = false, description = "Departure gate.")
  private val terminalProp =
    A2uiProperty.dynamicString("terminal", required = false, description = "Departure terminal.")
  private val boardingTimeProp =
    A2uiProperty.dynamicString("boardingTime", required = false, description = "Boarding time.")
  private val confirmationCodeProp =
    A2uiProperty.dynamicString("confirmationCode", required = false, description = "PNR reference.")
  private val totalFareProp =
    A2uiProperty.dynamicNumber("totalFare", required = false, description = "Total fare paid.")
  private val priorityBoardingProp =
    A2uiProperty.dynamicBoolean(
      "priorityBoarding",
      required = false,
      description = "Priority perk.",
    )
  private val extraBagProp =
    A2uiProperty.dynamicBoolean("extraBag", required = false, description = "Extra bag perk.")

  override val name: String = "PerforatedBoardingPass"
  override val description: String =
    "Confirmed perforated boarding pass with QR code, gate telemetry, and selected seat/perks."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      airlineProp,
      originCodeProp,
      originCityProp,
      destCodeProp,
      destCityProp,
      flightTimeProp,
      seatProp,
      gateProp,
      terminalProp,
      boardingTimeProp,
      confirmationCodeProp,
      totalFareProp,
      priorityBoardingProp,
      extraBagProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(originCodeProp) != null && properties.bind(destCodeProp) != null
  }

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val airline = properties.bind(airlineProp) ?: "UNITED • UA 991"
    val originCode = properties.bind(originCodeProp) ?: "CDG"
    val originCity = properties.bind(originCityProp) ?: "Paris"
    val destCode = properties.bind(destCodeProp) ?: "SFO"
    val destCity = properties.bind(destCityProp) ?: "San Francisco"
    val flightTime = properties.bind(flightTimeProp) ?: "10:15 AM – 12:30 PM"
    val seat = properties.bind(seatProp) ?: "1A"
    val gate = properties.bind(gateProp) ?: "K42"
    val terminal = properties.bind(terminalProp) ?: "2E"
    val boardingTime = properties.bind(boardingTimeProp) ?: "09:35 AM"
    val rawConfirmationCode = properties.bind(confirmationCodeProp) ?: "UA-88420-X"
    val confirmationCode = rawConfirmationCode.replace(Regex("(?i)^A2UI-"), "UA-")
    val totalFare = properties.bind(totalFareProp)?.toInt() ?: 725
    val priorityBoarding = properties.bind(priorityBoardingProp) ?: false
    val extraBag = properties.bind(extraBagProp) ?: false
    val cleanTerminal = terminal.replace(Regex("(?i)^terminal\\s*"), "").trim().ifEmpty { "2E" }
    val cleanGate = gate.replace(Regex("(?i)^gate\\s*"), "").trim().ifEmpty { "K42" }

    var showQrModal by remember { mutableStateOf(false) }

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White)
          .border(BorderStroke(1.5.dp, JetSkyCyan), RoundedCornerShape(24.dp))
    ) {
      // Top Sky Cyan Boarding Pass Header
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              Brush.verticalGradient(colors = listOf(JetSkyCyan.copy(alpha = 0.65f), Color.White))
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Verified,
              contentDescription = null,
              tint = JetEmerald,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = airline.uppercase(),
              style =
                MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.5.sp,
                  fontSize = 11.5.sp,
                ),
              color = JetDeepNavy,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Box(
            modifier =
              Modifier.background(JetEmeraldSoft, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "BOARDING PASS",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.5.sp,
                  letterSpacing = 0.4.sp,
                ),
              color = JetEmerald,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        AnimatedFlightRouteHeader(
          originCode = originCode,
          originCity = originCity,
          destCode = destCode,
          destCity = destCity,
        )

        Text(
          text = flightTime,
          style =
            MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
            ),
          color = JetDeepNavy,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
        )
      }

      // 4-Column Gate / Terminal / Boarding / Seat Telemetry Grid
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(JetWarmCream)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BoardingPassStatCol(
          label = "TERMINAL",
          value = cleanTerminal,
          modifier = Modifier.weight(1f),
        )
        BoardingPassStatCol(label = "GATE", value = cleanGate, modifier = Modifier.weight(1f))
        BoardingPassStatCol(
          label = "BOARDING",
          value = boardingTime,
          modifier = Modifier.weight(1.3f),
        )
        BoardingPassStatCol(
          label = "SEAT",
          value = seat,
          highlight = true,
          modifier = Modifier.weight(1f),
        )
      }

      // Active Perks & Upgrades Chips
      FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val isSuite = seat.startsWith("1") || seat.startsWith("2")
        BoardingPassPerkChip(
          text = if (isSuite) "✦ Business Suite $seat" else "✦ Comfort+ Seat $seat",
          bgColor = JetCobaltSoft,
          textColor = JetRoyalCobalt,
        )
        if (priorityBoarding) {
          BoardingPassPerkChip(
            text = "✦ Zone 1 Priority",
            bgColor = Color(0xFFFFF8E1),
            textColor = Color(0xFFB45309),
          )
        }
        if (extraBag) {
          BoardingPassPerkChip(
            text = "✦ +1 Checked Bag (23kg)",
            bgColor = JetEmeraldSoft,
            textColor = JetEmerald,
          )
        }
      }

      // Perforated Ticket Tear Line with Semicircular Side Cutout Notches
      Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
        val midY = size.height / 2f
        val notchRadius = 11.dp.toPx()

        drawLine(
          color = JetMutedSlate.copy(alpha = 0.55f),
          start = Offset(notchRadius + 8.dp.toPx(), midY),
          end = Offset(size.width - notchRadius - 8.dp.toPx(), midY),
          strokeWidth = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
        )
        // Left & right ticket punch notches matching the outer card background
        drawCircle(color = JetCardCream, radius = notchRadius, center = Offset(0f, midY))
        drawCircle(color = JetCardCream, radius = notchRadius, center = Offset(size.width, midY))
        drawCircle(
          color = JetSkyCyan,
          radius = notchRadius,
          center = Offset(0f, midY),
          style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
          color = JetSkyCyan,
          radius = notchRadius,
          center = Offset(size.width, midY),
          style = Stroke(width = 1.5.dp.toPx()),
        )
      }

      // Bottom Tear-Off Stub with QR Code & Total Paid
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable { showQrModal = true }
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 10.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "CONFIRMATION • $confirmationCode",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                fontSize = 10.sp,
              ),
            color = JetSlate,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Total Paid • $$totalFare",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontFamily = SekuyaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              ),
            color = JetDeepNavy,
            maxLines = 1,
          )
          Text(
            text = "Tap QR pass to enlarge for gate scanner",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = JetRoyalCobalt,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Box(
          modifier =
            Modifier.size(58.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(JetWarmCream)
              .border(BorderStroke(1.dp, JetDeepNavy.copy(alpha = 0.2f)), RoundedCornerShape(14.dp))
              .padding(6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = CoreUiR.drawable.qr),
            contentDescription = "Boarding Pass QR Code",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(46.dp),
          )
        }
      }
    }

    if (showQrModal) {
      Dialog(onDismissRequest = { showQrModal = false }) {
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = Color.White,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = originCode.uppercase(),
                  style =
                    MaterialTheme.typography.headlineLarge.copy(
                      fontFamily = SekuyaFontFamily,
                      fontSize = 32.sp,
                      lineHeight = 40.sp,
                      fontWeight = FontWeight.Normal,
                    ),
                  color = JetDeepNavy,
                )
                Icon(
                  imageVector = Icons.Rounded.Flight,
                  contentDescription = null,
                  tint = JetDeepNavy,
                  modifier = Modifier.size(28.dp).rotate(90f),
                )
                Text(
                  text = destCode.uppercase(),
                  style =
                    MaterialTheme.typography.headlineLarge.copy(
                      fontFamily = SekuyaFontFamily,
                      fontSize = 32.sp,
                      lineHeight = 40.sp,
                      fontWeight = FontWeight.Normal,
                    ),
                  color = JetDeepNavy,
                )
              }

              Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFB0C4D8)))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Gate $cleanGate",
                  style =
                    MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Medium,
                      fontSize = 16.sp,
                      lineHeight = 24.sp,
                      letterSpacing = 0.15.sp,
                    ),
                  color = JetDeepNavy,
                )
                Text(
                  text = "Seat $seat",
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

            Image(
              painter = painterResource(id = CoreUiR.drawable.qr),
              contentDescription = "Full Boarding Pass QR",
              modifier = Modifier.size(160.dp).padding(vertical = 4.dp),
              contentScale = ContentScale.Fit,
            )

            val airlineName =
              airline
                .substringBefore("•")
                .trim()
                .lowercase()
                .replaceFirstChar { it.uppercase() }
                .ifEmpty { "United Airlines" }
            val flightNumbers = airline.substringAfter("•", "UA 1003 • UA 1007").trim()

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = airlineName,
                style =
                  MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp,
                  ),
                color = JetMutedSlate,
              )
              Text(
                text = flightNumbers,
                style =
                  MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp,
                  ),
                color = JetMutedSlate,
              )
            }

            Button(
              onClick = { showQrModal = false },
              colors = ButtonDefaults.buttonColors(containerColor = JetDeepNavy),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text(
                text = "Done",
                color = JetCardCream,
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
}

@Composable
private fun BoardingPassStatCol(
  label: String,
  value: String,
  highlight: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 9.sp,
          letterSpacing = 0.4.sp,
        ),
      color = JetSlate,
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style =
        MaterialTheme.typography.titleMedium.copy(
          fontFamily = SekuyaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 13.5.sp,
        ),
      color = if (highlight) JetTangelo else JetDeepNavy,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun BoardingPassPerkChip(text: String, bgColor: Color, textColor: Color) {
  Box(
    modifier =
      Modifier.background(bgColor, RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(
      text = text,
      style =
        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
      color = textColor,
    )
  }
}

/**
 * Helper to merge custom action context into an A2UI action map and dispatch it via
 * [A2uiComponentScope.dispatchAction].
 */


