from __future__ import annotations

import asyncio
import json
import re
from typing import Any, AsyncGenerator

from google.adk.agents.invocation_context import InvocationContext
from google.adk.agents.parallel_agent import ParallelAgent
from google.adk.events.event import Event
from google.genai import types

from agents.activity import (
    MuseumSimulationAgent,
    RestaurantSimulationAgent,
    SingleMuseumSimulationAgent,
    SingleRestaurantSimulationAgent,
)
from agents.base import (
    BOOKING_ASSISTANT_CATALOG_ID,
    BaseSimulationAgent,
    SessionState,
    _get_genai_client,
    _resolve_airport,
    sessions,
)
from agents.flight import FlightSimulationAgent, SingleFlightSimulationAgent
from agents.hotel import HotelSimulationAgent, SingleHotelSimulationAgent

class ItineraryOrchestratorAgent(BaseSimulationAgent):
  name: str = "booking"

  def _create_chat_reply(
      self,
      ctx: InvocationContext,
      message: str,
      surface_id: str | None = None,
  ) -> Event:
    payload: dict[str, Any] = {"chatMessage": message}
    if surface_id:
      payload["surfaceId"] = surface_id
    return self._create_text_event(
        ctx, json.dumps(payload), author="__assistant__"
    )

  async def _generate_conversational_reply(
      self,
      user_prompt: str,
      itinerary: list[dict[str, Any]],
  ) -> str:
    first_item = itinerary[0] if itinerary else {}
    trip_title = first_item.get("tripTitle") or "your upcoming trip"
    trip_location = first_item.get("tripLocation") or ""
    item_titles = [e.get("title", "") for e in itinerary if e.get("title")][:4]
    items_summary = (
        ", ".join(item_titles)
        if item_titles
        else "flights, hotel stays, dining, and activities"
    )
    loc_suffix = f" to {trip_location}" if trip_location else ""
    fallback = (
        f"Hello! I'm your Trip Concierge for {trip_title}{loc_suffix}. "
        f"I can help you book or customize {items_summary}—just let me know "
        "what you'd like to arrange!"
    )
    try:
      client = _get_genai_client()
      prompt = (
          "You are a warm, helpful travel concierge in the JetSet app.\n"
          f"Trip: {trip_title}{loc_suffix}\n"
          f"Trip itinerary items: {json.dumps(itinerary)}\n"
          f"User message: {json.dumps(user_prompt)}\n"
          "Reply directly to the user in 1-2 concise, friendly sentences "
          "as plain text. Mention relevant items from their trip that you "
          "can help book (such as flights, hotel, dinner, or activities) "
          "if appropriate. Never mention 'A2UI' or internal system terms."
      )
      response = await asyncio.to_thread(
          client.models.generate_content,
          model="gemini-2.5-flash",
          contents=prompt,
          config=types.GenerateContentConfig(
              temperature=0.4,
          ),
      )
      if response.text and response.text.strip():
        return response.text.strip()
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(f"WARN: Gemini conversational reply fallback used: {e}")
    return fallback

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if ctx.session.id not in sessions:
      if len(sessions) > 500:
        for k in list(sessions.keys())[:100]:
          sessions.pop(k, None)
      sessions[ctx.session.id] = SessionState()

    user_text = ""
    if ctx.user_content and ctx.user_content.parts:
      user_text = ctx.user_content.parts[0].text or ""

    command = "start"
    intent = ""
    raw_prompt = ""
    prompt_text = ""
    target_title = ""
    try:
      data = json.loads(user_text)
      itinerary = data.get("itinerary", [])
      command = data.get("command", "start")
      intent = data.get("intent", "").lower()
      raw_prompt = data.get("prompt", "")
      prompt_text = raw_prompt.lower()
      target_title = data.get("targetTitle", "")
      print(
          f"DEBUG: command={command}, intent={intent}, target={target_title},"
          f" itinerary_len={len(itinerary)}"
      )
    except json.JSONDecodeError:
      itinerary = [
          {"type": "TRANSPORTATION", "title": "CDG > SFO"},
          {"type": "ACCOMMODATION", "title": "The Redwood Grove Hotel"},
          {"type": "FOOD_AND_DRINK", "title": "Dinner @ Quantum Bites"},
          {"type": "CULTURE", "title": "SFMOMA Museum Visit"},
      ]

    flight_items = [e for e in itinerary if e.get("type") == "TRANSPORTATION"]
    hotel_items = [e for e in itinerary if e.get("type") == "ACCOMMODATION"]
    restaurant_items = [
        e for e in itinerary if e.get("type") == "FOOD_AND_DRINK"
    ]
    museum_items = [
        e for e in itinerary if e.get("type") in ["CULTURE", "ACTIVITY"]
    ]

    # Conversational single-flow mode triggered by user chat message
    if command == "chat":
      combined = f"{intent} {prompt_text}".lower()

      if intent == "hotel" or (
          intent != "conversation"
          and (
              "hotel" in combined
              or "stay" in combined
              or "room" in combined
              or "suite" in combined
              or "redwood" in combined
              or "meurice" in combined
          )
      ):
        chosen = next(
            (h for h in hotel_items if h.get("title") == target_title),
            hotel_items[0]
            if hotel_items
            else {
                "type": "ACCOMMODATION",
                "title": target_title or "The Redwood Grove Hotel",
                "location": "Mountain View, CA",
            },
        )
        h_title = chosen.get("title", "The Redwood Grove Hotel")
        h_nights = (chosen.get("hotelDetail") or {}).get("nights", 4)
        sessions[ctx.session.id].completed_steps[h_title] = 0
        yield self._create_chat_reply(
            ctx,
            f"I've pulled up suite availability at {h_title} for your"
            f" {h_nights}-night stay. Choose your room tier below, then"
            " customize your check-in window and hospitality packages:",
            surface_id=h_title,
        )
        agent = SingleHotelSimulationAgent(
            name="chat_hotel",
            item_title=h_title,
            item_data=chosen,
            full_itinerary=itinerary,
        )
        async for event in agent.run_async(ctx):
          yield event
        yield self._create_chat_reply(
            ctx,
            f"Your NFC Digital Room Key at {h_title} is active and"
            " saved to your trip! Would you like to book your flights or"
            " reserve dinner next?",
        )
        return

      if intent == "dining" or (
          intent != "conversation"
          and (
              "dinner" in combined
              or "lunch" in combined
              or "restaurant" in combined
              or "table" in combined
              or "quantum" in combined
              or "dining" in combined
              or "verne" in combined
          )
      ):
        chosen = next(
            (r for r in restaurant_items if r.get("title") == target_title),
            next(
                (
                    r
                    for r in restaurant_items
                    if "dinner" in r.get("title", "").lower()
                ),
                restaurant_items[0]
                if restaurant_items
                else {
                    "type": "FOOD_AND_DRINK",
                    "title": target_title or "Dinner @ Quantum Bites",
                },
            ),
        )
        r_title = chosen.get("title", "Dinner @ Quantum Bites")
        sessions[ctx.session.id].completed_steps[r_title] = 0
        yield self._create_chat_reply(
            ctx,
            f"I found prime availability for {r_title}. Select your"
            " party size and seating zone below:",
            surface_id=r_title,
        )
        agent = SingleRestaurantSimulationAgent(
            name="chat_restaurant",
            item_title=r_title,
            item_data=chosen,
            full_itinerary=itinerary,
        )
        async for event in agent.run_async(ctx):
          yield event
        yield self._create_chat_reply(
            ctx,
            f"Your table for {r_title} is locked in! Anything else you'd like"
            " to book?",
        )
        return

      if intent == "activity" or (
          intent != "conversation"
          and (
              "museum" in combined
              or "activity" in combined
              or "sfmoma" in combined
              or "cruise" in combined
              or "tour" in combined
              or "louvre" in combined
          )
      ):
        chosen = next(
            (m for m in museum_items if m.get("title") == target_title),
            museum_items[0]
            if museum_items
            else {
                "type": "CULTURE",
                "title": target_title or "SFMOMA Museum Visit",
            },
        )
        m_title = chosen.get("title", "SFMOMA Museum Visit")
        sessions[ctx.session.id].completed_steps[m_title] = 0
        yield self._create_chat_reply(
            ctx,
            f"Here are the timed-entry options for {m_title}. Choose your"
            " preferred window:",
            surface_id=m_title,
        )
        agent = SingleMuseumSimulationAgent(
            name="chat_museum",
            item_title=m_title,
            item_data=chosen,
            full_itinerary=itinerary,
        )
        async for event in agent.run_async(ctx):
          yield event
        yield self._create_chat_reply(
            ctx,
            f"Your mobile passes for {m_title} are ready in your trip wallet!",
        )
        return

      if intent == "flight" or (
          intent != "conversation"
          and (
              "flight" in combined
              or "fly" in combined
              or "plane" in combined
              or "airline" in combined
              or "airport" in combined
              or "seat" in combined
              or "boarding" in combined
          )
      ):
        chosen = next(
            (f for f in flight_items if f.get("title") == target_title),
            flight_items[0]
            if flight_items
            else {
                "type": "TRANSPORTATION",
                "title": target_title or "CDG > SFO",
                "location": "SFO International Terminal",
            },
        )
        f_title = chosen.get("title", "CDG > SFO")
        sessions[ctx.session.id].completed_steps[f_title] = 0
        yield self._create_chat_reply(
            ctx,
            f"I found 3 flights for your {f_title} route. Select your preferred"
            " departure below, then customize your cabin seat and travel"
            " perks:",
            surface_id=f_title,
        )
        agent = SingleFlightSimulationAgent(
            name="chat_flight",
            item_title=f_title,
            item_data=chosen,
            full_itinerary=itinerary,
        )
        async for event in agent.run_async(ctx):
          yield event
        yield self._create_chat_reply(
            ctx,
            f"Your boarding pass for {f_title} is issued and saved to your"
            " itinerary! Would you like to book your hotel stay or reserve a"
            " table for dinner next?",
        )
        return

      # General conversational message (e.g. "hello!", "what can we do?")
      reply_text = await self._generate_conversational_reply(
          raw_prompt or prompt_text, itinerary
      )
      yield self._create_chat_reply(ctx, reply_text)
      yield self._create_end_event(ctx, author="__assistant__")
      return

    # Legacy parallel fallback if command == "start"
    sub_agents = []
    if hotel_items:
      sub_agents.append(
          HotelSimulationAgent(items=hotel_items, full_itinerary=itinerary)
      )
    if museum_items:
      sub_agents.append(
          MuseumSimulationAgent(items=museum_items, full_itinerary=itinerary)
      )
    if restaurant_items:
      sub_agents.append(
          RestaurantSimulationAgent(
              items=restaurant_items, full_itinerary=itinerary
          )
      )
    if flight_items:
      sub_agents.append(
          FlightSimulationAgent(items=flight_items, full_itinerary=itinerary)
      )

    if not sub_agents:
      yield self._create_text_event(
          ctx, "No relevant agents found for itinerary."
      )
      return

    parallel_agent = ParallelAgent(
        name="parallel_orchestrator", sub_agents=sub_agents
    )

    async for event in parallel_agent.run_async(ctx):
      yield event

