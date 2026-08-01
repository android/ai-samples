import asyncio
import json
import uuid
import os
import traceback
import re
from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import StreamingResponse
from typing import Dict, List, Optional, Any

from google.adk import Agent
from google.adk.runners import InMemoryRunner
from google.adk.tools import FunctionTool
from google.adk.tools.tool_context import ToolContext
from google.genai import types as genai_types

from a2ui.schema.manager import A2uiSchemaManager
from a2ui.schema.constants import VERSION_0_9
from a2ui.basic_catalog.provider import BasicCatalog
from a2ui.schema.catalog import CatalogConfig

def sanitize_agent_name(title: str) -> str:
    sanitized = re.sub(r'[^a-zA-Z0-9_]', '_', title)
    if not re.match(r'^[a-zA-Z_]', sanitized):
        sanitized = '_' + sanitized
    return sanitized

app = FastAPI()

# A2UI Catalog Specification System Prompt
schema_manager = A2uiSchemaManager(
    version=VERSION_0_9,
    catalogs=[
        BasicCatalog.get_config(version=VERSION_0_9),
        CatalogConfig.from_path(
            name="https://example.com/catalogs/booking_assistant/v1/catalog.json",
            catalog_path="booking_catalog.json"
        )
    ]
)

class SessionState:
    def __init__(self):
        self.pause_events: Dict[str, asyncio.Future] = {}
        self.queue: asyncio.Queue = asyncio.Queue()
        self.bg_tasks: List[asyncio.Task] = []

sessions: Dict[str, SessionState] = {}

# Custom A2UI Function Tools for ADK Agents

from pydantic import BaseModel, Field

A2UI_SYSTEM_INSTRUCTION = schema_manager.generate_system_prompt(
    role_description="You are a helpful travel booking assistant.",
    ui_description="Use InteractiveOptionPicker for flight/hotel/museum/restaurant booking choices, SeatSelectionPicker for flight seats, and BookingStatus for confirmed status.",
    include_schema=True,
    include_examples=True,
    allowed_components=["InteractiveOptionPicker", "SeatSelectionPicker", "BookingStatus"]
).replace("${expression}", "$(expression)")

class ComponentProperties(BaseModel):
    prompt: Optional[str] = Field(default=None, description="Header prompt/choice text for picker widgets.")
    options: Optional[List[str]] = Field(default=None, description="Custom option labels for InteractiveOptionPicker.")
    selectedIdx: Optional[int] = Field(default=None, description="Currently selected option index (0-indexed).")
    confirmBtnText: Optional[str] = Field(default=None, description="Label for picker confirmation button.")
    seats: Optional[List[str]] = Field(default=None, description="Seat labels list for SeatSelectionPicker.")
    selectedSeat: Optional[str] = Field(default=None, description="Currently selected seat label.")
    text: Optional[str] = Field(default=None, description="Booking success status description text.")

class A2uiComponent(BaseModel):
    id: str = Field(description="Must be 'root' for the main element in the surface card.")
    component: str = Field(description="The component name (e.g. 'InteractiveOptionPicker', 'SeatSelectionPicker', 'BookingStatus').")
    properties: ComponentProperties = Field(description="The properties for the component.")

def make_update_ui(surface_id: str, session: SessionState):
    async def update_ui(
        components: List[A2uiComponent],
        tool_context: ToolContext = None
    ) -> str:
        """Updates the UI surface with the specified components and waits for user interaction if interactive.
        
        Returns the user's choice (e.g. selected option or seat label), or empty string if not interactive.
        """
        # Convert Pydantic A2uiComponent objects to standard dictionaries to send, excluding None
        components_dict = [c.model_dump(exclude_none=True) for c in components]
        
        option_picker = next((c for c in components if c.component == "InteractiveOptionPicker"), None)
        seat_picker = next((c for c in components if c.component == "SeatSelectionPicker"), None)
        
        if option_picker:
            comp_dict = next(c for c in components_dict if c["component"] == "InteractiveOptionPicker")
            options = option_picker.properties.options or []
            selected_idx = option_picker.properties.selectedIdx
            confirmed = False
            
            while not confirmed:
                comp_dict["properties"]["selectedIdx"] = selected_idx
                await session.queue.put({
                    "updateComponents": {
                        "surfaceId": surface_id,
                        "components": components_dict
                    }
                })
                
                fut = asyncio.get_event_loop().create_future()
                session.pause_events[surface_id] = fut
                response = await fut
                
                if response.startswith("SelectOption_"):
                    selected_idx = int(response.removeprefix("SelectOption_"))
                elif response.startswith("ConfirmSelection_"):
                    confirmed = True
                    
            return options[selected_idx] if selected_idx is not None and selected_idx < len(options) else ""
            
        elif seat_picker:
            comp_dict = next(c for c in components_dict if c["component"] == "SeatSelectionPicker")
            selected_seat = seat_picker.properties.selectedSeat
            seat_confirmed = False
            
            while not seat_confirmed:
                comp_dict["properties"]["selectedSeat"] = selected_seat
                await session.queue.put({
                    "updateComponents": {
                        "surfaceId": surface_id,
                        "components": components_dict
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
                    "components": components_dict
                }
            })
            return ""
            
    return update_ui

# Flight Agent Coroutine
async def run_flight_agent(title: str, session: SessionState, thread_id: str):
    try:
        print(f"AGENT [Flight - '{title}']: started")
        await session.queue.put({"createSurface": {"surfaceId": title, "catalogId": "https://example.com/catalogs/booking_assistant/v1/catalog.json"}})
        
        agent = Agent(
            name=sanitize_agent_name(title),
            model="gemini-3.1-flash-lite",
            instruction=f"Book a flight for: '{title}'. Use the update_ui tool to ask the user to select flight time and seats. Finally use update_ui to show booking confirmation status.\n\n{A2UI_SYSTEM_INSTRUCTION}",
            tools=[
                FunctionTool(make_update_ui(title, session))
            ]
        )
        
        agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
        runner = InMemoryRunner(agent, app_name="BookingServer")
        await runner.session_service.create_session(
            app_name="BookingServer",
            user_id=thread_id,
            session_id=agent_session_id
        )
        
        async for _ in runner.run_async(
            new_message=genai_types.Content(parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]),
            user_id=thread_id,
            session_id=agent_session_id
        ):
            pass
            
    except Exception as e:
        print(f"AGENT [Flight - '{title}'] Error: {e}")
        traceback.print_exc()

# Hotel Agent Coroutine
async def run_hotel_agent(title: str, session: SessionState, thread_id: str):
    try:
        print(f"AGENT [Hotel - '{title}']: started")
        await session.queue.put({"createSurface": {"surfaceId": title, "catalogId": "https://example.com/catalogs/booking_assistant/v1/catalog.json"}})
        
        agent = Agent(
            name=sanitize_agent_name(title),
            model="gemini-3.1-flash-lite",
            instruction=f"Book a hotel for: '{title}'. Use the update_ui tool to ask the user to choose a lodging room. Finally use update_ui to show booking confirmation status.\n\n{A2UI_SYSTEM_INSTRUCTION}",
            tools=[
                FunctionTool(make_update_ui(title, session))
            ]
        )
        
        agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
        runner = InMemoryRunner(agent, app_name="BookingServer")
        await runner.session_service.create_session(
            app_name="BookingServer",
            user_id=thread_id,
            session_id=agent_session_id
        )
        
        async for _ in runner.run_async(
            new_message=genai_types.Content(parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]),
            user_id=thread_id,
            session_id=agent_session_id
        ):
            pass
            
    except Exception as e:
        print(f"AGENT [Hotel - '{title}'] Error: {e}")
        traceback.print_exc()

# Museum Agent Coroutine
async def run_museum_agent(title: str, session: SessionState, thread_id: str):
    try:
        print(f"AGENT [Museum - '{title}']: started")
        await session.queue.put({"createSurface": {"surfaceId": title, "catalogId": "https://example.com/catalogs/booking_assistant/v1/catalog.json"}})
        
        agent = Agent(
            name=sanitize_agent_name(title),
            model="gemini-3.1-flash-lite",
            instruction=f"Book museum tickets for: '{title}'. Use the update_ui tool to ask the user to choose entry ticket options. Finally use update_ui to show booking confirmation status.\n\n{A2UI_SYSTEM_INSTRUCTION}",
            tools=[
                FunctionTool(make_update_ui(title, session))
            ]
        )
        
        agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
        runner = InMemoryRunner(agent, app_name="BookingServer")
        await runner.session_service.create_session(
            app_name="BookingServer",
            user_id=thread_id,
            session_id=agent_session_id
        )
        
        async for _ in runner.run_async(
            new_message=genai_types.Content(parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]),
            user_id=thread_id,
            session_id=agent_session_id
        ):
            pass
            
    except Exception as e:
        print(f"AGENT [Museum - '{title}'] Error: {e}")
        traceback.print_exc()

# Restaurant Agent Coroutine
async def run_restaurant_agent(title: str, session: SessionState, thread_id: str):
    try:
        print(f"AGENT [Restaurant - '{title}']: started")
        await session.queue.put({"createSurface": {"surfaceId": title, "catalogId": "https://example.com/catalogs/booking_assistant/v1/catalog.json"}})
        
        agent = Agent(
            name=sanitize_agent_name(title),
            model="gemini-3.1-flash-lite",
            instruction=f"Book a table reservation for restaurant: '{title}'. Use the update_ui tool to ask the user to choose a table size option. Finally use update_ui to show booking confirmation status.\n\n{A2UI_SYSTEM_INSTRUCTION}",
            tools=[
                FunctionTool(make_update_ui(title, session))
            ]
        )
        
        agent_session_id = f"{thread_id}_{sanitize_agent_name(title)}"
        runner = InMemoryRunner(agent, app_name="BookingServer")
        await runner.session_service.create_session(
            app_name="BookingServer",
            user_id=thread_id,
            session_id=agent_session_id
        )
        
        async for _ in runner.run_async(
            new_message=genai_types.Content(parts=[genai_types.Part.from_text(text=f"Please book: '{title}'")]),
            user_id=thread_id,
            session_id=agent_session_id
        ):
            pass
            
    except Exception as e:
        print(f"AGENT [Restaurant - '{title}'] Error: {e}")
        traceback.print_exc()


@app.post("/")
async def root(input_data: dict):
    thread_id = input_data.get("threadId", "")
    run_id = input_data.get("runId", "")
    
    if thread_id in sessions:
        old_session = sessions[thread_id]
        print(f"SERVER: Cleaning up old background tasks for sessionId={thread_id}")
        for task in old_session.bg_tasks:
            if not task.done():
                task.cancel()
                
    session = SessionState()
    sessions[thread_id] = session
    
    # Extract itinerary events
    last_user_msg = None
    for msg in reversed(input_data.get("messages", [])):
        role = msg.get("role") or msg.get("messageRole")
        if role == "user":
            last_user_msg = msg
            break
            
    user_text = ""
    if last_user_msg:
        content = last_user_msg.get("content")
        content_parts = last_user_msg.get("contentParts")
        if content:
            if isinstance(content, list):
                for part in content:
                    if isinstance(part, dict) and part.get("type") == "text":
                        user_text += part.get("text", "")
                    elif isinstance(part, str):
                        user_text += part
            elif isinstance(content, str):
                user_text = content
        elif content_parts:
            user_text = "".join((part.get("text", "") if isinstance(part, dict) else part.text) for part in content_parts)
            
    itinerary_items = []
    try:
        data = json.loads(user_text)
        itinerary_items = data.get("events", [])
    except Exception as e:
        print("Failed to parse itinerary JSON:", e)
        
    async def event_generator():
        print(f"SERVER [threadId={thread_id}]: Starting event generator stream")
        # 1. Run Started
        yield f"event: RUN_STARTED\ndata: {json.dumps({'type': 'RUN_STARTED', 'threadId': thread_id, 'runId': run_id})}\n\n"
        
        # 2. Start sub-agents concurrently
        tasks = []
        for item in itinerary_items:
            title = item.get("title", "Booking")
            type_val = item.get("type")
            if type_val == "TRANSPORTATION":
                tasks.append(run_flight_agent(title, session, thread_id))
            elif type_val == "LODGING" or type_val == "ACCOMMODATION":
                tasks.append(run_hotel_agent(title, session, thread_id))
            elif type_val in ["CULTURE", "ACTIVITY"]:
                tasks.append(run_museum_agent(title, session, thread_id))
            elif type_val == "FOOD_AND_DRINK":
                tasks.append(run_restaurant_agent(title, session, thread_id))
                
        # Run sub-agents in background
        async def run_all_and_signal():
            try:
                if tasks:
                    await asyncio.gather(*tasks)
                print(f"SERVER [threadId={thread_id}]: All sub-agent tasks completed successfully")
            except Exception as gather_err:
                print(f"SERVER [threadId={thread_id}]: Exception in gather:")
                traceback.print_exc()
            finally:
                await session.queue.put("__DONE__")
            
        bg_task = asyncio.create_task(run_all_and_signal())
        session.bg_tasks.append(bg_task)
        
        # 3. Read from session queue and yield to client
        while True:
            try:
                payload = await asyncio.wait_for(session.queue.get(), timeout=15.0)
                print(f"SERVER [threadId={thread_id}]: Collected queue payload: {payload}")
                if payload == "__DONE__":
                    print(f"SERVER [threadId={thread_id}]: Collected __DONE__ signal, ending generator stream")
                    break
                    
                message_id = str(uuid.uuid4())
                
                # Emit text message start
                yield f"event: TEXT_MESSAGE_START\ndata: {json.dumps({'type': 'TEXT_MESSAGE_START', 'messageId': message_id, 'role': 'assistant'})}\n\n"
                
                # Emit JSON payload as content
                payload_str = json.dumps(payload)
                yield f"event: TEXT_MESSAGE_CONTENT\ndata: {json.dumps({'type': 'TEXT_MESSAGE_CONTENT', 'messageId': message_id, 'delta': payload_str})}\n\n"
                
                # Emit text message end
                yield f"event: TEXT_MESSAGE_END\ndata: {json.dumps({'type': 'TEXT_MESSAGE_END', 'messageId': message_id})}\n\n"
            except asyncio.TimeoutError:
                # Send keep-alive ping
                print(f"SERVER [threadId={thread_id}]: Sending keep-alive ping")
                yield ": keep-alive\n\n"
                
        # 4. Run Finished
        yield f"event: RUN_FINISHED\ndata: {json.dumps({'type': 'RUN_FINISHED', 'threadId': thread_id, 'runId': run_id, 'outcome': {'type': 'success'}})}\n\n"
        print(f"SERVER [threadId={thread_id}]: Generator stream finished")
        
    return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/respond")
async def respond(sessionId: str = Query(...), agentId: str = Query(...), value: str = Query(...)):
    print(f"SERVER: Received /respond - sessionId={sessionId}, agentId={agentId}, value={value}")
    session = sessions.get(sessionId)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
        
    fut = session.pause_events.get(agentId)
    if fut and not fut.done():
        fut.set_result(value)
        return {"status": "success"}
        
    raise HTTPException(status_code=404, detail="Pause event not found")
