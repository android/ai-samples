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
    LIVE_SEAT_PATCH_DELAY_SEC,
    PROGRESSIVE_SKELETON_DELAY_SEC,
    BaseSimulationAgent,
    _get_genai_client,
    _resolve_airport,
    sessions,
)
from schemas import FlightConfigProposal, FlightOptionSchema

def _build_fallback_flight_config(
    item: dict[str, Any], full_itinerary: list[dict[str, Any]]
) -> dict[str, Any]:
  """Builds a deterministic, route-accurate flight configuration for any trip."""
  title = item.get("title", "Flight")
  location = item.get("location", "")
  extra_info = item.get("extraInfo", "")
  flight_detail = item.get("flightDetail") or {}

  fd_origin = flight_detail.get("origin", "")
  fd_dest = flight_detail.get("destination", "")
  fd_airline = flight_detail.get("airline", "")
  fd_flight_num = flight_detail.get("flightNum", "")
  fd_aircraft = flight_detail.get("aircraft", "")
  fd_duration = flight_detail.get("duration", "")
  fd_gate = flight_detail.get("gate", "")
  fd_terminal = flight_detail.get("departureTerminal", "")

  is_return = "return" in title.lower() or "home" in title.lower()

  # Infer origin and destination from flightDetail, location, or title
  if fd_origin and fd_dest:
    origin_code, origin_city = _resolve_airport(fd_origin, "AMS", fd_origin)
    dest_code, dest_city = _resolve_airport(fd_dest, "SFO", fd_dest)
  elif "->" in location or "→" in location or " to " in location.lower():
    parts = re.split(r"->|→|\bto\b", location, maxsplit=1, flags=re.IGNORECASE)
    origin_code, origin_city = _resolve_airport(
        parts[0].strip(), "AMS", "Amsterdam"
    )
    dest_code, dest_city = _resolve_airport(
        parts[1].strip() if len(parts) > 1 else title, "SFO", "San Francisco"
    )
  else:
    target_code, target_city = _resolve_airport(
        f"{title} {location}", "SFO", "San Francisco"
    )
    if is_return and target_code == "AMS":
      # Find the outbound destination from earlier in the itinerary
      outbound_city = "San Francisco"
      outbound_code = "SFO"
      for other in full_itinerary:
        if other.get("title") != title:
          c_code, c_city = _resolve_airport(
              f"{other.get('title', '')} {other.get('location', '')}", "", ""
          )
          if c_code and c_code != "AMS":
            outbound_code, outbound_city = c_code, c_city
            break
      origin_code, origin_city = outbound_code, outbound_city
      dest_code, dest_city = "AMS", "Amsterdam"
    else:
      origin_code, origin_city = "AMS", "Amsterdam"
      dest_code, dest_city = target_code, target_city
      if origin_code == dest_code:
        origin_code, origin_city = "SFO", "San Francisco"

  # Parse airline & flight number from extraInfo if not in flightDetail
  airline = fd_airline
  flight_num = fd_flight_num
  if not flight_num and extra_info:
    fn_match = re.search(r"\b([A-Z]{2}\s?\d{2,4})\b", extra_info)
    if fn_match:
      flight_num = fn_match.group(1)

  route_defaults: dict[str, tuple[str, str, str, str, int]] = {
      "SFO": ("KLM Royal Dutch", "KL 605", "Airbus A350-900", "11h 15m", 640),
      "NRT": (
          "ANA All Nippon",
          "NH 212",
          "Boeing 787-9 Dreamliner",
          "11h 45m",
          780,
      ),
      "HND": ("ANA All Nippon", "NH 204", "Boeing 777-300ER", "11h 35m", 790),
      "CDG": ("Air France", "AF 1641", "Airbus A220-300", "1h 25m", 185),
      "DUB": ("Aer Lingus", "EI 605", "Airbus A320neo", "1h 40m", 165),
      "FCO": ("ITA Airways", "AZ 109", "Airbus A321neo", "2h 15m", 220),
      "ICN": (
          "Korean Air",
          "KE 926",
          "Boeing 787-9 Dreamliner",
          "11h 20m",
          740,
      ),
  }
  lookup_key = dest_code if dest_code != "AMS" else origin_code
  def_airline, def_fn, def_aircraft, def_dur, def_fare = route_defaults.get(
      lookup_key,
      ("KLM Royal Dutch", "KL 601", "Airbus A350-900", "3h 30m", 320),
  )

  airline = airline or def_airline
  flight_num = flight_num or def_fn
  aircraft = fd_aircraft or def_aircraft
  duration = fd_duration or def_dur
  base_fare = def_fare
  gate = fd_gate or "D42"
  terminal = fd_terminal or "T2"

  prefix = "".join(ch for ch in flight_num if ch.isalpha()) or "KL"
  num_str = "".join(ch for ch in flight_num if ch.isdigit()) or "605"
  base_num = int(num_str)

  options = [
      {
          "id": "opt_1",
          "flightNumber": f"{prefix} {base_num}",
          "departureTime": "09:50 AM",
          "arrivalTime": "12:05 PM",
          "duration": duration,
          "fare": base_fare,
          "badge": "TOP PICK • NON-STOP",
          "aircraft": aircraft,
      },
      {
          "id": "opt_2",
          "flightNumber": f"{prefix} {base_num + 4}",
          "departureTime": "01:25 PM",
          "arrivalTime": "03:40 PM",
          "duration": duration,
          "fare": base_fare + 55,
          "badge": "AFTERNOON DIRECT",
          "aircraft": aircraft,
      },
      {
          "id": "opt_3",
          "flightNumber": f"{prefix} {base_num + 12}",
          "departureTime": "06:15 PM",
          "arrivalTime": "08:30 PM",
          "duration": duration,
          "fare": max(120, base_fare - 30),
          "badge": "EVENING VALUE",
          "aircraft": aircraft,
      },
  ]

  return {
      "originCode": origin_code,
      "originCity": origin_city,
      "destCode": dest_code,
      "destCity": dest_city,
      "airline": airline,
      "flightNumber": flight_num,
      "aircraftName": aircraft,
      "duration": duration,
      "baseFare": base_fare,
      "gate": gate,
      "terminal": terminal,
      "boardingGroup": "ZONE 2",
      "rationale": (
          f"Optimal nonstop {origin_code} → {dest_code} schedule matched to"
          f" your {dest_city} itinerary."
      ),
      "occupiedSeats": ["1B", "1C", "2B", "3C", "4A", "4D"],
      "contestedSeat": "2C",
      "options": options,
  }



class SingleFlightSimulationAgent(BaseSimulationAgent):
  name: str = "single_flight"
  item_title: str = "Flight"
  item_data: dict[str, Any] = {}
  full_itinerary: list[dict[str, Any]] = []

  async def _generate_flight_config(
      self, fallback: dict[str, Any]
  ) -> dict[str, Any]:
    try:
      client = _get_genai_client()
      prompt = (
          "You are a travel concierge and airline inventory engine generating"
          " an A2UI v0.9 flight configurator payload.\n"
          f"Flight event item: {json.dumps(self.item_data)}\n"
          f"Full trip itinerary context: {json.dumps(self.full_itinerary)}\n"
          f"Baseline route defaults: {json.dumps(fallback)}\n"
          "Return a realistic FlightConfigProposal JSON matching the exact"
          " route, airline, and trip dates/activities.\n"
          "- `rationale`: 1 concise sentence explaining how the recommended"
          " departure aligns with the traveler's trip schedule (never mention"
          " 'AI' or 'A2UI').\n"
          "- `options`: Provide 3 distinct flight options on this route with"
          " realistic flight numbers, times, aircraft, fares, and short badges"
          " (e.g. 'BEST SCHEDULE', 'MORNING EXPRESS', 'EVENING VALUE' — never"
          " mention 'AI' or 'A2UI').\n"
          "- Keep `occupiedSeats` as a subset of ['1B', '1C', '2B', '3C', '4A',"
          " '4D'] (never mark '1A' or '3A' occupied so the user can pick them)"
          " and set `contestedSeat` to '2C' (not in occupiedSeats)."
      )
      response = await asyncio.to_thread(
          client.models.generate_content,
          model="gemini-2.5-flash",
          contents=prompt,
          config=types.GenerateContentConfig(
              response_mime_type="application/json",
              response_schema=FlightConfigProposal,
              temperature=0.3,
          ),
      )
      if response.text:
        parsed = json.loads(response.text)
        occ = [
            s
            for s in parsed.get("occupiedSeats", fallback["occupiedSeats"])
            if s not in ("1A", "3A", "2C")
        ]
        parsed["occupiedSeats"] = occ or ["1B", "1C", "2B", "3C", "4D"]
        parsed["contestedSeat"] = "2C"
        if not parsed.get("options"):
          parsed["options"] = fallback["options"]
        print(
            f"INFO: Gemini generated flight config for {self.item_title}:"
            f" {parsed.get('originCode')} -> {parsed.get('destCode')} ("
            f"{len(parsed.get('options', []))} options)"
        )
        return parsed
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"WARN: Gemini flight config fallback used for {self.item_title}: {e}"
      )
    return fallback

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title
    baseline = _build_fallback_flight_config(
        self.item_data, self.full_itinerary
    )
    base_first_opt = baseline["options"][0]

    # =========================================================================
    # ACT 1: Progressive Streaming & Partial Readiness (FlightConfiguratorShell)
    # =========================================================================
    if session.completed_steps.get(title, 0) < 1:
      initial_data_model = {
          "flight": {
              "originCode": baseline["originCode"],
              "originCity": baseline["originCity"],
              "destCode": baseline["destCode"],
              "destCity": baseline["destCity"],
              "airline": (
                  f"{baseline['airline']} • {base_first_opt['flightNumber']}"
              ),
              "flightNumber": base_first_opt["flightNumber"],
              "departureTime": base_first_opt["departureTime"],
              "arrivalTime": base_first_opt["arrivalTime"],
              "duration": base_first_opt["duration"],
              "aircraftName": base_first_opt.get(
                  "aircraft", baseline["aircraftName"]
              ),
              "baseFare": base_first_opt["fare"],
              "gate": baseline["gate"],
              "terminal": baseline["terminal"],
              "boardingGroup": baseline["boardingGroup"],
              "statusBadge": "SCANNING LIVE FARES",
              "aiReason": baseline["rationale"],
              "options": baseline["options"],
              "optionStrings": [],
          },
          "cabin": {
              "occupiedSeats": baseline["occupiedSeats"],
              "livePatchedSeat": "",
              "livePatchMessage": "",
          },
          "selection": {
              "flightId": base_first_opt["flightNumber"],
              "departureTime": (
                  f"{base_first_opt['departureTime']} –"
                  f" {base_first_opt['arrivalTime']}"
              ),
              "seat": "1A",
              "seatExtraPrice": 85,
              "priorityBoarding": False,
              "extraBag": False,
          },
      }

      # 1a. Emit createSurface (sendDataModel=True) + initial data model +
      #     FlightConfiguratorShell referencing unresolved child 'flight_selector'
      #     so the skeleton shimmer renders while Gemini generates options.
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
                      "value": initial_data_model,
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "root",
                          "component": "FlightConfiguratorShell",
                          "airline": {"path": "/flight/airline"},
                          "originCode": {"path": "/flight/originCode"},
                          "originCity": {"path": "/flight/originCity"},
                          "destCode": {"path": "/flight/destCode"},
                          "destCity": {"path": "/flight/destCity"},
                          "statusBadge": {"path": "/flight/statusBadge"},
                          "bodySlot": "flight_selector",
                      }],
                  },
              },
          ],
      )

      # Generate live flight options & rationale via Gemini 2.5 Flash while shimmer shows
      config = await self._generate_flight_config(baseline)
      options = config["options"]
      first_opt = options[0]
      option_strings = [
          f"{opt['flightNumber']}|{opt['departureTime']} –"
          f" {opt['arrivalTime']}|{opt.get('aircraft', config['aircraftName'])}"
          f" • {opt['duration']}|{opt['fare']}|{opt['badge']}"
          for opt in options
      ]
      ai_reason = config.get("rationale") or baseline["rationale"]

      # 1b. Stream the Gemini-generated data model & resolved 'flight_selector'
      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight",
                      "value": {
                          "originCode": config["originCode"],
                          "originCity": config["originCity"],
                          "destCode": config["destCode"],
                          "destCity": config["destCity"],
                          "airline": (
                              f"{config['airline']} •"
                              f" {first_opt['flightNumber']}"
                          ),
                          "flightNumber": first_opt["flightNumber"],
                          "departureTime": first_opt["departureTime"],
                          "arrivalTime": first_opt["arrivalTime"],
                          "duration": first_opt["duration"],
                          "aircraftName": first_opt.get(
                              "aircraft", config["aircraftName"]
                          ),
                          "baseFare": first_opt["fare"],
                          "gate": config["gate"],
                          "terminal": config["terminal"],
                          "boardingGroup": config["boardingGroup"],
                          "statusBadge": "OPTIONS READY",
                          "aiReason": ai_reason,
                          "options": options,
                          "optionStrings": option_strings,
                      },
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/flightId",
                      "value": first_opt["flightNumber"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/departureTime",
                      "value": (
                          f"{first_opt['departureTime']} –"
                          f" {first_opt['arrivalTime']}"
                      ),
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "flight_selector",
                          "component": "FlightOptionSelector",
                          "aiReason": {"path": "/flight/aiReason"},
                          "options": {"path": "/flight/optionStrings"},
                          "selectedFlightId": {"path": "/selection/flightId"},
                          "selectedFlightTime": {
                              "path": "/selection/departureTime"
                          },
                          "baseFare": {"path": "/flight/baseFare"},
                          "confirmBtnText": "Continue to Cabin & Seats",
                          "action": {
                              "event": {
                                  "name": "select_flight",
                                  "context": {
                                      "agentId": title,
                                      "selectionType": "time",
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
      config = await self._generate_flight_config(baseline)
      options = config["options"]
      first_opt = options[0]

    # Resolve which flight option the user picked in Act 1
    act1_resp = session.results.get(title)
    chosen_opt = first_opt
    if isinstance(act1_resp, dict):
      sel_id = act1_resp.get("selectedFlightId")
      sel_time = act1_resp.get("time", "")
      for opt in options:
        if (
            (sel_id and opt["flightNumber"] == sel_id)
            or (sel_id and opt["id"] == sel_id)
            or (sel_time and opt["flightNumber"] in str(sel_time))
            or (sel_time and opt["departureTime"] in str(sel_time))
        ):
          chosen_opt = opt
          break
    elif isinstance(act1_resp, str):
      for opt in options:
        if (
            opt["flightNumber"] in act1_resp
            or opt["departureTime"] in act1_resp
        ):
          chosen_opt = opt
          break

    # =========================================================================
    # ACT 2: Hybrid Catalogs + Custom Compose Canvas Fuselage + Reactive Binding
    # =========================================================================
    if session.completed_steps.get(title, 0) < 2:
      # 2a. First update FlightConfiguratorShell to point to unresolved
      #     'cabin_experiential_col' and 'fare_footer' so multi-slot shimmer renders
      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/airline",
                      "value": (
                          f"{config['airline']} • {chosen_opt['flightNumber']}"
                      ),
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/flightNumber",
                      "value": chosen_opt["flightNumber"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/departureTime",
                      "value": chosen_opt["departureTime"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/arrivalTime",
                      "value": chosen_opt["arrivalTime"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/baseFare",
                      "value": chosen_opt["fare"],
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/statusBadge",
                      "value": "LOADING CABIN...",
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "root",
                          "component": "FlightConfiguratorShell",
                          "airline": {"path": "/flight/airline"},
                          "originCode": {"path": "/flight/originCode"},
                          "originCity": {"path": "/flight/originCity"},
                          "destCode": {"path": "/flight/destCode"},
                          "destCity": {"path": "/flight/destCity"},
                          "statusBadge": {"path": "/flight/statusBadge"},
                          "bodySlot": "cabin_experiential_col",
                          "footerSlot": "fare_footer",
                      }],
                  },
              },
          ],
      )

      await asyncio.sleep(PROGRESSIVE_SKELETON_DELAY_SEC * 0.75)

      # 2b. Stream the Hybrid Catalog components:
      #     - Standard M3 Column ('cabin_experiential_col')
      #     - Custom Compose Canvas Fuselage ('AircraftCabinSeatMap')
      #     - Custom 'FlightAddonsCard' wrapping Standard M3 CheckBox children
      #     - Custom 'LiveFareSummaryFooter' bound to the same data model paths
      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/statusBadge",
                      "value": "CABIN READY",
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [
                          {
                              "id": "cabin_experiential_col",
                              "component": "Column",
                              "children": ["seat_map_canvas", "addons_card"],
                          },
                          {
                              "id": "seat_map_canvas",
                              "component": "AircraftCabinSeatMap",
                              "aircraft": {"path": "/flight/aircraftName"},
                              "occupiedSeats": {"path": "/cabin/occupiedSeats"},
                              "liveAlert": {"path": "/cabin/livePatchMessage"},
                              "selectedSeat": {"path": "/selection/seat"},
                              "seatUpcharge": {
                                  "path": "/selection/seatExtraPrice"
                              },
                          },
                          {
                              "id": "addons_card",
                              "component": "FlightAddonsCard",
                              "title": "TRAVEL PERKS & UPGRADES",
                              "priorityBoarding": {
                                  "path": "/selection/priorityBoarding"
                              },
                              "extraBag": {"path": "/selection/extraBag"},
                              "children": [
                                  "addon_priority_cb",
                                  "addon_bag_cb",
                              ],
                          },
                          {
                              "id": "addon_priority_cb",
                              "component": "CheckBox",
                              "label": "Zone 1 Priority Boarding & FastTrack",
                              "value": {"path": "/selection/priorityBoarding"},
                          },
                          {
                              "id": "addon_bag_cb",
                              "component": "CheckBox",
                              "label": "Extra 23kg Checked Bag",
                              "value": {"path": "/selection/extraBag"},
                          },
                          {
                              "id": "fare_footer",
                              "component": "LiveFareSummaryFooter",
                              "baseFare": {"path": "/flight/baseFare"},
                              "selectedSeat": {"path": "/selection/seat"},
                              "seatUpcharge": {
                                  "path": "/selection/seatExtraPrice"
                              },
                              "priorityBoarding": {
                                  "path": "/selection/priorityBoarding"
                              },
                              "extraBag": {"path": "/selection/extraBag"},
                              "confirmBtnText": "Confirm Seat & Lock Fare",
                              "action": {
                                  "event": {
                                      "name": "confirm_flight_config",
                                      "context": {
                                          "agentId": title,
                                          "selectionType": (
                                              "confirm_flight_config"
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

      # =======================================================================
      # ACT 3: Live Surgical Server Data Patch (mid-selection updateDataModel)
      # =======================================================================
      session.pause_events[title].clear()
      try:
        await asyncio.wait_for(
            session.pause_events[title].wait(),
            timeout=LIVE_SEAT_PATCH_DELAY_SEC,
        )
      except asyncio.TimeoutError:
        contested_seat = config.get("contestedSeat", "2C")
        patched_occupied = list(
            dict.fromkeys(config["occupiedSeats"] + [contested_seat])
        )
        yield self._create_a2ui_event(
            ctx,
            author=title,
            messages=[
                {
                    "version": "v0.9",
                    "updateDataModel": {
                        "surfaceId": title,
                        "path": "/cabin/occupiedSeats",
                        "value": patched_occupied,
                    },
                },
                {
                    "version": "v0.9",
                    "updateDataModel": {
                        "surfaceId": title,
                        "path": "/cabin/livePatchedSeat",
                        "value": contested_seat,
                    },
                },
                {
                    "version": "v0.9",
                    "updateDataModel": {
                        "surfaceId": title,
                        "path": "/cabin/livePatchMessage",
                        "value": (
                            f"Seat {contested_seat} just booked by another"
                            " traveler"
                        ),
                    },
                },
            ],
        )
        await session.pause_events[title].wait()

      # =======================================================================
      # ACT 4: Synchronized clientDataModel & Perforated Boarding Pass Morph
      # =======================================================================
      act3_resp = session.results.get(title)
      selected_seat = "1A"
      seat_extra = 85
      priority_boarding = False
      extra_bag = False
      base_fare = int(chosen_opt["fare"])

      if isinstance(act3_resp, dict):
        client_dm = act3_resp.get("clientDataModel") or {}
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
        flight_dm = (
            surface_dm.get("flight", {}) if isinstance(surface_dm, dict) else {}
        )

        selected_seat = (
            selection_dm.get("seat")
            or act3_resp.get("selectedSeat")
            or act3_resp.get("seat")
            or "1A"
        )
        if selected_seat.startswith(("1", "2")):
          default_seat_extra = 85
        elif selected_seat.startswith("3"):
          default_seat_extra = 35
        else:
          default_seat_extra = 0

        seat_extra = int(selection_dm.get("seatExtraPrice", default_seat_extra))
        priority_boarding = bool(
            selection_dm.get(
                "priorityBoarding", act3_resp.get("priorityBoarding", False)
            )
        )
        extra_bag = bool(
            selection_dm.get("extraBag", act3_resp.get("extraBag", False))
        )
        base_fare = int(flight_dm.get("baseFare", base_fare))
      elif isinstance(act3_resp, str) and act3_resp:
        selected_seat = act3_resp
        seat_extra = (
            85
            if selected_seat.startswith(("1", "2"))
            else (35 if selected_seat.startswith("3") else 0)
        )

      computed_total = (
          base_fare
          + seat_extra
          + (25 if priority_boarding else 0)
          + (45 if extra_bag else 0)
      )
      if isinstance(act3_resp, dict) and act3_resp.get("totalFare"):
        total_fare = int(act3_resp["totalFare"])
      else:
        total_fare = computed_total

      is_business = selected_seat.startswith(("1", "2"))
      boarding_group = (
          "ZONE 1"
          if (priority_boarding or is_business)
          else config["boardingGroup"]
      )
      pnr_suffix = "".join(ch for ch in config["destCode"] if ch.isalpha())[:2]
      carrier_code = (
          "".join(
              ch for ch in chosen_opt.get("flightNumber", "") if ch.isalpha()
          )[:2]
          or "JP"
      )
      flight_time_range = (
          f"{chosen_opt['departureTime']} – {chosen_opt['arrivalTime']}"
      )

      yield self._create_a2ui_event(
          ctx,
          author=title,
          messages=[
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/seat",
                      "value": selected_seat,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/priorityBoarding",
                      "value": priority_boarding,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/selection/extraBag",
                      "value": extra_bag,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/totalFare",
                      "value": total_fare,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/flightTimeRange",
                      "value": flight_time_range,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/boardingGroup",
                      "value": boarding_group,
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/confirmationCode",
                      "value": f"{carrier_code}-{pnr_suffix}9",
                  },
              },
              {
                  "version": "v0.9",
                  "updateDataModel": {
                      "surfaceId": title,
                      "path": "/flight/statusBadge",
                      "value": "BOARDING PASS ISSUED",
                  },
              },
              {
                  "version": "v0.9",
                  "updateComponents": {
                      "surfaceId": title,
                      "components": [{
                          "id": "root",
                          "component": "PerforatedBoardingPass",
                          "airline": {"path": "/flight/airline"},
                          "originCode": {"path": "/flight/originCode"},
                          "originCity": {"path": "/flight/originCity"},
                          "destCode": {"path": "/flight/destCode"},
                          "destCity": {"path": "/flight/destCity"},
                          "flightTime": {"path": "/flight/flightTimeRange"},
                          "seat": {"path": "/selection/seat"},
                          "gate": {"path": "/flight/gate"},
                          "terminal": {"path": "/flight/terminal"},
                          "boardingTime": {"path": "/flight/boardingGroup"},
                          "confirmationCode": {
                              "path": "/flight/confirmationCode"
                          },
                          "totalFare": {"path": "/flight/totalFare"},
                          "priorityBoarding": {
                              "path": "/selection/priorityBoarding"
                          },
                          "extraBag": {"path": "/selection/extraBag"},
                      }],
                  },
              },
          ],
      )

      yield self._create_end_event(ctx, author=title)
      await asyncio.sleep(0.5)
      session.completed_steps[title] = 2


class FlightSimulationAgent(BaseSimulationAgent):
  name: str = "flight"
  items: list[dict[str, Any]] = []
  full_itinerary: list[dict[str, Any]] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleFlightSimulationAgent(
            name=f"single_flight_{i}",
            item_title=item.get("title", "Flight"),
            item_data=item,
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(
        name="flights_parallel", sub_agents=sub_agents
    )

    async for event in parallel_agent.run_async(ctx):
      yield event

