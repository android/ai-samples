from __future__ import annotations

import asyncio
import json
import re
from typing import Any, AsyncGenerator

from google.adk.agents.invocation_context import InvocationContext
from google.adk.agents.parallel_agent import ParallelAgent
from google.adk.events.event import Event
from google.genai import types

from agents.base import (
    BOOKING_ASSISTANT_CATALOG_ID,
    PROGRESSIVE_SKELETON_DELAY_SEC,
    BaseSimulationAgent,
    _get_genai_client,
    sessions,
)
from schemas import HotelConfigProposal, HotelRoomOptionSchema

def _build_fallback_hotel_config(
    item: dict[str, Any], full_itinerary: list[dict[str, Any]]
) -> dict[str, Any]:
  """Builds route- and trip-accurate baseline hotel metadata from itinerary data."""
  title = item.get("title", "Hotel")
  hotel_detail = item.get("hotelDetail") or {}
  raw_loc = (
      hotel_detail.get("address")
      or item.get("tripLocation")
      or item.get("location")
      or ""
  ).strip()
  if not raw_loc or raw_loc.lower() == title.lower():
    location = item.get("tripLocation") or "Mountain View, CA"
  else:
    location = raw_loc

  rating = hotel_detail.get("rating") or "4.8"
  location_label = f"{location} • ★ {rating}"

  check_in_date = (
      hotel_detail.get("checkInDate")
      or (item.get("dateLabel") or "OCT 27").upper()
  )
  check_out_date = hotel_detail.get("checkOutDate") or "OCT 31"
  nights = int(hotel_detail.get("nights") or 4)

  # Parse baseline rate from hotelDetail.pricePerNight if present
  raw_price = str(hotel_detail.get("pricePerNight") or "240")
  digits = re.findall(r"\d+", raw_price.replace(",", ""))
  base_rate = int(digits[0]) if digits else 240
  if base_rate > 2000:
    # Convert KRW/JPY-style numbers into USD-equivalent nightly integer for card display
    base_rate = 260

  conf_code = hotel_detail.get("confNumber") or "RG-8842"
  venues = [
      ev.get("title", "")
      for ev in full_itinerary
      if ev.get("type") not in ("ACCOMMODATION", "TRANSPORTATION")
      and ev.get("title")
  ]
  venue_hint = venues[0] if venues else "your itinerary activities"

  suites = [
      {
          "id": "suite_1",
          "name": (
              f"{title.replace('The ', '').split()[0]} Signature King Suite"
          ),
          "amenities": "Private Balcony • Ergonomic Studio Desk • Espresso Bar",
          "rate": base_rate,
          "badge": "POPULAR • BALCONY VIEW",
      },
      {
          "id": "suite_2",
          "name": "Garden Courtyard Studio",
          "amenities": "Courtyard View • Rainfall Shower • Quiet Wing",
          "rate": max(140, base_rate - 45),
          "badge": "QUIET RETREAT",
      },
      {
          "id": "suite_3",
          "name": "Skyline Executive Loft",
          "amenities": (
              "Top-Floor Panoramic View • Deep Soaking Tub • Lounge Access"
          ),
          "rate": base_rate + 70,
          "badge": "VIP UPGRADE",
      },
  ]

  return {
      "hotelName": title,
      "locationLabel": location_label,
      "checkInDate": check_in_date,
      "checkOutDate": check_out_date,
      "nights": nights,
      "rationale": (
          f"Well-situated for {venue_hint}, with quiet rooms and flexible"
          " check-in."
      ),
      "liveUpdateAlert": (
          "Front Desk Live Update: Complimentary welcome drink & early"
          f" check-in waiver unlocked at {title}!"
      ),
      "roomNumber": "SUITE 418",
      "confirmationCode": conf_code,
      "suites": suites,
  }



class SingleHotelSimulationAgent(BaseSimulationAgent):
  name: str = "single_hotel"
  item_title: str = "Hotel"
  item_data: dict[str, Any] = {}
  full_itinerary: list[dict[str, Any]] = []

  async def _generate_hotel_config(
      self, fallback: dict[str, Any]
  ) -> dict[str, Any]:
    try:
      client = _get_genai_client()
      prompt = (
          "You are a luxury hotel inventory engine generating an A2UI v0.9"
          " hotel configurator payload.\n"
          f"Hotel event item: {json.dumps(self.item_data)}\n"
          f"Full trip itinerary context: {json.dumps(self.full_itinerary)}\n"
          f"Baseline hotel defaults: {json.dumps(fallback)}\n"
          "Return a realistic HotelConfigProposal JSON tailored to this exact"
          " hotel, city, and trip itinerary:\n"
          "- Keep `checkInDate`, `checkOutDate`, and `nights` consistent with"
          " the baseline hotel defaults.\n"
          "- `rationale`: 1 concise sentence explaining why this property fits"
          " the traveler's itinerary venues (never mention 'AI' or 'A2UI').\n"
          "- `suites`: Provide 3 distinct room/suite tiers in this hotel with"
          " authentic names, 3 bulleted amenities separated by ' • ', integer"
          " USD nightly `rate` (between 150 and 950), and a short room-specific"
          " `badge` (e.g. 'POPULAR • BALCONY VIEW', 'QUIET RETREAT', 'VIP"
          " UPGRADE' — never put distance to venue on a room badge since all"
          " rooms are in the same hotel, and never mention 'AI' or 'A2UI')."
      )
      response = await asyncio.to_thread(
          client.models.generate_content,
          model="gemini-2.5-flash",
          contents=prompt,
          config=types.GenerateContentConfig(
              response_mime_type="application/json",
              response_schema=HotelConfigProposal,
              temperature=0.3,
          ),
      )
      if response.text:
        parsed = json.loads(response.text)
        if not parsed.get("suites"):
          parsed["suites"] = fallback["suites"]
        for idx, s in enumerate(parsed["suites"]):
          s["id"] = f"suite_{idx + 1}"
        parsed["nights"] = int(parsed.get("nights") or fallback["nights"])
        parsed["checkInDate"] = (
            parsed.get("checkInDate") or fallback["checkInDate"]
        )
        parsed["checkOutDate"] = (
            parsed.get("checkOutDate") or fallback["checkOutDate"]
        )
        print(
            f"INFO: Gemini generated hotel config for {self.item_title}:"
            f" {[s.get('name') for s in parsed['suites']]}"
        )
        return parsed
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"WARN: Gemini hotel config fallback used for {self.item_title}: {e}"
      )
    return fallback

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title
    baseline = _build_fallback_hotel_config(self.item_data, self.full_itinerary)
    base_first_suite = baseline["suites"][0]

    # =========================================================================
    # STEP 1: Progressive HotelConfiguratorShell + HotelRoomSelector
    # =========================================================================
    if session.completed_steps.get(title, 0) < 1:
      initial_hotel_dm = {
          "hotel": {
              "hotelName": title,
              "location": baseline["locationLabel"],
              "checkInDate": baseline["checkInDate"],
              "checkOutDate": baseline["checkOutDate"],
              "nights": baseline["nights"],
              "statusBadge": "SCANNING SUITES",
              "aiReason": baseline["rationale"],
              "optionStrings": [],
              "livePatchMessage": "",
          },
          "selection": {
              "roomId": base_first_suite["id"],
              "roomName": base_first_suite["name"],
              "nightlyRate": base_first_suite["rate"],
              "checkInTime": "11:00 AM Early",
              "floorPreference": "High Floor • Quiet",
              "breakfastIncluded": False,
              "lateCheckout": False,
          },
      }

      # Emit progressive shell immediately so skeleton shimmer renders while Gemini runs
      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "createSurface": {
                      "surfaceId": title,
                      "catalogId": BOOKING_ASSISTANT_CATALOG_ID,
                      "sendDataModel": True,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/",
                      "value": initial_hotel_dm,
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "root",
                          "component": "HotelConfiguratorShell",
                          "hotelName": {"path": "/hotel/hotelName"},
                          "location": {"path": "/hotel/location"},
                          "checkInDate": {"path": "/hotel/checkInDate"},
                          "checkOutDate": {"path": "/hotel/checkOutDate"},
                          "nights": {"path": "/hotel/nights"},
                          "statusBadge": {"path": "/hotel/statusBadge"},
                          "bodySlot": "hotel_room_selector",
                      }],
                  },
              },
          ],
      )

      # Call Gemini 2.5 Flash to generate hotel suites & itinerary rationale
      config = await self._generate_hotel_config(baseline)
      suite_options = config["suites"]
      first_suite = suite_options[0]
      option_strings = [
          f"{opt['id']}|{opt['name']}|{opt['amenities']}|{opt['rate']}|{opt['badge']}"
          for opt in suite_options
      ]

      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/hotel",
                      "value": {
                          "hotelName": title,
                          "location": config.get(
                              "locationLabel", baseline["locationLabel"]
                          ),
                          "checkInDate": config["checkInDate"],
                          "checkOutDate": config["checkOutDate"],
                          "nights": config["nights"],
                          "statusBadge": "SUITES READY",
                          "aiReason": config.get(
                              "rationale", baseline["rationale"]
                          ),
                          "optionStrings": option_strings,
                          "livePatchMessage": "",
                      },
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/roomId",
                      "value": first_suite["id"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/roomName",
                      "value": first_suite["name"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/nightlyRate",
                      "value": first_suite["rate"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "hotel_room_selector",
                          "component": "HotelRoomSelector",
                          "aiReason": {"path": "/hotel/aiReason"},
                          "options": {"path": "/hotel/optionStrings"},
                          "selectedRoomId": {"path": "/selection/roomId"},
                          "selectedRoomName": {"path": "/selection/roomName"},
                          "nightlyRate": {"path": "/selection/nightlyRate"},
                          "confirmBtnText": "Customize Stay & Check-In",
                          "action": {
                              "event": {
                                  "name": "select_hotel_room",
                                  "context": {
                                      "agentId": title,
                                      "selectionType": "hotel_room",
                                  },
                              }
                          },
                      }],
                  },
              },
          ],
      )

      if title not in session.pause_events:
        session.pause_events[title] = asyncio.Event()
      else:
        session.pause_events[title].clear()

      await session.pause_events[title].wait()
      session.completed_steps[title] = 1
    else:
      config = await self._generate_hotel_config(baseline)
      suite_options = config["suites"]
      first_suite = suite_options[0]

    # Resolve selected suite from Step 1
    step1_resp = session.results.get(title)
    chosen_suite = first_suite
    if isinstance(step1_resp, dict):
      sel_room_id = step1_resp.get("selectedRoomId")
      sel_opt = step1_resp.get("selectedOption") or step1_resp.get("time") or ""
      for opt in suite_options:
        if (sel_room_id and opt["id"] == sel_room_id) or (
            sel_opt and opt["name"] in str(sel_opt)
        ):
          chosen_suite = opt
          break

    # =========================================================================
    # STEP 2: HotelStayCustomizer + Reactive Packages + Live Front-Desk Patch
    # =========================================================================
    if session.completed_steps.get(title, 0) < 2:
      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/roomName",
                      "value": chosen_suite["name"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/nightlyRate",
                      "value": chosen_suite["rate"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/hotel/statusBadge",
                      "value": "CUSTOMIZE STAY",
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [
                          {
                              "id": "root",
                              "component": "HotelConfiguratorShell",
                              "hotelName": {"path": "/hotel/hotelName"},
                              "location": {"path": "/hotel/location"},
                              "checkInDate": {"path": "/hotel/checkInDate"},
                              "checkOutDate": {"path": "/hotel/checkOutDate"},
                              "nights": {"path": "/hotel/nights"},
                              "statusBadge": {"path": "/hotel/statusBadge"},
                              "bodySlot": "hotel_stay_customizer",
                              "footerSlot": "hotel_fare_footer",
                          },
                          {
                              "id": "hotel_stay_customizer",
                              "component": "HotelStayCustomizer",
                              "selectedRoomName": {
                                  "path": "/selection/roomName"
                              },
                              "checkInTime": {"path": "/selection/checkInTime"},
                              "floorPreference": {
                                  "path": "/selection/floorPreference"
                              },
                              "liveAlert": {"path": "/hotel/livePatchMessage"},
                              "children": [
                                  "hotel_breakfast_cb",
                                  "hotel_checkout_cb",
                              ],
                          },
                          {
                              "id": "hotel_breakfast_cb",
                              "component": "CheckBox",
                              "label": (
                                  "Artisan Breakfast & Espresso Bar (+$28/nt)"
                              ),
                              "value": {"path": "/selection/breakfastIncluded"},
                          },
                          {
                              "id": "hotel_checkout_cb",
                              "component": "CheckBox",
                              "label": (
                                  "Guaranteed 4:00 PM Late Checkout (+$45)"
                              ),
                              "value": {"path": "/selection/lateCheckout"},
                          },
                          {
                              "id": "hotel_fare_footer",
                              "component": "HotelLiveSummaryFooter",
                              "nightlyRate": {"path": "/selection/nightlyRate"},
                              "nights": {"path": "/hotel/nights"},
                              "checkInTime": {"path": "/selection/checkInTime"},
                              "floorPreference": {
                                  "path": "/selection/floorPreference"
                              },
                              "breakfastIncluded": {
                                  "path": "/selection/breakfastIncluded"
                              },
                              "lateCheckout": {
                                  "path": "/selection/lateCheckout"
                              },
                              "confirmBtnText": "Confirm Suite & Issue Key",
                              "action": {
                                  "event": {
                                      "name": "confirm_hotel_config",
                                      "context": {
                                          "agentId": title,
                                          "selectionType": (
                                              "confirm_hotel_config"
                                          ),
                                      },
                                  }
                              },
                          },
                      ],
                  },
              },
          ],
      )

      session.pause_events[title].clear()
      try:
        await asyncio.wait_for(
            session.pause_events[title].wait(),
            timeout=2.8,
        )
      except asyncio.TimeoutError:
        yield self._create_a2ui_event(
            ctx,
            author=title,
            messages=[{
                "version": "v0.9",
                "updateDataModel": {
                    "surfaceId": title,
                    "path": "/hotel/livePatchMessage",
                    "value": config.get(
                        "liveUpdateAlert", baseline["liveUpdateAlert"]
                    ),
                },
            }],
        )
        await session.pause_events[title].wait()

      # =======================================================================
      # STEP 3: DigitalHotelKeyPass Morph
      # =======================================================================
      step2_resp = session.results.get(title)
      check_in_time = "11:00 AM Early"
      floor_pref = "High Floor • Quiet"
      breakfast = False
      late_checkout = False
      nightly_rate = int(chosen_suite["rate"])

      if isinstance(step2_resp, dict):
        client_dm = step2_resp.get("clientDataModel") or {}
        if isinstance(client_dm, dict) and "a2uiClientDataModel" in client_dm:
          client_dm = client_dm["a2uiClientDataModel"].get(
              "v0.9", client_dm["a2uiClientDataModel"]
          )
        surfaces_dm = (
            client_dm.get("surfaces", client_dm)
            if isinstance(client_dm, dict)
            else {}
        )
        surface_dm = (
            surfaces_dm.get(title, {}) if isinstance(surfaces_dm, dict) else {}
        )
        selection_dm = (
            surface_dm.get("selection", {})
            if isinstance(surface_dm, dict)
            else {}
        )
        check_in_time = (
            selection_dm.get("checkInTime")
            or step2_resp.get("checkInTime")
            or "11:00 AM Early"
        )
        floor_pref = (
            selection_dm.get("floorPreference")
            or step2_resp.get("floorPreference")
            or "High Floor • Quiet"
        )
        breakfast = bool(
            selection_dm.get(
                "breakfastIncluded", step2_resp.get("breakfastIncluded", False)
            )
        )
        late_checkout = bool(
            selection_dm.get(
                "lateCheckout", step2_resp.get("lateCheckout", False)
            )
        )
        nightly_rate = int(
            selection_dm.get("nightlyRate", chosen_suite["rate"])
        )

      nights = int(config.get("nights") or baseline["nights"])
      computed_total = (
          (nightly_rate * nights)
          + (28 * nights if breakfast else 0)
          + (45 if late_checkout else 0)
      )
      total_stay = (
          int(step2_resp["totalFare"])
          if isinstance(step2_resp, dict) and step2_resp.get("totalFare")
          else computed_total
      )
      room_number = config.get("roomNumber") or baseline["roomNumber"]
      conf_code = config.get("confirmationCode") or baseline["confirmationCode"]

      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/checkInTime",
                      "value": check_in_time,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/floorPreference",
                      "value": floor_pref,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/breakfastIncluded",
                      "value": breakfast,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/lateCheckout",
                      "value": late_checkout,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/hotel/totalStayPrice",
                      "value": total_stay,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/hotel/roomNumber",
                      "value": room_number,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/hotel/confirmationCode",
                      "value": conf_code,
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "root",
                          "component": "DigitalHotelKeyPass",
                          "hotelName": {"path": "/hotel/hotelName"},
                          "roomName": {"path": "/selection/roomName"},
                          "roomNumber": {"path": "/hotel/roomNumber"},
                          "floorPreference": {
                              "path": "/selection/floorPreference"
                          },
                          "checkInDate": {"path": "/hotel/checkInDate"},
                          "checkOutDate": {"path": "/hotel/checkOutDate"},
                          "checkInTime": {"path": "/selection/checkInTime"},
                          "confirmationCode": {
                              "path": "/hotel/confirmationCode"
                          },
                          "totalStayPrice": {"path": "/hotel/totalStayPrice"},
                          "breakfastIncluded": {
                              "path": "/selection/breakfastIncluded"
                          },
                          "lateCheckout": {"path": "/selection/lateCheckout"},
                      }],
                  },
              },
          ],
      )
      yield self._create_end_event(ctx, author=title)
      await asyncio.sleep(0.4)
      session.completed_steps[title] = 2


class HotelSimulationAgent(BaseSimulationAgent):
  name: str = "hotel"
  items: list[dict[str, Any]] = []
  full_itinerary: list[dict[str, Any]] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleHotelSimulationAgent(
            name=f"single_hotel_{i}",
            item_title=item.get("title", "Hotel"),
            item_data=item,
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(name="hotels_parallel", sub_agents=sub_agents)

    async for event in parallel_agent.run_async(ctx):
      yield event

