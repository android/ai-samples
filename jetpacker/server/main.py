# Copyright 2026 Google LLC.
# SPDX-License-Identifier: Apache-2.0

"""Booking Assistant ADK Agent Server for Jetpacker with Jetpack A2UI."""

import asyncio
import json
import os
import re
import traceback
from typing import Any, Dict, List, Optional
import urllib.request
import uuid

import fastapi
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from google.adk import Agent
from google.adk.runners import InMemoryRunner
from google.adk.tools import FunctionTool
from google.adk.tools.tool_context import ToolContext
from google.genai import types as genai_types
import pydantic
from pydantic import BaseModel, Field
import uvicorn

from a2ui.basic_catalog.provider import BasicCatalog
from a2ui.schema.catalog import CatalogConfig
from a2ui.schema.constants import VERSION_0_9
from a2ui.schema.manager import A2uiSchemaManager


def sanitize_agent_name(title: str) -> str:
  sanitized = re.sub(r'[^a-zA-Z0-9_]', '_', title)
  if not re.match(r'^[a-zA-Z_]', sanitized):
    sanitized = '_' + sanitized
  return sanitized


app = FastAPI(title="JetPacker Booking Assistant Server", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize A2UI Schema Manager with custom booking component catalog
CATALOG_PATH = os.path.join(os.path.dirname(__file__), "booking_catalog.json")

schema_manager = A2uiSchemaManager(
    version=VERSION_0_9,
    catalogs=[
        BasicCatalog.get_config(version=VERSION_0_9),
        CatalogConfig.from_path(
            name="https://example.com/catalogs/booking_assistant/v1/catalog.json",
            catalog_path=CATALOG_PATH,
        ),
    ],
)

A2UI_SYSTEM_INSTRUCTION = schema_manager.generate_system_prompt(
    role_description="You are a helpful travel booking assistant.",
    ui_description=(
        "Use InteractiveOptionPicker for choices, SeatSelectionPicker for seat"
        " selection, and BookingStatus for confirmed status."
    ),
    include_schema=True,
    include_examples=True,
    allowed_components=[
        "InteractiveOptionPicker",
        "SeatSelectionPicker",
        "BookingStatus",
    ],
).replace("${expression}", "$(expression)")


class SessionState:

  def __init__(self):
    self.pause_events: Dict[str, asyncio.Future] = {}
    self.queue: asyncio.Queue = asyncio.Queue()
    self.bg_tasks: List[asyncio.Task] = []


sessions: Dict[str, SessionState] = {}


class ComponentProperties(BaseModel):
  title: Optional[str] = Field(default=None, description="Title for the card.")
  category: Optional[str] = Field(
      default=None,
      description="Category name (Flight, Hotel, Activity, Dining).",
  )
  status: Optional[str] = Field(
      default=None,
      description="Status label (e.g. CONFIRMATION REQUIRED, ACTION REQUIRED).",
  )
  prompt: Optional[str] = Field(
      default=None, description="Header prompt/choice text for picker widgets."
  )
  options: Optional[List[str]] = Field(
      default=None,
      description="Custom option labels for InteractiveOptionPicker.",
  )
  selectedOption: Optional[str] = Field(
      default=None, description="Currently selected option string."
  )
  selectedIdx: Optional[int] = Field(
      default=None,
      description="Currently selected option index (0-indexed).",
  )
  confirmBtnText: Optional[str] = Field(
      default=None, description="Label for picker confirmation button."
  )
  seats: Optional[List[str]] = Field(
      default=None, description="Seat labels list for SeatSelectionPicker."
  )
  selectedSeat: Optional[str] = Field(
      default=None, description="Currently selected seat label."
  )
  description: Optional[str] = Field(
      default=None, description="Booking status description text."
  )
  text: Optional[str] = Field(
      default=None, description="Booking status description text fallback."
  )
  action: Optional[Dict[str, Any]] = Field(
      default=None, description="Custom action payload."
  )


class A2uiComponent(BaseModel):
  id: str = Field(
      default="root",
      description="Must be 'root' for the main element in the surface card.",
  )
  component: str = Field(
      description=(
          "The component name (e.g. 'InteractiveOptionPicker',"
          " 'SeatSelectionPicker', 'BookingStatus')."
      )
  )
  properties: ComponentProperties = Field(
      description="The properties for the component."
  )


def make_update_ui(surface_id: str, session: SessionState):
  async def update_ui(
      components: List[A2uiComponent],
      tool_context: ToolContext = None,
  ) -> str:
    """Updates the UI surface with the specified components and waits for user interaction."""
    components_dict = [c.model_dump(exclude_none=True) for c in components]
    for c in components_dict:
      if "type" not in c and "component" in c:
        c["type"] = c["component"]

    option_picker = next(
        (c for c in components if c.component == "InteractiveOptionPicker"),
        None,
    )
    seat_picker = next(
        (c for c in components if c.component == "SeatSelectionPicker"), None
    )

    if option_picker:
      options = option_picker.properties.options or []
      selected_option = option_picker.properties.selectedOption or (
          options[0] if options else ""
      )
      confirmed = False

      while not confirmed:
        for c in components_dict:
          if c.get("component") == "InteractiveOptionPicker":
            c["properties"]["selectedOption"] = selected_option
            c["properties"]["status"] = "CONFIRMATION REQUIRED"

        await session.queue.put({
            "updateComponents": {
                "surfaceId": surface_id,
                "components": components_dict,
            }
        })

        fut = asyncio.get_event_loop().create_future()
        session.pause_events[surface_id] = fut
        response = await fut

        if response.startswith("SelectOption_"):
          idx = int(response.removeprefix("SelectOption_"))
          if 0 <= idx < len(options):
            selected_option = options[idx]
        elif response.startswith("ConfirmSelection_") or response in [
            "Confirmed",
            "Confirm",
        ]:
          confirmed = True
        else:
          selected_option = response
          confirmed = True

      return selected_option

    elif seat_picker:
      seats = (
          seat_picker.properties.seats
          or seat_picker.properties.options
          or ["1A", "1B", "2A", "2B"]
      )
      selected_seat = seat_picker.properties.selectedSeat or seats[0]
      seat_confirmed = False

      while not seat_confirmed:
        for c in components_dict:
          if c.get("component") == "SeatSelectionPicker":
            c["properties"]["selectedSeat"] = selected_seat
            c["properties"]["status"] = "ACTION REQUIRED"

        await session.queue.put({
            "updateComponents": {
                "surfaceId": surface_id,
                "components": components_dict,
            }
        })

        fut = asyncio.get_event_loop().create_future()
        session.pause_events[surface_id] = fut
        response = await fut

        selected_seat = response
        seat_confirmed = True

      return selected_seat

    else:
      await session.queue.put({
          "updateComponents": {
              "surfaceId": surface_id,
              "components": components_dict,
          }
      })
      return ""

  return update_ui


async def run_flight_agent(title: str, session: SessionState, thread_id: str):
  try:
    print(f"AGENT [Flight - '{title}']: started")
    await session.queue.put({
        "createSurface": {
            "surfaceId": title,
            "catalogId": (
                "https://example.com/catalogs/booking_assistant/v1/catalog.json"
            ),
        }
    })

    agent = Agent(
        name=sanitize_agent_name(title),
        model="gemini-2.5-flash",
        instruction=(
            f"Book a flight for: '{title}'. Use the update_ui tool to ask the"
            " user to select flight time and seats. Finally use update_ui to"
            " show booking confirmation status.\n\n"
            + A2UI_SYSTEM_INSTRUCTION
        ),
        tools=[FunctionTool(make_update_ui(title, session))],
    )

    agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
    runner = InMemoryRunner(agent, app_name="BookingServer")
    await runner.session_service.create_session(
        app_name="BookingServer",
        user_id=thread_id,
        session_id=agent_session_id,
    )

    async for _ in runner.run_async(
        new_message=genai_types.Content(
            parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]
        ),
        user_id=thread_id,
        session_id=agent_session_id,
    ):
      pass
  except Exception as e:
    print(f"AGENT [Flight - '{title}'] Error: {e}")
    traceback.print_exc()


async def run_hotel_agent(title: str, session: SessionState, thread_id: str):
  try:
    print(f"AGENT [Hotel - '{title}']: started")
    await session.queue.put({
        "createSurface": {
            "surfaceId": title,
            "catalogId": (
                "https://example.com/catalogs/booking_assistant/v1/catalog.json"
            ),
        }
    })

    agent = Agent(
        name=sanitize_agent_name(title),
        model="gemini-2.5-flash",
        instruction=(
            f"Book a hotel for: '{title}'. Use the update_ui tool to ask the"
            " user to choose a lodging room. Finally use update_ui to show"
            " booking confirmation status.\n\n"
            + A2UI_SYSTEM_INSTRUCTION
        ),
        tools=[FunctionTool(make_update_ui(title, session))],
    )

    agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
    runner = InMemoryRunner(agent, app_name="BookingServer")
    await runner.session_service.create_session(
        app_name="BookingServer",
        user_id=thread_id,
        session_id=agent_session_id,
    )

    async for _ in runner.run_async(
        new_message=genai_types.Content(
            parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]
        ),
        user_id=thread_id,
        session_id=agent_session_id,
    ):
      pass
  except Exception as e:
    print(f"AGENT [Hotel - '{title}'] Error: {e}")
    traceback.print_exc()


async def run_museum_agent(title: str, session: SessionState, thread_id: str):
  try:
    print(f"AGENT [Museum - '{title}']: started")
    await session.queue.put({
        "createSurface": {
            "surfaceId": title,
            "catalogId": (
                "https://example.com/catalogs/booking_assistant/v1/catalog.json"
            ),
        }
    })

    agent = Agent(
        name=sanitize_agent_name(title),
        model="gemini-2.5-flash",
        instruction=(
            f"Book museum tickets for: '{title}'. Use the update_ui tool to ask"
            " the user to choose entry ticket options. Finally use update_ui to"
            " show booking confirmation status.\n\n"
            + A2UI_SYSTEM_INSTRUCTION
        ),
        tools=[FunctionTool(make_update_ui(title, session))],
    )

    agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
    runner = InMemoryRunner(agent, app_name="BookingServer")
    await runner.session_service.create_session(
        app_name="BookingServer",
        user_id=thread_id,
        session_id=agent_session_id,
    )

    async for _ in runner.run_async(
        new_message=genai_types.Content(
            parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]
        ),
        user_id=thread_id,
        session_id=agent_session_id,
    ):
      pass
  except Exception as e:
    print(f"AGENT [Museum - '{title}'] Error: {e}")
    traceback.print_exc()


async def run_restaurant_agent(
    title: str, session: SessionState, thread_id: str
):
  try:
    print(f"AGENT [Restaurant - '{title}']: started")
    await session.queue.put({
        "createSurface": {
            "surfaceId": title,
            "catalogId": (
                "https://example.com/catalogs/booking_assistant/v1/catalog.json"
            ),
        }
    })

    agent = Agent(
        name=sanitize_agent_name(title),
        model="gemini-2.5-flash",
        instruction=(
            f"Book a table reservation for restaurant: '{title}'. Use the"
            " update_ui tool to ask the user to choose a party size. Finally use"
            " update_ui to show booking confirmation status.\n\n"
            + A2UI_SYSTEM_INSTRUCTION
        ),
        tools=[FunctionTool(make_update_ui(title, session))],
    )

    agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
    runner = InMemoryRunner(agent, app_name="BookingServer")
    await runner.session_service.create_session(
        app_name="BookingServer",
        user_id=thread_id,
        session_id=agent_session_id,
    )

    async for _ in runner.run_async(
        new_message=genai_types.Content(
            parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]
        ),
        user_id=thread_id,
        session_id=agent_session_id,
    ):
      pass
  except Exception as e:
    print(f"AGENT [Restaurant - '{title}'] Error: {e}")
    traceback.print_exc()


async def stream_booking_session(input_data: dict) -> StreamingResponse:
  thread_id = (
      input_data.get("session_id")
      or input_data.get("threadId")
      or str(uuid.uuid4())
  )

  if thread_id in sessions:
    old_session = sessions[thread_id]
    print(f"SERVER: Cleaning up old background tasks for sessionId={thread_id}")
    for task in old_session.bg_tasks:
      if not task.done():
        task.cancel()

  session = SessionState()
  sessions[thread_id] = session

  # Extract itinerary items
  itinerary_items = []
  if "itinerary" in input_data:
    itinerary_items = input_data["itinerary"]
  elif "new_message" in input_data:
    parts = input_data.get("new_message", {}).get("parts", [])
    for p in parts:
      txt = p.get("text", "")
      try:
        data = json.loads(txt)
        if "itinerary" in data:
          itinerary_items = data["itinerary"]
      except Exception:
        pass
  elif "messages" in input_data:
    for msg in reversed(input_data.get("messages", [])):
      content = msg.get("content")
      if isinstance(content, str):
        try:
          data = json.loads(content)
          itinerary_items = data.get("events", [])
          if itinerary_items:
            break
        except Exception:
          pass

  async def event_generator():
    print(f"SERVER [threadId={thread_id}]: Starting event generator stream")
    tasks = []
    for item in itinerary_items:
      title = item.get("title", "Booking")
      type_val = item.get("type", "")
      if type_val in ["TRANSPORTATION", "Flight"]:
        tasks.append(run_flight_agent(title, session, thread_id))
      elif type_val in ["LODGING", "ACCOMMODATION", "Hotel"]:
        tasks.append(run_hotel_agent(title, session, thread_id))
      elif type_val in ["CULTURE", "ACTIVITY", "Activity"]:
        tasks.append(run_museum_agent(title, session, thread_id))
      elif type_val in ["FOOD_AND_DRINK", "Dining"]:
        tasks.append(run_restaurant_agent(title, session, thread_id))

    async def run_all_and_signal():
      try:
        if tasks:
          await asyncio.gather(*tasks)
        print(f"SERVER [threadId={thread_id}]: All sub-agent tasks completed")
      except Exception:
        traceback.print_exc()
      finally:
        await session.queue.put("__DONE__")

    bg_task = asyncio.create_task(run_all_and_signal())
    session.bg_tasks.append(bg_task)

    while True:
      try:
        payload = await asyncio.wait_for(session.queue.get(), timeout=15.0)
        if payload == "__DONE__":
          break
        yield f"data: {json.dumps(payload)}\n\n"
      except asyncio.TimeoutError:
        yield ": keep-alive\n\n"

    print(f"SERVER [threadId={thread_id}]: Stream completed")

  return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/")
async def root_post(input_data: dict):
  return await stream_booking_session(input_data)


@app.post("/run_sse")
async def run_sse_post(input_data: dict):
  return await stream_booking_session(input_data)


class ResponseData(BaseModel):
  seat: Optional[str] = None
  time: Optional[str] = None
  tickets: Optional[str] = None
  people: Optional[str] = None
  confirmed: Optional[bool] = None


@app.post("/respond")
async def respond(
    request: Request,
    session_id: Optional[str] = Query(None),
    sessionId: Optional[str] = Query(None),
    agent_id: Optional[str] = Query(None),
    agentId: Optional[str] = Query(None),
    value: Optional[str] = Query(None),
    data: Optional[ResponseData] = None,
):
  s_id = session_id or sessionId
  a_id = agent_id or agentId

  if not s_id or s_id not in sessions:
    raise HTTPException(status_code=404, detail="Session not found")

  session = sessions[s_id]

  # Resolve value
  resp_value = value
  if not resp_value and data:
    if data.seat:
      resp_value = data.seat
    elif data.time:
      resp_value = data.time
    elif data.tickets:
      resp_value = data.tickets
    elif data.people:
      resp_value = data.people
    elif data.confirmed:
      resp_value = "Confirmed"

  if not resp_value:
    try:
      body = await request.json()
      resp_value = (
          body.get("seat")
          or body.get("time")
          or body.get("tickets")
          or body.get("selectedOption")
          or body.get("value")
          or ("Confirmed" if body.get("confirmed") else None)
      )
    except Exception:
      pass

  if not resp_value:
    resp_value = "Confirmed"

  # Find the paused future
  fut = session.pause_events.get(a_id) if a_id else None
  if not fut:
    for k, v in session.pause_events.items():
      if not v.done():
        fut = v
        break

  if fut and not fut.done():
    fut.set_result(resp_value)
    return {"status": "success", "message": "User response delivered"}

  return {"status": "accepted", "message": "No active pause found"}


@app.get("/stream")
def get_stream_token(auth_only: bool = False):
  """Returns an OIDC token or local endpoint for streaming access."""
  audience = os.environ.get(
      "CLOUD_RUN_AUDIENCE",
      "https://jetpacker-server-254502043090.us-central1.run.app",
  )
  url = (
      "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience="
      + audience
  )

  try:
    req = urllib.request.Request(url)
    req.add_header("Metadata-Flavor", "Google")
    token = urllib.request.urlopen(req, timeout=2).read().decode("utf-8")
    return {"token": token, "url": f"{audience}/run_sse"}
  except Exception:
    return {"token": "local_dev_token", "url": "http://localhost:8000/run_sse"}


@app.get("/health")
def health():
  return {"status": "healthy", "service": "jetpacker-a2ui-booking-server"}


if __name__ == "__main__":
  uvicorn.run(app, host="0.0.0.0", port=8000)
