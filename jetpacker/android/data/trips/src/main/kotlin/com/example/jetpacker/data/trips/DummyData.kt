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

package com.example.jetpacker.data.trips

import com.example.jetpacker.core.ui.R
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import java.time.Instant

object DummyData {

  private val dayMillis = 24 * 3600 * 1000L
  private val hourMillis = 3600 * 1000L
  private val romeBase = Instant.parse("2025-06-10T10:00:00Z").toEpochMilli()
  private val seoulBase = Instant.parse("2025-10-15T10:00:00Z").toEpochMilli()
  private val ioBaseTime = Instant.parse("2026-05-18T10:00:00Z").toEpochMilli()
  private val dublinBase = Instant.parse("2026-08-14T10:00:00Z").toEpochMilli()
  private val parisBase = Instant.parse("2026-09-20T10:00:00Z").toEpochMilli()

  val trips =
    listOf(
      Trip(
        "2025-1",
        "Roman Holiday",
        "Rome, Italy",
        romeBase,
        romeBase + 8 * dayMillis,
        R.drawable.img_rome,
        participants = listOf("Alice", "Bob", "Charlie"),
      ),
      Trip(
        "2025-2",
        "Scenic Seoul",
        "Seoul, South Korea",
        seoulBase,
        seoulBase + 7 * dayMillis,
        R.drawable.img_seoul,
        participants = listOf("David", "Eva"),
      ),
      Trip(
        "2026-1",
        "California I/O",
        "California, USA",
        ioBaseTime,
        ioBaseTime + 4 * dayMillis,
        R.drawable.img_california,
        participants = listOf("Frank", "Grace"),
      ),
      Trip(
        "2026-2",
        "Emerald Isle Weekend",
        "Dublin, Ireland",
        dublinBase,
        dublinBase + 2 * dayMillis,
        R.drawable.img_dublin_skyline,
        participants = listOf("Hank", "Ivy"),
      ),
      Trip(
        "2026-3",
        "Romantic Paris",
        "Paris, France",
        parisBase,
        parisBase + 7 * dayMillis,
        R.drawable.img_paris_skyline,
        participants = listOf("Jack", "Kelly", "Liam"),
      ),
    )

  val events = mutableListOf<TimelineEvent>()
  val tourDetails = mutableListOf<TourDetail>()

  val flightDetails = mutableListOf<FlightDetail>()
  val hotelDetails = mutableListOf<HotelDetail>()
  val diningDetails = mutableListOf<DiningDetail>()
  val activityDetails = mutableListOf<ActivityDetail>()
  val museumDetails = mutableListOf<MuseumDetail>()
  val expenses = mutableListOf<Expense>()
  val voiceNotes = mutableListOf<VoiceNoteEntity>()

  init {
    // 1. California work (2026-1)

    events.addAll(
      listOf(
        // Day 1: Flight in from Amsterdam, car rental pickup and hotel check-in
        TimelineEvent(
          id = "io_1",
          tripId = "2026-1",
          type = EventType.TRANSPORTATION,
          timestamp = ioBaseTime + 9 * hourMillis,
          title = "AMS > SFO",
          location = "SFO Airport",
          description = "Direct inbound international flight from Schiphol to San Francisco.",
        ),
        TimelineEvent(
          id = "io_2",
          tripId = "2026-1",
          type = EventType.CAR_RENTAL,
          timestamp = ioBaseTime + 11 * hourMillis,
          title = "Car pickup",
          location = "Zephyr Wheels Rental Center",
          description = "Picking up pre-booked premium sedan @ Zephyr Wheels.",
        ),
        TimelineEvent(
          id = "io_3",
          tripId = "2026-1",
          type = EventType.ACCOMMODATION,
          timestamp = ioBaseTime + 15 * hourMillis,
          title = "Check-in @ The Redwood Grove Hotel",
          location = "The Redwood Grove Hotel",
          language = "English",
          description = "Settling in and resting up before the bustling conference days begin.",
        ),
        // Day 2: Sightseeing, SFO boat trip and museum visit
        TimelineEvent(
          id = "io_4",
          tripId = "2026-1",
          type = EventType.ACTIVITY,
          timestamp = ioBaseTime + dayMillis + 10 * hourMillis,
          title = "SFO Boat Trip",
          location = "Pier 39, San Francisco",
          description = "Scenic bay area navigation straight under the Golden Gate bridge.",
        ),
        TimelineEvent(
          id = "io_5",
          tripId = "2026-1",
          type = EventType.CULTURE,
          timestamp = ioBaseTime + dayMillis + 14 * hourMillis,
          title = "SFMOMA Visit",
          location = "San Francisco Museum of Modern Art",
          description = "Exploring breathtaking modern and contemporary masterworks.",
        ),
        // Day 3: Keynotes, Lunch, Sessions, Dinner
        TimelineEvent(
          id = "io_6",
          tripId = "2026-1",
          type = EventType.WORK,
          timestamp = ioBaseTime + 2 * dayMillis + 9 * hourMillis,
          title = "Google I/O keynote sessions",
          location = "Shoreline Amphitheatre",
          description = "Main sundar's keynote covering all major Gemini and Android announcements.",
        ),
        TimelineEvent(
          id = "io_7",
          tripId = "2026-1",
          type = EventType.FOOD_AND_DRINK,
          timestamp = ioBaseTime + 2 * dayMillis + 13 * hourMillis,
          title = "Lunch with team",
          location = "Shoreline Amphitheatre",
          description = "Bonding over healthy conference catering options and catching up.",
        ),
        TimelineEvent(
          id = "io_8",
          tripId = "2026-1",
          type = EventType.WORK,
          timestamp = ioBaseTime + 2 * dayMillis + 14 * hourMillis,
          title = "Afternoon sessions",
          location = "Shoreline Amphitheatre",
          description = "Attending insightful mobile and GenAI integrations talks.",
        ),
        TimelineEvent(
          id = "io_9",
          tripId = "2026-1",
          type = EventType.FOOD_AND_DRINK,
          timestamp = ioBaseTime + 2 * dayMillis + 19 * hourMillis,
          title = "Dinner @ Quantum Bites",
          location = "Quantum Bites",
          description = "Celebrating a fruitful day with delicious, contemporary dining.",
        ),
        // Day 4: Sessions all day
        TimelineEvent(
          id = "io_10",
          tripId = "2026-1",
          type = EventType.WORK,
          timestamp = ioBaseTime + 3 * dayMillis + 9 * hourMillis,
          title = "Technical deep-dive sessions",
          location = "Shoreline Amphitheatre",
          description = "Advanced platform engineering and UI architecture workshops.",
        ),
        // Day 5: Hotel checkout, car rental return, flight out
        TimelineEvent(
          id = "io_11",
          tripId = "2026-1",
          type = EventType.ACCOMMODATION,
          timestamp = ioBaseTime + 4 * dayMillis + 9 * hourMillis,
          title = "Hotel Checkout",
          location = "The Redwood Grove Hotel",
          language = "English",
          description = "Packing and handing off room keys at the express checkout.",
        ),
        TimelineEvent(
          id = "io_12",
          tripId = "2026-1",
          type = EventType.CAR_RENTAL,
          timestamp = ioBaseTime + 4 * dayMillis + 11 * hourMillis,
          title = "Car Rental Return",
          location = "Zephyr Wheels Rental Center",
          description = "Returning the vehicle and signing off the rental agreement.",
        ),
        TimelineEvent(
          id = "io_13",
          tripId = "2026-1",
          type = EventType.TRANSPORTATION,
          timestamp = ioBaseTime + 4 * dayMillis + 14 * hourMillis,
          title = "SFO > AMS",
          location = "SFO Airport",
          description = "Heading home on an overnight outbound flight back to Amsterdam.",
        ),
      )
    )

    tourDetails.add(
      TourDetail(
        "io_detail_1",
        "io_6",
        "Google I/O Keynotes",
        "CONFERENCE",
        R.drawable.img_california,
        "Tuesday, May 19",
        "09:00 AM - 12:00 PM",
        "Shoreline Amphitheatre",
        "1 Amphitheatre Pkwy, Mountain View, CA 94043",
        null,
        "The main event of Google I/O where all the major announcements are made. Join Sundar and team to hear about the latest in AI, Android, and more.",
        "Main entrance of Shoreline Amphitheatre.",
        listOf("Arrive 1 hour early", "Bring your badge"),
      )
    )

    tourDetails.add(
      TourDetail(
        "io_detail_car",
        "io_2",
        "Zephyr Wheels Car Rental",
        "CAR_RENTAL",
        R.drawable.img_california,
        "Sunday, May 17",
        "11:00 AM",
        "Zephyr Wheels Rental Center",
        "SFO Airport Rental Car Center, San Francisco, CA",
        null,
        "Premium Sedan car pickup. Rental includes unlimited mileage, GPS navigation, and full insurance coverage.",
        "Main pickup desk at Zephyr Wheels Center.",
        listOf("Bring your valid driver license", "Credit card required for deposit"),
      )
    )

    tourDetails.addAll(
      listOf(
        TourDetail(
          "io_detail_boat",
          "io_4",
          "SFO Golden Gate Boat Trip",
          "ACTIVITY",
          R.drawable.img_california,
          "Monday, May 18",
          "10:00 AM",
          "Pier 39",
          "San Francisco, CA",
          null,
          "Enjoy a scenic pass around Alcatraz and straight under the magnificent Golden Gate Bridge. Fully guided boat experience.",
          "Main ticket kiosk at Pier 39.",
          listOf("Wear layers, can get windy", "Bring a camera"),
        ),
        TourDetail(
          "io_detail_sess",
          "io_8",
          "Advanced Tech & AI Sessions",
          "CONFERENCE",
          R.drawable.img_california,
          "Tuesday, May 19",
          "02:00 PM",
          "Google I/O Tents",
          "Shoreline Amphitheatre",
          null,
          "Deep dives into Gemini, Adk, and new LLM agent frameworks for mobile integrations.",
          "Main sessions tent entrance.",
          listOf("Bring your laptop", "Fully charged battery recommended"),
        ),
        TourDetail(
          "io_detail_check",
          "io_11",
          "The Redwood Grove - Checkout",
          "ACCOMMODATION",
          R.drawable.img_california,
          "Thursday, May 21",
          "09:00 AM",
          "Front Desk Express",
          "The Redwood Grove Hotel",
          null,
          "Finalizing all auxiliary room charges and handing off room credentials.",
          "Hotel lobby front desk.",
          listOf("Drop keys in express box"),
        ),
        TourDetail(
          "io_detail_ret",
          "io_12",
          "Zephyr Wheels - Car Return",
          "CAR_RENTAL",
          R.drawable.img_california,
          "Thursday, May 21",
          "11:00 AM",
          "Rental Drop Area",
          "Zephyr Center Drop-off, SFO",
          null,
          "Returning vehicle and finalizing mileage check.",
          "Main express return aisle.",
          listOf("Ensure fuel is topped off"),
        ),
        TourDetail(
          "par_detail_eiffel",
          "par_5",
          "Eiffel Tower Visit",
          "ACTIVITY",
          R.drawable.img_paris_skyline,
          "Monday, Dec 07",
          "10:00 AM",
          "Eiffel Tower Entrance",
          "Champ de Mars, 5 Av. Anatole France, 75007 Paris",
          null,
          "Ascend Gustave Eiffel's iconic, 324-meter wrought-iron tower. Offers magnificent, sweeping views across the Paris cityscape and the River Seine.",
          "Pillar South Ticket Office",
          listOf("Bring printed or digital tickets", "Arrive 30 mins prior for security"),
        ),
        TourDetail(
          "dub_detail_moher",
          "dub_4",
          "Cliffs of Moher Coastal Drive",
          "ACTIVITY",
          R.drawable.img_dublin_skyline,
          "Sunday, Sep 06",
          "10:00 AM",
          "Wild Atlantic Way",
          "Cliffs of Moher, Liscannor, Co. Clare, Ireland",
          null,
          "Experience the majestic Cliffs of Moher rising 700 feet over the Atlantic Ocean. An essential, scenic coastal drive along Ireland's stunning Wild Atlantic Way.",
          "Visitor Center Car Park",
          listOf("Wear comfortable walking shoes", "Prepare for windy coastal weather"),
        ),
        TourDetail(
          "rome_detail_walk",
          "rome_7",
          "Trevi Fountain & Pantheon Walk",
          "ACTIVITY",
          R.drawable.img_rome,
          "Wednesday, Jan 15",
          "10:00 AM",
          "Historic Rome Center",
          "Piazza della Rotonda, 00186 Roma",
          null,
          "Enjoy a guided morning stroll through Rome’s cobblestone alleys traversing past the magnificent Trevi Foundation directly to the historic Pantheon.",
          "Piazza della Rotonda Obelisk",
          listOf("Bring coins to make a wish at the fountain"),
        ),
        TourDetail(
          "rome_detail_tivoli",
          "rome_8",
          "Tivoli (Villa d'Este) Day Trip",
          "ACTIVITY",
          R.drawable.img_rome,
          "Thursday, Jan 16",
          "10:00 AM",
          "Villa d'Este Entrance",
          "Piazza Trento, 1, 00019 Tivoli RM",
          null,
          "Visit the enchanting 16th-century Villa d'Este famous for its hillside terraced Italian Renaissance garden and mesmerizing array of elegant fountains.",
          "Villa d'Este Ticket Office",
          listOf("Comfortable shoes highly recommended"),
        ),
      )
    )

    museumDetails.add(
      MuseumDetail(
        eventId = "io_5",
        description =
          "San Francisco Museum of Modern Art (SFMOMA) is a modern art museum in San Francisco, California.",
        address = "151 3rd St, San Francisco, CA 94103",
        openingHours = "Mon-Tue: 10AM-5PM, Thu: 1PM-8PM, Fri-Sun: 10AM-5PM",
        admissionPrice = "$25",
        ticketWebsite = "https://www.sfmoma.org/tickets/",
        rating = "4.6",
        phone = "+1 415 555 0142",
      )
    )

    // 2. Dublin (2026-2) - 3 days
    events.addAll(
      listOf(
        TimelineEvent(
          id = "dub_1",
          tripId = "2026-2",
          type = EventType.TRANSPORTATION,
          timestamp = dublinBase,
          title = "Flight to Dublin",
          location = "JFK Terminal 4",
          extraInfo = "EI 104",
        ),
        TimelineEvent(
          id = "dub_2",
          tripId = "2026-2",
          type = EventType.ACCOMMODATION,
          timestamp = dublinBase + 4 * hourMillis,
          title = "Check-in: The Shelbourne",
          location = "St Stephen's Green",
          language = "English",
        ),
        TimelineEvent(
          id = "dub_3",
          tripId = "2026-2",
          type = EventType.FOOD_AND_DRINK,
          timestamp = dublinBase + 10 * hourMillis,
          title = "Dinner at Fade Street Social",
          location = "Fade St",
        ),
        TimelineEvent(
          id = "dub_4",
          tripId = "2026-2",
          type = EventType.ACTIVITY,
          timestamp = dublinBase + 24 * hourMillis + 2 * hourMillis,
          title = "Day Tour: Cliffs of Moher",
          location = "Pickup from hotel",
        ),
        TimelineEvent(
          id = "dub_5",
          tripId = "2026-2",
          type = EventType.FOOD_AND_DRINK,
          timestamp = dublinBase + 24 * hourMillis + 10 * hourMillis,
          title = "Dinner at Temple Bar",
          location = "Temple Bar District",
        ),
        TimelineEvent(
          id = "dub_6",
          tripId = "2026-2",
          type = EventType.TRANSPORTATION,
          timestamp = dublinBase + 48 * hourMillis + 5 * hourMillis,
          title = "Flight Out",
          location = "Dublin Airport",
          extraInfo = "EI 105",
        ),
      )
    )

    flightDetails.addAll(
      listOf(
        FlightDetail(
          eventId = "dub_1",
          airline = "Aer Lingus",
          flightNum = "EI 104",
          origin = "JFK",
          destination = "DUB",
          gate = "B22",
          seat = "12A",
          departureTerminal = "Terminal 4",
          arrivalTerminal = "Terminal 2",
          arrivalGate = "408",
          duration = "6h 55m",
          aircraft = "Airbus A330-300",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "dub_6",
          airline = "Aer Lingus",
          flightNum = "EI 105",
          origin = "DUB",
          destination = "JFK",
          gate = "408",
          seat = "14C",
          departureTerminal = "Terminal 2",
          arrivalTerminal = "Terminal 4",
          arrivalGate = "B22",
          duration = "7h 15m",
          aircraft = "Airbus A330-300",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "io_1",
          airline = "United Airlines",
          flightNum = "UA 991",
          origin = "AMS",
          destination = "SFO",
          gate = "G22",
          seat = "32B",
          departureTerminal = "Terminal 2",
          arrivalTerminal = "International Terminal",
          arrivalGate = "G94",
          duration = "10h 25m",
          aircraft = "Boeing 777-300ER",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "io_13",
          airline = "United Airlines",
          flightNum = "UA 992",
          origin = "SFO",
          destination = "AMS",
          gate = "G94",
          seat = "34A",
          departureTerminal = "International Terminal",
          arrivalTerminal = "Terminal 2",
          arrivalGate = "G22",
          duration = "9h 55m",
          aircraft = "Boeing 777-300ER",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "rome_1",
          airline = "ITA Airways",
          flightNum = "AZ 611",
          origin = "JFK",
          destination = "FCO",
          gate = "A14",
          seat = "18C",
          departureTerminal = "Terminal 8",
          arrivalTerminal = "Terminal 3",
          arrivalGate = "D12",
          duration = "8h 30m",
          aircraft = "Airbus A350-900",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "rome_13",
          airline = "ITA Airways",
          flightNum = "AZ 610",
          origin = "FCO",
          destination = "JFK",
          gate = "D12",
          seat = "19A",
          departureTerminal = "Terminal 3",
          arrivalTerminal = "Terminal 8",
          arrivalGate = "A14",
          duration = "9h 10m",
          aircraft = "Airbus A350-900",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "seo_1",
          airline = "Korean Air",
          flightNum = "KE 82",
          origin = "JFK",
          destination = "ICN",
          gate = "B28",
          seat = "28H",
          departureTerminal = "Terminal 4",
          arrivalTerminal = "Terminal 2",
          arrivalGate = "242",
          duration = "14h 30m",
          aircraft = "Boeing 747-8I",
          baggageAllowance = "2 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "seo_10",
          airline = "Korean Air",
          flightNum = "KE 81",
          origin = "ICN",
          destination = "JFK",
          gate = "242",
          seat = "29C",
          departureTerminal = "Terminal 2",
          arrivalTerminal = "Terminal 4",
          arrivalGate = "B28",
          duration = "13h 45m",
          aircraft = "Boeing 747-8I",
          baggageAllowance = "2 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "par_1",
          airline = "Air France",
          flightNum = "AF 7",
          origin = "JFK",
          destination = "CDG",
          gate = "A22",
          seat = "24F",
          departureTerminal = "Terminal 8",
          arrivalTerminal = "Terminal 2E",
          arrivalGate = "M42",
          duration = "7h 25m",
          aircraft = "Boeing 777-300ER",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
        FlightDetail(
          eventId = "par_10",
          airline = "Air France",
          flightNum = "AF 8",
          origin = "CDG",
          destination = "JFK",
          gate = "M42",
          seat = "25E",
          departureTerminal = "Terminal 2E",
          arrivalTerminal = "Terminal 8",
          arrivalGate = "A22",
          duration = "8h 15m",
          aircraft = "Boeing 777-300ER",
          baggageAllowance = "1 x 23kg Checked, 1 Carry-on",
        ),
      )
    )

    hotelDetails.addAll(
      listOf(
        HotelDetail(
          eventId = "io_3",
          name = "The Redwood Grove Hotel",
          address = "123 Sequoia Ave, Mountain View, CA 94043",
          checkInTime = ioBaseTime + 15 * hourMillis,
          checkOutTime = ioBaseTime + 4 * dayMillis + 9 * hourMillis,
          confNumber = "RED-11223",
          rating = "4.7",
          ratingCount = "850",
          pricePerNight = "$250/night",
          guests = "1 Adult",
          phone = "+1 650 555 0134",
        ),
        HotelDetail(
          eventId = "dub_2",
          name = "The Shelbourne",
          address = "27 St Stephen's Green, Dublin",
          checkInTime = dublinBase + 4 * hourMillis,
          checkOutTime = dublinBase + 48 * hourMillis,
          confNumber = "SHEL-99882",
          rating = "4.8",
          ratingCount = "1.2k",
          pricePerNight = "$320/night",
          guests = "2 Adults",
          phone = "+353 20 911 1234",
        ),
        HotelDetail(
          eventId = "rome_2",
          name = "Hotel Artemide",
          address = "Via Nazionale, 22, 00184 Roma RM",
          checkInTime = romeBase + 10 * hourMillis,
          checkOutTime = romeBase + 192 * hourMillis + 5 * hourMillis,
          confNumber = "ART-33445",
          rating = "4.9",
          ratingCount = "2.1k",
          pricePerNight = "€180/night",
          guests = "2 Guests",
          phone = "+39 06 496 0123",
        ),
        HotelDetail(
          eventId = "seo_2",
          name = "Four Seasons Hotel",
          address = "Gwanghwamun, Seoul",
          checkInTime = seoulBase + 8 * hourMillis,
          checkOutTime = seoulBase + 168 * hourMillis + 5 * hourMillis,
          confNumber = "FSS-55667",
          rating = "4.8",
          ratingCount = "1.8k",
          pricePerNight = "₩450,000/night",
          guests = "2 Guests",
          phone = "+82 2 555 0123",
        ),
        HotelDetail(
          eventId = "par_2",
          name = "Hotel Le Meurice",
          address = "Rue de Rivoli, 75001 Paris",
          checkInTime = parisBase + 4 * hourMillis,
          checkOutTime = parisBase + 168 * hourMillis + 5 * hourMillis,
          confNumber = "LM-77889",
          rating = "4.9",
          ratingCount = "1.5k",
          pricePerNight = "€950/night",
          guests = "2 Adults",
          phone = "+33 1 99 00 12 34",
        ),
      )
    )

    diningDetails.addAll(
      listOf(
        DiningDetail(
          eventId = "dub_3",
          restaurantName = "Fade Street Social",
          address = "4-6 Fade St, Dublin",
          reservationTime = dublinBase + 10 * hourMillis,
          partySize = 2,
          rating = "4.5",
          reviewCount = "324",
          priceRange = "$$ - Moderate",
          phone = "+353 20 911 4321",
        ),
        DiningDetail(
          eventId = "dub_5",
          restaurantName = "The Temple Bar Pub",
          address = "47-48 Temple Bar, Dublin",
          reservationTime = dublinBase + 24 * hourMillis + 10 * hourMillis,
          partySize = 4,
          rating = "4.7",
          reviewCount = "1,280",
          priceRange = "$$ - Moderate",
          phone = "+353 20 911 8765",
        ),
        DiningDetail(
          eventId = "io_7",
          restaurantName = "Shoreline Cafe",
          address = "Shoreline Amphitheatre, Mountain View",
          reservationTime = ioBaseTime + 2 * dayMillis + 13 * hourMillis,
          partySize = 4,
          rating = "4.2",
          reviewCount = "150",
          priceRange = "$$ - Moderate",
          phone = "+1 650 555 0198",
        ),
        DiningDetail(
          eventId = "io_9",
          restaurantName = "Quantum Bites",
          address = "Quantum Bites Restaurant, Mountain View",
          reservationTime = ioBaseTime + 2 * dayMillis + 19 * hourMillis,
          partySize = 2,
          rating = "4.6",
          reviewCount = "89",
          priceRange = "$$$ - Expensive",
          phone = "+1 650 555 0199",
        ),
        DiningDetail(
          eventId = "rome_4",
          restaurantName = "Da Enzo al 29",
          address = "Via dei Vascellari, 29, 00153 Roma",
          reservationTime = romeBase + 24 * hourMillis + 10 * hourMillis,
          partySize = 2,
          rating = "4.8",
          reviewCount = "1,500",
          priceRange = "$ - Inexpensive",
          phone = "+39 06 581 2345",
        ),
        DiningDetail(
          eventId = "rome_6",
          restaurantName = "Pizzarium Bonci",
          address = "Via della Meloria, 43, 00136 Roma",
          reservationTime = romeBase + 48 * hourMillis + 5 * hourMillis,
          partySize = 1,
          rating = "4.7",
          reviewCount = "2,000",
          priceRange = "$ - Inexpensive",
          phone = "+39 06 3974 5416",
        ),
        DiningDetail(
          eventId = "rome_10",
          restaurantName = "Armando al Pantheon",
          address = "Salita de' Crescenzi, 31, 00186 Roma",
          reservationTime = romeBase + 120 * hourMillis + 10 * hourMillis,
          partySize = 2,
          rating = "4.6",
          reviewCount = "1,200",
          priceRange = "$$ - Moderate",
          phone = "+39 06 6880 3034",
        ),
        DiningDetail(
          eventId = "seo_3",
          restaurantName = "Jungsik",
          address = "11 Seolleung-ro 158-gil, Gangnam-gu, Seoul",
          reservationTime = seoulBase + 11 * hourMillis,
          partySize = 2,
          rating = "4.9",
          reviewCount = "600",
          priceRange = "$$$$ - Very Expensive",
          phone = "+82 2 517 4654",
        ),
        DiningDetail(
          eventId = "par_4",
          restaurantName = "Le Jules Verne",
          address = "Eiffel Tower, Avenue Gustave Eiffel, 75007 Paris",
          reservationTime = parisBase + 24 * hourMillis + 10 * hourMillis,
          partySize = 2,
          rating = "4.7",
          reviewCount = "1,800",
          priceRange = "$$$$ - Very Expensive",
          phone = "+33 1 45 55 61 44",
        ),
        DiningDetail(
          eventId = "par_9",
          restaurantName = "Pierre Hermé",
          address = "Le Marais, Paris",
          reservationTime = parisBase + 144 * hourMillis + 5 * hourMillis,
          partySize = 2,
          rating = "4.8",
          reviewCount = "2,500",
          priceRange = "$$ - Moderate",
          phone = "+33 1 43 54 47 77",
        ),
      )
    )

    activityDetails.add(
      ActivityDetail(
        eventId = "dub_4",
        activityName = "Cliffs of Moher Coastal Drive",
        address = "Wild Atlantic Way",
        durationMinutes = 480,
      )
    )

    // 3. Rome (2025-1) - 9 days
    events.addAll(
      listOf(
        TimelineEvent(
          id = "rome_1",
          tripId = "2025-1",
          type = EventType.TRANSPORTATION,
          timestamp = romeBase,
          title = "Flight to Rome (FCO)",
          location = "JFK Terminal 8",
        ),
        TimelineEvent(
          id = "rome_2",
          tripId = "2025-1",
          type = EventType.ACCOMMODATION,
          timestamp = romeBase + 10 * hourMillis,
          title = "Check-in: Hotel Artemide",
          location = "Via Nazionale",
          language = "Italian",
        ),
        TimelineEvent(
          id = "rome_3",
          tripId = "2025-1",
          type = EventType.CULTURE,
          timestamp = romeBase + 24 * hourMillis + 1 * hourMillis,
          title = "Colosseum & Roman Forum Tour",
          location = "Piazza del Colosseo",
          audioNotes = listOf("The underground area was fascinating to see."),
        ),
        TimelineEvent(
          id = "rome_4",
          tripId = "2025-1",
          type = EventType.FOOD_AND_DRINK,
          timestamp = romeBase + 24 * hourMillis + 10 * hourMillis,
          title = "Dinner at Da Enzo al 29",
          location = "Trastevere",
        ),
        TimelineEvent(
          id = "rome_5",
          tripId = "2025-1",
          type = EventType.CULTURE,
          timestamp = romeBase + 48 * hourMillis + 1 * hourMillis,
          title = "Vatican Museums & Sistine Chapel",
          location = "Vatican City",
        ),
        TimelineEvent(
          id = "rome_6",
          tripId = "2025-1",
          type = EventType.FOOD_AND_DRINK,
          timestamp = romeBase + 48 * hourMillis + 5 * hourMillis,
          title = "Lunch at Pizzarium Bonci",
          location = "Via della Meloria",
        ),
        TimelineEvent(
          id = "rome_7",
          tripId = "2025-1",
          type = EventType.ACTIVITY,
          timestamp = romeBase + 72 * hourMillis + 1 * hourMillis,
          title = "Trevi Fountain & Pantheon Walk",
          location = "Rome Center",
          audioNotes =
            listOf(
              "Found the best gelato place near Trevi Fountain with incredible pistachio flavor."
            ),
        ),
        TimelineEvent(
          id = "rome_8",
          tripId = "2025-1",
          type = EventType.ACTIVITY,
          timestamp = romeBase + 96 * hourMillis + 1 * hourMillis,
          title = "Day trip to Tivoli (Villa d'Este)",
          location = "Tivoli",
        ),
        TimelineEvent(
          id = "rome_9",
          tripId = "2025-1",
          type = EventType.CULTURE,
          timestamp = romeBase + 120 * hourMillis + 1 * hourMillis,
          title = "Borghese Gallery",
          location = "Villa Borghese",
        ),
        TimelineEvent(
          id = "rome_10",
          tripId = "2025-1",
          type = EventType.FOOD_AND_DRINK,
          timestamp = romeBase + 120 * hourMillis + 10 * hourMillis,
          title = "Dinner at Armando al Pantheon",
          location = "Salita de' Crescenzi",
        ),
        TimelineEvent(
          id = "rome_11",
          tripId = "2025-1",
          type = EventType.ACTIVITY,
          timestamp = romeBase + 144 * hourMillis + 1 * hourMillis,
          title = "Trastevere Neighborhood Walk",
          location = "Trastevere",
        ),
        TimelineEvent(
          id = "rome_12",
          tripId = "2025-1",
          type = EventType.ACTIVITY,
          timestamp = romeBase + 168 * hourMillis + 1 * hourMillis,
          title = "Appian Way Bike Tour",
          location = "Via Appia Antica",
        ),
        TimelineEvent(
          id = "rome_13",
          tripId = "2025-1",
          type = EventType.TRANSPORTATION,
          timestamp = romeBase + 192 * hourMillis + 5 * hourMillis,
          title = "Flight Out",
          location = "FCO Airport",
        ),
      )
    )

    // 4. Seoul (2025-2) - 8 days
    events.addAll(
      listOf(
        TimelineEvent(
          id = "seo_1",
          tripId = "2025-2",
          type = EventType.TRANSPORTATION,
          timestamp = seoulBase,
          title = "Flight to Seoul (ICN)",
          location = "JFK Terminal 4",
        ),
        TimelineEvent(
          id = "seo_2",
          tripId = "2025-2",
          type = EventType.ACCOMMODATION,
          timestamp = seoulBase + 8 * hourMillis,
          title = "Check-in: Four Seasons Hotel",
          location = "Gwanghwamun",
          language = "Korean",
        ),
        TimelineEvent(
          id = "seo_3",
          tripId = "2025-2",
          type = EventType.FOOD_AND_DRINK,
          timestamp = seoulBase + 11 * hourMillis,
          title = "Dinner at Jungsik",
          location = "Cheongdam-dong",
        ),
        TimelineEvent(
          id = "seo_4",
          tripId = "2025-2",
          type = EventType.CULTURE,
          timestamp = seoulBase + 24 * hourMillis + 1 * hourMillis,
          title = "Gyeongbokgung Palace Tour",
          location = "Jongno-gu",
        ),
        TimelineEvent(
          id = "seo_5",
          tripId = "2025-2",
          type = EventType.ACTIVITY,
          timestamp = seoulBase + 48 * hourMillis + 1 * hourMillis,
          title = "N Seoul Tower & Bukchon",
          location = "Namsan",
        ),
        TimelineEvent(
          id = "seo_6",
          tripId = "2025-2",
          type = EventType.ACTIVITY,
          timestamp = seoulBase + 72 * hourMillis + 1 * hourMillis,
          title = "Myeongdong Shopping",
          location = "Myeongdong",
        ),
        TimelineEvent(
          id = "seo_7",
          tripId = "2025-2",
          type = EventType.ACTIVITY,
          timestamp = seoulBase + 96 * hourMillis + 1 * hourMillis,
          title = "DMZ Day Tour",
          location = "DMZ",
        ),
        TimelineEvent(
          id = "seo_8",
          tripId = "2025-2",
          type = EventType.ACTIVITY,
          timestamp = seoulBase + 120 * hourMillis + 1 * hourMillis,
          title = "Dongdaemun Design Plaza",
          location = "Eulji-ro",
        ),
        TimelineEvent(
          id = "seo_9",
          tripId = "2025-2",
          type = EventType.ACTIVITY,
          timestamp = seoulBase + 144 * hourMillis + 1 * hourMillis,
          title = "Gangnam District Explore",
          location = "Gangnam",
        ),
        TimelineEvent(
          id = "seo_10",
          tripId = "2025-2",
          type = EventType.TRANSPORTATION,
          timestamp = seoulBase + 168 * hourMillis + 5 * hourMillis,
          title = "Flight Out",
          location = "ICN Airport",
        ),
      )
    )

    // 5. Paris (2026-3) - 8 days
    events.addAll(
      listOf(
        TimelineEvent(
          id = "par_1",
          tripId = "2026-3",
          type = EventType.TRANSPORTATION,
          timestamp = parisBase,
          title = "Flight to Paris (CDG)",
          location = "JFK Terminal 8",
        ),
        TimelineEvent(
          id = "par_2",
          tripId = "2026-3",
          type = EventType.ACCOMMODATION,
          timestamp = parisBase + 4 * hourMillis,
          title = "Check-in: Hotel Le Meurice",
          location = "Rue de Rivoli",
          language = "French",
        ),
        TimelineEvent(
          id = "par_3",
          tripId = "2026-3",
          type = EventType.CULTURE,
          timestamp = parisBase + 24 * hourMillis + 1 * hourMillis,
          title = "Visit to the Louvre",
          location = "Rue de Rivoli",
        ),
        TimelineEvent(
          id = "par_4",
          tripId = "2026-3",
          type = EventType.FOOD_AND_DRINK,
          timestamp = parisBase + 24 * hourMillis + 10 * hourMillis,
          title = "Dinner at Le Jules Verne",
          location = "Eiffel Tower",
          placeId = "ChIJl_7p8uFv5kcRj5ZGEf31ILM",
        ),
        TimelineEvent(
          id = "par_5",
          tripId = "2026-3",
          type = EventType.ACTIVITY,
          timestamp = parisBase + 48 * hourMillis + 1 * hourMillis,
          title = "Eiffel Tower Visit",
          location = "Champ de Mars",
        ),
        TimelineEvent(
          id = "par_6",
          tripId = "2026-3",
          type = EventType.CULTURE,
          timestamp = parisBase + 72 * hourMillis + 1 * hourMillis,
          title = "Versailles Palace Tour",
          location = "Versailles",
        ),
        TimelineEvent(
          id = "par_7",
          tripId = "2026-3",
          type = EventType.ACTIVITY,
          timestamp = parisBase + 96 * hourMillis + 1 * hourMillis,
          title = "Montmartre Walk",
          location = "Sacre-Coeur",
        ),
        TimelineEvent(
          id = "par_8",
          tripId = "2026-3",
          type = EventType.ACTIVITY,
          timestamp = parisBase + 120 * hourMillis + 1 * hourMillis,
          title = "Seine River Cruise",
          location = "Pont de l Alma",
        ),
        TimelineEvent(
          id = "par_9",
          tripId = "2026-3",
          type = EventType.FOOD_AND_DRINK,
          timestamp = parisBase + 144 * hourMillis + 5 * hourMillis,
          title = "Pastry and Macaron Tasting",
          location = "Le Marais",
        ),
        TimelineEvent(
          id = "par_10",
          tripId = "2026-3",
          type = EventType.TRANSPORTATION,
          timestamp = parisBase + 168 * hourMillis + 5 * hourMillis,
          title = "Flight Out",
          location = "CDG Airport",
        ),
      )
    )
    museumDetails.add(
      MuseumDetail(
        eventId = "par_3",
        description =
          "The Louvre, or the Louvre Museum, is the world's most-visited museum and a historic monument in Paris, France.",
        address = "Rue de Rivoli, 75001 Paris, France",
        openingHours = "Mon, Wed, Thu, Sat, Sun: 9AM–6PM, Fri: 9AM–9:45PM, Tue: Closed",
        admissionPrice = "€17",
        ticketWebsite = "https://www.louvre.fr/en",
        rating = "4.7",
        phone = "+33 1 99 00 56 78",
        infoUrls = listOf("https://www.louvre.fr/en/visit/faq"),
      )
    )

    expenses.addAll(
      listOf(
        Expense(
          title = "SFO Taxi",
          amount = 45.0,
          currency = "USD",
          category = "Transport",
          tripId = "2026-1",
          timestamp = 1000L,
        ),
        Expense(
          title = "SFMOMA Ticket",
          amount = 25.0,
          currency = "USD",
          category = "Activity",
          tripId = "2026-1",
          timestamp = 2000L,
        ),
        Expense(
          title = "Dinner @ Quantum Bites",
          amount = 120.0,
          currency = "USD",
          category = "Food",
          tripId = "2026-1",
          timestamp = 3000L,
        ),
        Expense(
          title = "Colosseum Ticket",
          amount = 16.0,
          currency = "EUR",
          category = "Culture",
          tripId = "2025-1",
          timestamp = romeBase + 24 * hourMillis + 1 * hourMillis,
        ),
        Expense(
          title = "Gelato",
          amount = 5.5,
          currency = "EUR",
          category = "Food",
          tripId = "2025-1",
          timestamp = romeBase + 30 * hourMillis,
        ),
        Expense(
          title = "Myeongdong Street Food",
          amount = 15000.0,
          currency = "KRW",
          category = "Food",
          tripId = "2025-2",
          timestamp = seoulBase + 72 * hourMillis + 1 * hourMillis,
        ),
        Expense(
          title = "N Seoul Tower",
          amount = 12000.0,
          currency = "KRW",
          category = "Activity",
          tripId = "2025-2",
          timestamp = seoulBase + 24 * hourMillis + 1 * hourMillis,
        ),
      )
    )

    voiceNotes.addAll(
      listOf(
        VoiceNoteEntity(
          id = "rome_vn_1",
          tripId = "2025-1",
          title = "Colosseum Visit",
          transcription =
            "The Colosseum was amazing. We saw the underground area and it was fascinating.",
          timestamp = romeBase + 24 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "rome_vn_2",
          tripId = "2025-1",
          title = "Gelato near Trevi",
          transcription =
            "Found the best gelato place near Trevi Fountain. Pistachio flavor was incredible.",
          timestamp = romeBase + 48 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "rome_vn_3",
          tripId = "2025-1",
          title = "Vatican Museums",
          transcription = "The Sistine Chapel was breathtaking. So crowded though, but worth it.",
          timestamp = romeBase + 72 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "rome_vn_4",
          tripId = "2025-1",
          title = "Pantheon",
          transcription =
            "The dome of the Pantheon is a marvel. Light coming through the oculus was beautiful.",
          timestamp = romeBase + 96 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "rome_vn_5",
          tripId = "2025-1",
          title = "Trastevere Dinner",
          transcription =
            "Had amazing pasta in Trastevere. The atmosphere there at night is so lively.",
          timestamp = romeBase + 120 * hourMillis + 19 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "rome_vn_6",
          tripId = "2025-1",
          title = "Borghese Gallery",
          transcription =
            "Bernini sculptures at Borghese are unbelievable. Apollo and Daphne looked so real.",
          timestamp = romeBase + 144 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "rome_vn_7",
          tripId = "2025-1",
          title = "Appian Way Bike",
          transcription =
            "Biking on the ancient Appian Way was a bit bumpy but a great experience.",
          timestamp = romeBase + 168 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "seoul_vn_1",
          tripId = "2025-2",
          title = "Gyeongbokgung Palace",
          transcription = "The changing of the guard ceremony was cool. Palace grounds are huge.",
          timestamp = seoulBase + 24 * hourMillis + 10 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "seoul_vn_2",
          tripId = "2025-2",
          title = "N Seoul Tower View",
          transcription = "Great view of the city from N Seoul Tower. Sunset was beautiful.",
          timestamp = seoulBase + 48 * hourMillis + 18 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "seoul_vn_3",
          tripId = "2025-2",
          title = "Bukchon Hanok Village",
          transcription =
            "Walking through the traditional houses was nice. Very quiet and peaceful.",
          timestamp = seoulBase + 72 * hourMillis + 11 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "seoul_vn_4",
          tripId = "2025-2",
          title = "Myeongdong Street Food",
          transcription = "Tried so many street foods in Myeongdong. The egg bread was delicious.",
          timestamp = seoulBase + 96 * hourMillis + 19 * hourMillis,
          matchingEventsJson = "[]",
        ),
        VoiceNoteEntity(
          id = "seoul_vn_5",
          tripId = "2025-2",
          title = "Gangnam District",
          transcription =
            "Gangnam is so modern and bustling. Lots of shopping and high-tech stuff.",
          timestamp = seoulBase + 120 * hourMillis + 15 * hourMillis,
          matchingEventsJson = "[]",
        ),
      )
    )
  }
}
