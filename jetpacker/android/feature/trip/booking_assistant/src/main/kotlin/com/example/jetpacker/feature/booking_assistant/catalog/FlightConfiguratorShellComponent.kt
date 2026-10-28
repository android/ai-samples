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
 * Act 1–3 Progressive Root Container: Renders the live animated route arc header (`originCode ✈
 * destCode`) immediately while observing `bodySlot` and `footerSlot` via
 * `observeA2uiComponentState`. Any unresolved child slot renders a bespoke shimmer skeleton until
 * its component definition arrives over SSE.
 */
class FlightConfiguratorShellComponent : A2uiComponent {
  private val airlineProp =
    A2uiProperty.dynamicString(
      key = "airline",
      required = false,
      description = "Airline and flight number header.",
    )
  private val originCodeProp =
    A2uiProperty.dynamicString(
      key = "originCode",
      required = true,
      description = "Origin 3-letter IATA airport code.",
    )
  private val originCityProp =
    A2uiProperty.dynamicString(
      key = "originCity",
      required = false,
      description = "Origin city or terminal label.",
    )
  private val destCodeProp =
    A2uiProperty.dynamicString(
      key = "destCode",
      required = true,
      description = "Destination 3-letter IATA airport code.",
    )
  private val destCityProp =
    A2uiProperty.dynamicString(
      key = "destCity",
      required = false,
      description = "Destination city or terminal label.",
    )
  private val statusBadgeProp =
    A2uiProperty.dynamicString(
      key = "statusBadge",
      required = false,
      description = "Live step status pill text.",
    )
  private val bodySlotProp =
    A2uiProperty.componentId(
      key = "bodySlot",
      required = false,
      description = "Component ID for the main configurator body slot.",
    )
  private val footerSlotProp =
    A2uiProperty.componentId(
      key = "footerSlot",
      required = false,
      description = "Component ID for the live fare summary footer slot.",
    )

  override val name: String = "FlightConfiguratorShell"
  override val description: String =
    "Progressive flight configurator container with animated trajectory arc and child slots."
  override val properties: List<A2uiProperty<*>> =
    listOf(
      airlineProp,
      originCodeProp,
      originCityProp,
      destCodeProp,
      destCityProp,
      statusBadgeProp,
      bodySlotProp,
      footerSlotProp,
    )

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
    return properties.bind(originCodeProp) != null && properties.bind(destCodeProp) != null
  }

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val airline = properties.bind(airlineProp) ?: "JETPACKER AIR"
    val originCode = properties.bind(originCodeProp) ?: "CDG"
    val originCity = properties.bind(originCityProp) ?: "Origin"
    val destCode = properties.bind(destCodeProp) ?: "SFO"
    val destCity = properties.bind(destCityProp) ?: "Destination"
    val bodySlotId = properties[bodySlotProp]
    val footerSlotId = properties[footerSlotProp]

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Brush.verticalGradient(colors = listOf(Color(0xFFEAF7F5), JetWarmCream)))
          .border(BorderStroke(1.dp, JetSkyCyan.copy(alpha = 0.75f)), RoundedCornerShape(24.dp))
    ) {
      // Hero Route Trajectory Banner
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.FlightTakeoff,
              contentDescription = null,
              tint = JetDeepNavy,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = airline.uppercase(),
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 11.sp,
                  lineHeight = 16.sp,
                ),
              color = JetDeepNavy,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Origin ✈ Animated Dotted Curve ✈ Destination
          AnimatedFlightRouteHeader(
            originCode = originCode,
            originCity = originCity,
            destCode = destCode,
            destCity = destCity,
          )
        }
      }

      // Progressive Body Slot (Act 1 Shimmer -> Act 1 Flight Picker -> Act 2/3 Cabin Seat Map)
      if (!bodySlotId.isNullOrEmpty()) {
        val bodyState = observeA2uiComponentState(id = bodySlotId)
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
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
            label = "BodySlotTransition",
          ) { state ->
            when (state) {
              is A2uiComponentState.Loading -> FlightSlotShimmerSkeleton(isFooter = false)
              is A2uiComponentState.Error ->
                Text(
                  text = "Unable to load cabin module.",
                  color = JetTangelo,
                  style = MaterialTheme.typography.bodySmall,
                )
              is A2uiComponentState.Success -> A2uiComponent(state.component)
            }
          }
        }
      }

      // Progressive Footer Slot (Live Reactive Fare Summary Bar)
      if (!footerSlotId.isNullOrEmpty()) {
        val footerState = observeA2uiComponentState(id = footerSlotId)
        Box(
          modifier =
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
        ) {
          AnimatedContent(
            targetState = footerState,
            transitionSpec = {
              (fadeIn(animationSpec = tween(280)) + slideInVertically { it / 3 })
                .togetherWith(fadeOut(animationSpec = tween(150)))
                .using(SizeTransform(clip = false))
            },
            label = "FooterSlotTransition",
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

@Composable
internal fun AnimatedFlightRouteHeader(
  originCode: String,
  originCity: String,
  destCode: String,
  destCity: String,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "FlightArcTrajectory")
  val flightProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "FlightRouteProgress",
    )
  val flightPainter = rememberVectorPainter(image = Icons.Rounded.Flight)

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.32f)) {
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
        textAlign = TextAlign.Center,
      )
      Text(
        text = originCity,
        style =
          MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
          ),
        color = JetMutedSlate,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }

    Box(
      modifier = Modifier.weight(0.36f).height(46.dp).padding(horizontal = 4.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {
        val startX = 8.dp.toPx()
        val endX = size.width - 8.dp.toPx()
        val midX = size.width / 2f
        val baseY = size.height * 0.74f
        val controlY = -size.height * 0.14f

        val arcPath =
          Path().apply {
            moveTo(startX, baseY)
            quadraticTo(midX, controlY, endX, baseY)
          }
        drawPath(
          path = arcPath,
          color = JetDeepNavy.copy(alpha = 0.42f),
          style =
            Stroke(
              width = 1.8.dp.toPx(),
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f),
            ),
        )

        // Origin & Destination airport endpoints
        drawCircle(color = JetDeepNavy, radius = 3.dp.toPx(), center = Offset(startX, baseY))
        drawCircle(color = JetDeepNavy, radius = 3.dp.toPx(), center = Offset(endX, baseY))

        // Fade-in at origin (0..0.16), travel across arc, fade-out at destination (0.82..1.0)
        val t = flightProgress
        val markerAlpha =
          when {
            t < 0.16f -> (t / 0.16f).coerceIn(0f, 1f)
            t > 0.82f -> ((1f - t) / 0.18f).coerceIn(0f, 1f)
            else -> 1f
          }

        val oneMinusT = 1f - t
        val bx = oneMinusT * oneMinusT * startX + 2f * oneMinusT * t * midX + t * t * endX
        val by = oneMinusT * oneMinusT * baseY + 2f * oneMinusT * t * controlY + t * t * baseY

        // Tangent vector along quadratic Bezier curve so airplane banks along trajectory
        val dx = 2f * oneMinusT * (midX - startX) + 2f * t * (endX - midX)
        val dy = 2f * oneMinusT * (controlY - baseY) + 2f * t * (baseY - controlY)
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f

        val centerOffset = Offset(bx, by)
        val glowRadiusPx = 16.5.dp.toPx()
        val circleRadiusPx = 12.dp.toPx()
        val iconSizePx = 16.dp.toPx()

        
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  JetCoralOrange.copy(alpha = 0.9f * markerAlpha),
                  JetCoralOrange.copy(alpha = 0.55f * markerAlpha),
                  Color.Transparent,
                ),
              center = centerOffset,
              radius = glowRadiusPx,
            ),
          radius = glowRadiusPx,
          center = centerOffset,
        )
        drawCircle(
          color = JetCoralOrange.copy(alpha = 0.65f * markerAlpha),
          radius = 13.8.dp.toPx(),
          center = centerOffset,
        )

        // 24.dp white badge circle
        drawCircle(
          color = Color.White.copy(alpha = markerAlpha),
          radius = circleRadiusPx,
          center = centerOffset,
        )

        // 16.dp airplane icon inside the moving circle
        withTransform({
          translate(left = bx - iconSizePx / 2f, top = by - iconSizePx / 2f)
          rotate(degrees = angleDeg, pivot = Offset(iconSizePx / 2f, iconSizePx / 2f))
        }) {
          with(flightPainter) {
            draw(
              size = Size(iconSizePx, iconSizePx),
              alpha = markerAlpha,
              colorFilter = ColorFilter.tint(JetDeepNavy),
            )
          }
        }
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.32f)) {
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
        textAlign = TextAlign.Center,
      )
      Text(
        text = destCity,
        style =
          MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
          ),
        color = JetMutedSlate,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
internal fun FlightSlotShimmerSkeleton(isFooter: Boolean) {
  val transition = rememberInfiniteTransition(label = "SlotShimmer")
  val shimmerAlpha by
    transition.animateFloat(
      initialValue = 0.35f,
      targetValue = 0.85f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 850, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "ShimmerPulse",
    )

  if (isFooter) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(62.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(JetDeepNavy.copy(alpha = 0.12f * shimmerAlpha + 0.08f))
          .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier =
            Modifier.width(96.dp)
              .height(10.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(JetDeepNavy.copy(alpha = 0.2f * shimmerAlpha))
        )
        Box(
          modifier =
            Modifier.width(64.dp)
              .height(18.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(JetDeepNavy.copy(alpha = 0.28f * shimmerAlpha))
        )
      }
      Box(
        modifier =
          Modifier.width(124.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(JetSkyCyan.copy(alpha = 0.45f * shimmerAlpha))
      )
    }
  } else {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(Color.White.copy(alpha = 0.7f))
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier =
            Modifier.size(20.dp)
              .clip(CircleShape)
              .background(JetTangelo.copy(alpha = 0.35f * shimmerAlpha))
        )
        Box(
          modifier =
            Modifier.fillMaxWidth(0.75f)
              .height(12.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(JetDeepNavy.copy(alpha = 0.18f * shimmerAlpha))
        )
      }
      repeat(2) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(68.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(JetSkyCyan.copy(alpha = 0.22f * shimmerAlpha))
        )
      }
    }
  }
}

