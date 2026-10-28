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

package com.example.jetpacker.feature.booking_assistant

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import com.example.jetpacker.feature.booking_assistant.catalog.AircraftCabinSeatMapComponent
import com.example.jetpacker.feature.booking_assistant.catalog.BookingConfirmationComponent
import com.example.jetpacker.feature.booking_assistant.catalog.BookingStatusComponent
import com.example.jetpacker.feature.booking_assistant.catalog.DigitalHotelKeyPassComponent
import com.example.jetpacker.feature.booking_assistant.catalog.FlightAddonsCardComponent
import com.example.jetpacker.feature.booking_assistant.catalog.FlightConfiguratorShellComponent
import com.example.jetpacker.feature.booking_assistant.catalog.FlightOptionSelectorComponent
import com.example.jetpacker.feature.booking_assistant.catalog.HotelConfiguratorShellComponent
import com.example.jetpacker.feature.booking_assistant.catalog.HotelLiveSummaryFooterComponent
import com.example.jetpacker.feature.booking_assistant.catalog.HotelRoomSelectorComponent
import com.example.jetpacker.feature.booking_assistant.catalog.HotelStayCustomizerComponent
import com.example.jetpacker.feature.booking_assistant.catalog.InteractiveOptionPickerComponent
import com.example.jetpacker.feature.booking_assistant.catalog.LiveFareSummaryFooterComponent
import com.example.jetpacker.feature.booking_assistant.catalog.PerforatedBoardingPassComponent
import com.example.jetpacker.feature.booking_assistant.catalog.SeatSelectionPickerComponent

const val BOOKING_ASSISTANT_CATALOG_ID: String =
  "https://example.com/catalogs/booking_assistant/v1/catalog.json"

/**
 * Returns the hybrid A2UI catalog for JetPacker's Booking Assistant, combining standard Material 3
 * A2UI components ([MaterialA2uiBasicCatalogV1Defaults]) with custom domain-specific travel
 * components.
 */
fun bookingAssistantCatalog(): A2uiCatalog {
  return A2uiCatalog(
    catalogId = BOOKING_ASSISTANT_CATALOG_ID,
    components =
      listOf(
        // Standard Material 3 A2UI basic catalog components (Hybrid Catalog showcase)
        MaterialA2uiBasicCatalogV1Defaults.card,
        MaterialA2uiBasicCatalogV1Defaults.column,
        MaterialA2uiBasicCatalogV1Defaults.row,
        MaterialA2uiBasicCatalogV1Defaults.text,
        MaterialA2uiBasicCatalogV1Defaults.checkBox,
        MaterialA2uiBasicCatalogV1Defaults.divider,
        // Hero Agentic Flight Configurator custom components
        FlightConfiguratorShellComponent(),
        FlightOptionSelectorComponent(),
        AircraftCabinSeatMapComponent(),
        FlightAddonsCardComponent(),
        LiveFareSummaryFooterComponent(),
        PerforatedBoardingPassComponent(),
        // Multi-Step Boutique Hotel Configurator custom components
        HotelConfiguratorShellComponent(),
        HotelRoomSelectorComponent(),
        HotelStayCustomizerComponent(),
        HotelLiveSummaryFooterComponent(),
        DigitalHotelKeyPassComponent(),
        // Supporting itinerary components (Hotels, Activities, Dining, Fallback)
        InteractiveOptionPickerComponent(),
        SeatSelectionPickerComponent(),
        BookingStatusComponent(),
        BookingConfirmationComponent(),
      ),
  )
}
