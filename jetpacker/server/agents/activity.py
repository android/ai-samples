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
    BaseSimulationAgent,
    _get_genai_client,
    sessions,
)
from schemas import ActivityOrDiningProposal

class SingleMuseumSimulationAgent(BaseSimulationAgent):
  name: str = "single_museum"
  item_title: str = "Museum"
  item_data: dict[str, Any] = {}
  full_itinerary: list[dict[str, Any]] = []

  async def _generate_activity_proposal(self) -> dict[str, Any]:
    title = self.item_title
    date_label = self.item_data.get("dateLabel") or ""
    fallback = {
        "prompt": (
            "Select your preferred timed-entry window and pass tier for"
            f" {title}{f' ({date_label})' if date_label else ''}:"
        ),
        "options": [
            "10:00 AM • VIP Skip-the-Line + Audio Guide",
            "01:30 PM • Express Afternoon Entry",
            "04:00 PM • Golden Hour Curator Tour",
        ],
        "holdConfirmationPrompt": (
            f"Holding 2 passes for {title}. Instant mobile QR entry included."
        ),
        "confirmedDescription": "Mobile QR Passes added to your trip wallet.",
    }
    try:
      client = _get_genai_client()
      prompt = (
          "You are a travel concierge generating an A2UI v0.9 activity/museum"
          " ticket booking payload.\n"
          f"Activity event item: {json.dumps(self.item_data)}\n"
          f"Full trip itinerary context: {json.dumps(self.full_itinerary)}\n"
          "Return an ActivityOrDiningProposal JSON tailored to this specific"
          " museum, tour, or activity:\n"
          "- `prompt`: 1 concise sentence inviting the user to choose a"
          " timed-entry window or tour tier (never mention 'AI' or 'A2UI').\n"
          "- `options`: Exactly 3 realistic time-slot + experience tier strings"
          " formatted like '10:00 AM • <Specific Tour/Entry Tier>'.\n"
          "- `holdConfirmationPrompt`: 1 concise sentence summarizing what"
          " is held before final confirmation.\n"
          "- `confirmedDescription`: Short confirmation note for the wallet."
      )
      response = await asyncio.to_thread(
          client.models.generate_content,
          model="gemini-2.5-flash",
          contents=prompt,
          config=types.GenerateContentConfig(
              response_mime_type="application/json",
              response_schema=ActivityOrDiningProposal,
              temperature=0.3,
          ),
      )
      if response.text:
        parsed = json.loads(response.text)
        if parsed.get("options"):
          print(
              f"INFO: Gemini generated activity options for {title}:"
              f" {parsed['options']}"
          )
          return parsed
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(f"WARN: Gemini activity fallback used for {title}: {e}")
    return fallback

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title
    proposal = await self._generate_activity_proposal()
    options = proposal["options"]

    # Step 1: Select Timed Entry Slot
    yield self._create_a2ui_event(
        ctx,
        author=title,
        messages=[
            {
                "version": "v0.9",
                "createSurface": {
                    "surfaceId": title,
                    "catalogId": BOOKING_ASSISTANT_CATALOG_ID,
                },
            },
            {
                "version": "v0.9",
                "updateComponents": {
                    "surfaceId": title,
                    "components": [{
                        "id": "root",
                        "component": "InteractiveOptionPicker",
                        "category": "Activity",
                        "title": title,
                        "status": "SELECT ENTRY SLOT",
                        "prompt": proposal["prompt"],
                        "options": options,
                        "selectedOption": options[0],
                        "confirmBtnText": "Continue to Pass Confirmation",
                        "action": {
                            "event": {
                                "name": "confirm_option",
                                "context": {
                                    "agentId": title,
                                    "selectionType": "tickets",
                                },
                            }
                        },
                    }],
                },
            },
        ],
    )

    pause_key = title
    if pause_key not in session.pause_events:
      session.pause_events[pause_key] = asyncio.Event()
    else:
      session.pause_events[pause_key].clear()

    await session.pause_events[pause_key].wait()
    resp = session.results.get(title)
    chosen_slot = (
        (
            resp.get("selectedOption")
            or resp.get("people")
            or resp.get("tickets")
            or resp.get("time")
            or options[0]
        )
        if isinstance(resp, dict)
        else str(resp or options[0])
    )

    # Step 2: Final Confirmation Card
    yield self._create_a2ui_event(
        ctx,
        author=title,
        messages=[{
            "version": "v0.9",
            "updateComponents": {
                "surfaceId": title,
                "components": [{
                    "id": "root",
                    "component": "BookingConfirmation",
                    "category": "Activity",
                    "title": title,
                    "status": "FINAL REVIEW",
                    "prompt": (
                        f"Holding passes for '{chosen_slot}'."
                        f" {proposal['holdConfirmationPrompt']}"
                    ),
                    "confirmBtnText": "Issue Mobile Passes",
                    "action": {
                        "event": {
                            "name": "confirm_booking",
                            "context": {
                                "agentId": title,
                                "selectionType": "confirm",
                            },
                        }
                    },
                }],
            },
        }],
    )

    session.pause_events[pause_key].clear()
    await session.pause_events[pause_key].wait()

    yield self._create_a2ui_event(
        ctx,
        author=title,
        messages=[{
            "version": "v0.9",
            "updateComponents": {
                "surfaceId": title,
                "components": [{
                    "id": "root",
                    "component": "BookingStatus",
                    "category": "Activity",
                    "title": title,
                    "description": (
                        f"Confirmed: {chosen_slot} •"
                        f" {proposal['confirmedDescription']}"
                    ),
                    "status": "Confirmed",
                }],
            },
        }],
    )
    await asyncio.sleep(0.4)
    yield self._create_end_event(ctx, author=title)


class MuseumSimulationAgent(BaseSimulationAgent):
  name: str = "museum"
  items: list[dict[str, Any]] = []
  full_itinerary: list[dict[str, Any]] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleMuseumSimulationAgent(
            name=f"single_museum_{i}",
            item_title=item.get("title", "Museum"),
            item_data=item,
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(
        name="museums_parallel", sub_agents=sub_agents
    )

    async for event in parallel_agent.run_async(ctx):
      yield event

    yield self._create_end_event(ctx, author=self.name)


class SingleRestaurantSimulationAgent(BaseSimulationAgent):
  name: str = "single_restaurant"
  item_title: str = "Restaurant"
  item_data: dict[str, Any] = {}
  full_itinerary: list[dict[str, Any]] = []

  async def _generate_dining_proposal(self) -> dict[str, Any]:
    title = self.item_title
    dining_detail = self.item_data.get("diningDetail") or {}
    party = dining_detail.get("partySize") or 2
    date_label = self.item_data.get("dateLabel") or ""
    fallback = {
        "prompt": (
            f"Select your party size and seating zone for {title}"
            f"{f' ({date_label})' if date_label else ''}:"
        ),
        "options": [
            f"{party} Guests • 7:00 PM Chef's Counter",
            f"{party} Guests • 7:45 PM Main Dining Room",
            f"{party} Guests • 8:15 PM Patio Terrace",
            f"{party + 2} Guests • 8:30 PM Private Booth",
        ],
        "holdConfirmationPrompt": (
            "Complimentary welcome aperitif included with your reservation."
        ),
        "confirmedDescription": "Reservation confirmed and added to itinerary.",
    }
    try:
      client = _get_genai_client()
      prompt = (
          "You are a restaurant concierge generating an A2UI v0.9 dining"
          " reservation payload.\n"
          f"Dining event item: {json.dumps(self.item_data)}\n"
          f"Full trip itinerary context: {json.dumps(self.full_itinerary)}\n"
          "Return an ActivityOrDiningProposal JSON tailored to this specific"
          " restaurant and city:\n"
          "- `prompt`: 1 concise sentence inviting the user to choose their"
          " seating zone and time (never mention 'AI' or 'A2UI').\n"
          "- `options`: Exactly 4 realistic seating options formatted like"
          " '2 Guests • 7:30 PM <Specific Seating Zone/Tasting Counter>'.\n"
          "- `holdConfirmationPrompt`: 1 concise sentence describing a"
          " signature welcome perk or tasting note for this restaurant.\n"
          "- `confirmedDescription`: Short confirmation code and summary."
      )
      response = await asyncio.to_thread(
          client.models.generate_content,
          model="gemini-2.5-flash",
          contents=prompt,
          config=types.GenerateContentConfig(
              response_mime_type="application/json",
              response_schema=ActivityOrDiningProposal,
              temperature=0.3,
          ),
      )
      if response.text:
        parsed = json.loads(response.text)
        if parsed.get("options"):
          print(
              f"INFO: Gemini generated dining options for {title}:"
              f" {parsed['options']}"
          )
          return parsed
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(f"WARN: Gemini dining fallback used for {title}: {e}")
    return fallback

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title
    proposal = await self._generate_dining_proposal()
    options = proposal["options"]

    yield self._create_a2ui_event(
        ctx,
        author=title,
        messages=[
            {
                "version": "v0.9",
                "createSurface": {
                    "surfaceId": title,
                    "catalogId": BOOKING_ASSISTANT_CATALOG_ID,
                },
            },
            {
                "version": "v0.9",
                "updateComponents": {
                    "surfaceId": title,
                    "components": [{
                        "id": "root",
                        "component": "InteractiveOptionPicker",
                        "category": "Dining",
                        "title": title,
                        "status": "SELECT TABLE & TIME",
                        "prompt": proposal["prompt"],
                        "options": options,
                        "selectedOption": options[0],
                        "confirmBtnText": "Hold Table & Review",
                        "action": {
                            "event": {
                                "name": "confirm_option",
                                "context": {
                                    "agentId": title,
                                    "selectionType": "tickets",
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

    resp = session.results.get(title)
    if isinstance(resp, dict):
      table_choice = (
          resp.get("selectedOption")
          or resp.get("people")
          or resp.get("tickets")
          or options[0]
      )
    else:
      table_choice = str(resp or options[0])

    # Step 2: Confirm dietary/tasting note
    yield self._create_a2ui_event(
        ctx,
        author=title,
        messages=[{
            "version": "v0.9",
            "updateComponents": {
                "surfaceId": title,
                "components": [{
                    "id": "root",
                    "component": "BookingConfirmation",
                    "category": "Dining",
                    "title": title,
                    "status": "CONFIRM RESERVATION",
                    "prompt": (
                        f"Table held for {table_choice}."
                        f" {proposal['holdConfirmationPrompt']}"
                    ),
                    "confirmBtnText": "Confirm Table Reservation",
                    "action": {
                        "event": {
                            "name": "confirm_booking",
                            "context": {
                                "agentId": title,
                                "selectionType": "confirm",
                            },
                        }
                    },
                }],
            },
        }],
    )

    session.pause_events[title].clear()
    await session.pause_events[title].wait()

    yield self._create_a2ui_event(
        ctx,
        author=title,
        messages=[{
            "version": "v0.9",
            "updateComponents": {
                "surfaceId": title,
                "components": [{
                    "id": "root",
                    "component": "BookingStatus",
                    "category": "Dining",
                    "title": title,
                    "description": (
                        f"Confirmed: {table_choice} •"
                        f" {proposal['confirmedDescription']}"
                    ),
                    "status": "Confirmed",
                }],
            },
        }],
    )
    yield self._create_end_event(ctx, author=title)
    await asyncio.sleep(0.4)


class RestaurantSimulationAgent(BaseSimulationAgent):
  name: str = "restaurant"
  items: list[dict[str, Any]] = []
  full_itinerary: list[dict[str, Any]] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleRestaurantSimulationAgent(
            name=f"single_restaurant_{i}",
            item_title=item.get("title", "Restaurant"),
            item_data=item,
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(
        name="restaurants_parallel", sub_agents=sub_agents
    )

    async for event in parallel_agent.run_async(ctx):
      yield event

