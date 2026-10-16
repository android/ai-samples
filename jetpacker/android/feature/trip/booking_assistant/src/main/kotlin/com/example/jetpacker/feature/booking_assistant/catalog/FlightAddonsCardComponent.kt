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
 * Act 2 & Act 3 Hybrid Add-ons Card: Showcases the Hybrid Catalog architecture by hosting standard
 * `MaterialA2uiBasicCatalogV1Defaults.checkBox` child components (`childrenProp`) inside a styled
 * JetPacker perks card, while also supporting direct two-way binding to `priorityBoarding` and
 * `extraBag`.
 */
class FlightAddonsCardComponent : A2uiComponent {
  private val titleProp =
    A2uiProperty.dynamicString(
      key = "title",
      required = false,
      description = "Title for the flight add-ons section.",
    )
  private val priorityBoardingProp =
    A2uiProperty.dynamicBoolean(
      key = "priorityBoarding",
      required = false,
      description = "Two-way bound Priority Boarding toggle (+$25).",
    )
  private val extraBagProp =
    A2uiProperty.dynamicBoolean(
      key = "extraBag",
      required = false,
      description = "Two-way bound Extra Checked Bag toggle (+$45).",
    )
  private val childrenProp =
    A2uiProperty.childList(
      key = "children",
      required = false,
      description = "Child Material 3 A2UI components (e.g. CheckBox controls).",
    )

  override val name: String = "FlightAddonsCard"
  override val description: String =
    "Hybrid container for flight add-on perks embedding standard Material 3 A2UI CheckBox controls."
  override val properties: List<A2uiProperty<*>> =
    listOf(titleProp, priorityBoardingProp, extraBagProp, childrenProp)

  @Composable
  override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean = true

  @Composable
  override fun A2uiComponentScope.Content(properties: A2uiComponentProperties, modifier: Modifier) {
    val title = properties.bind(titleProp) ?: "TRAVEL PERKS & UPGRADES"
    val priorityBoarding = properties.bind(priorityBoardingProp) ?: false
    val extraBag = properties.bind(extraBagProp) ?: false
    val updatePriority = properties.bindUpdater(priorityBoardingProp)
    val updateBag = properties.bindUpdater(extraBagProp)
    val childRefs = properties.bindChildReferences(childrenProp).orEmpty()

    Column(
      modifier =
        modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(Color.White)
          .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Luggage,
          contentDescription = null,
          tint = JetRoyalCobalt,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = title.uppercase(),
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

      if (childRefs.isNotEmpty()) {
        // Render standard Material 3 A2UI CheckBox components streamed from the server
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          childRefs.forEachIndexed { idx, ref ->
            val isActive = if (idx == 0) priorityBoarding else extraBag
            val badgeText = if (idx == 0) "+$25" else "+$45"
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (isActive) JetCobaltSoft else JetWarmCream.copy(alpha = 0.65f))
                  .border(
                    BorderStroke(
                      1.dp,
                      if (isActive) JetRoyalCobalt.copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                    ),
                    RoundedCornerShape(14.dp),
                  )
                  .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                key(ref.id, ref.baseDataPath) {
                  when (val state = observeA2uiComponentState(id = ref.id)) {
                    is A2uiComponentState.Loading -> FlightSlotShimmerSkeleton(isFooter = true)
                    is A2uiComponentState.Error ->
                      Text("Perk unavailable", style = MaterialTheme.typography.bodySmall)
                    is A2uiComponentState.Success -> A2uiComponent(state.component)
                  }
                }
              }
              Box(
                modifier =
                  Modifier.background(
                      if (isActive) JetRoyalCobalt else Color.White,
                      RoundedCornerShape(50),
                    )
                    .border(
                      BorderStroke(1.dp, JetRoyalCobalt.copy(alpha = 0.3f)),
                      RoundedCornerShape(50),
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
              ) {
                Text(
                  text = badgeText,
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp,
                    ),
                  color = if (isActive) Color.White else JetDeepNavy,
                )
              }
            }
          }
        }
      } else {
        // Fallback interactive perk toggles if no child CheckBox references were streamed
        PerkToggleRow(
          label = "Zone 1 Priority Boarding & FastTrack",
          priceLabel = "+$25",
          checked = priorityBoarding,
          onCheckedChange = { updatePriority?.invoke(it) },
        )
        PerkToggleRow(
          label = "Extra 23kg Checked Bag Allowance",
          priceLabel = "+$45",
          checked = extraBag,
          onCheckedChange = { updateBag?.invoke(it) },
        )
      }
    }
  }
}

@Composable
private fun PerkToggleRow(
  label: String,
  priceLabel: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(if (checked) JetCobaltSoft else JetWarmCream.copy(alpha = 0.65f))
        .border(
          BorderStroke(1.dp, if (checked) JetRoyalCobalt else Color(0xFFE2E8F0)),
          RoundedCornerShape(14.dp),
        )
        .clickable { onCheckedChange(!checked) }
        .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      modifier = Modifier.weight(1f).padding(end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = checked,
        onCheckedChange = null,
        colors = CheckboxDefaults.colors(checkedColor = JetRoyalCobalt, uncheckedColor = JetSlate),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = label,
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
          ),
        color = JetDeepNavy,
      )
    }
    Box(
      modifier =
        Modifier.background(if (checked) JetRoyalCobalt else Color.White, RoundedCornerShape(50))
          .border(BorderStroke(1.dp, JetRoyalCobalt.copy(alpha = 0.3f)), RoundedCornerShape(50))
          .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
      Text(
        text = priceLabel,
        style =
          MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
        color = if (checked) Color.White else JetDeepNavy,
      )
    }
  }
}

