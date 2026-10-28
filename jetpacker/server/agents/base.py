from __future__ import annotations

import asyncio
import json
import os
import re
from typing import Any, AsyncGenerator

from google import genai
from google.adk.agents.base_agent import BaseAgent
from google.adk.agents.invocation_context import InvocationContext
from google.adk.events.event import Event
from google.adk.events.event_actions import EventActions
from google.genai import types

BOOKING_ASSISTANT_CATALOG_ID = (
    "https://example.com/catalogs/booking_assistant/v1/catalog.json"
)
PROGRESSIVE_SKELETON_DELAY_SEC = 1.1
LIVE_SEAT_PATCH_DELAY_SEC = 3.5


def _get_genai_client() -> genai.Client:
  """Initializes a Gemini client using Vertex AI or API key."""
  project = os.environ.get("GOOGLE_CLOUD_PROJECT", "your-gcp-project")
  location = os.environ.get("GOOGLE_CLOUD_LOCATION", "us-central1")
  if os.environ.get("GOOGLE_GENAI_USE_VERTEXAI", "").lower() in ("1", "true"):
    return genai.Client(vertexai=True, project=project, location=location)
  api_key = os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
  if api_key:
    return genai.Client(api_key=api_key)
  return genai.Client(vertexai=True, project=project, location=location)


class SessionState:

  def __init__(self) -> None:
    self.pause_events: dict[str, asyncio.Event] = {}
    self.results: dict[str, Any] = {}
    self.completed_steps: dict[str, int] = {}


sessions: dict[str, SessionState] = {}

_KNOWN_AIRPORTS: dict[str, tuple[str, str]] = {
    "ams": ("AMS", "Amsterdam"),
    "amsterdam": ("AMS", "Amsterdam"),
    "schiphol": ("AMS", "Amsterdam"),
    "sfo": ("SFO", "San Francisco"),
    "san francisco": ("SFO", "San Francisco"),
    "cdg": ("CDG", "Paris"),
    "paris": ("CDG", "Paris"),
    "nrt": ("NRT", "Tokyo"),
    "hnd": ("HND", "Tokyo"),
    "tokyo": ("NRT", "Tokyo"),
    "narita": ("NRT", "Tokyo"),
    "haneda": ("HND", "Tokyo"),
    "dub": ("DUB", "Dublin"),
    "dublin": ("DUB", "Dublin"),
    "fco": ("FCO", "Rome"),
    "rome": ("FCO", "Rome"),
    "fiumicino": ("FCO", "Rome"),
    "icn": ("ICN", "Seoul"),
    "seoul": ("ICN", "Seoul"),
    "incheon": ("ICN", "Seoul"),
    "lhr": ("LHR", "London"),
    "london": ("LHR", "London"),
    "jfk": ("JFK", "New York"),
    "new york": ("JFK", "New York"),
    "lax": ("LAX", "Los Angeles"),
    "los angeles": ("LAX", "Los Angeles"),
    "bcn": ("BCN", "Barcelona"),
    "barcelona": ("BCN", "Barcelona"),
    "sin": ("SIN", "Singapore"),
    "singapore": ("SIN", "Singapore"),
}


def _resolve_airport(
    raw_text: str, default_code: str, default_city: str
) -> tuple[str, str]:
  """Extracts an (IATA code, City Name) tuple from free-form text."""
  if not raw_text:
    return default_code, default_city

  lower = raw_text.lower()
  for key, (code, city) in _KNOWN_AIRPORTS.items():
    if re.search(rf"\b{re.escape(key)}\b", lower):
      return code, city

  iata_match = re.search(r"\b([A-Z]{3})\b", raw_text)
  if iata_match:
    code = iata_match.group(1)
    if code not in ("THE", "AND", "FOR", "VIA", "NON", "ADS"):
      city_clean = re.sub(
          r"^(?:return\s+)?flight\s+(?:to|from)\s+",
          "",
          re.sub(r"\([A-Z]{3}\)", "", raw_text),
          flags=re.IGNORECASE,
      ).strip(" -•\t\r\n")
      return code, (city_clean or code)

  cleaned = re.sub(
      r"^(return\s+)?flight\s+(to|from)\s+", "", raw_text, flags=re.IGNORECASE
  ).strip()
  if cleaned:
    code = "".join(ch for ch in cleaned.upper() if ch.isalpha())[:3].ljust(
        3, "X"
    )
    return code, cleaned

  return default_code, default_city



class BaseSimulationAgent(BaseAgent):

  async def _generate_text(self, prompt: str) -> str:
    try:
      client = _get_genai_client()
      response = await asyncio.to_thread(
          client.models.generate_content,
          model="gemini-2.5-flash",
          contents=prompt,
      )
      text = response.text or ""
      return text.strip()
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(f"WARN: _generate_text fallback triggered: {e}")
      return ""

  def _create_text_event(
      self, ctx: InvocationContext, text: str, author: str | None = None
  ) -> Event:
    return Event(
        invocation_id=ctx.invocation_id,
        author=author or self.name,
        content=types.Content(parts=[types.Part(text=text)]),
        branch=ctx.branch,
    )

  def _create_a2ui_event(
      self,
      ctx: InvocationContext,
      author: str,
      messages: list[dict[str, Any]],
  ) -> Event:
    return self._create_text_event(
        ctx,
        json.dumps(messages),
        author=author,
    )

  def _create_end_event(
      self, ctx: InvocationContext, author: str | None = None
  ) -> Event:
    return Event(
        invocation_id=ctx.invocation_id,
        author=author or self.name,
        actions=EventActions(end_of_agent=True),
        content=types.Content(parts=[]),
        branch=ctx.branch,
    )

  def _create_options_event(
      self,
      ctx: InvocationContext,
      title: str,
      options: list[str],
      message: str,
      ui_type: str = "seat_map",
  ) -> Event:
    return self._create_text_event(
        ctx,
        json.dumps({
            "ui": ui_type,
            "options": options,
            "message": message,
        }),
        author=title,
    )

