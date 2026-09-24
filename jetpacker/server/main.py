import asyncio
import json
from typing import AsyncGenerator
import urllib.request

import fastapi
from google.adk.agents.base_agent import BaseAgent
from google.adk.agents.invocation_context import InvocationContext
from google.adk.agents.parallel_agent import ParallelAgent
from google.adk.cli.fast_api import get_fast_api_app
from google.adk.cli.utils.base_agent_loader import BaseAgentLoader
from google.adk.events.event import Event
from google.adk.events.event_actions import EventActions
from google import genai
from google.genai import types
import pydantic
import uvicorn

app = fastapi.FastAPI()


class SessionState:

  def __init__(self):
    self.pause_events: dict[str, asyncio.Event] = {}
    self.results: dict[str, any] = {}
    self.completed_steps: dict[str, int] = {}


sessions: dict[str, SessionState] = {}


class BaseSimulationAgent(BaseAgent):

  async def _generate_text(self, prompt: str) -> str:
    client = genai.Client(vertexai=True)
    response = await asyncio.to_thread(
        client.models.generate_content,
        model="gemini-2.5-flash",
        contents=prompt,
    )
    text = response.text or ""
    return text.strip()

  def _create_text_event(
      self, ctx: InvocationContext, text: str, author: str | None = None
  ) -> Event:
    return Event(
        invocation_id=ctx.invocation_id,
        author=author or self.name,
        content=types.Content(parts=[types.Part(text=text)]),
        branch=ctx.branch,
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


class SingleFlightSimulationAgent(BaseSimulationAgent):
  name: str = "single_flight"
  item_title: str = "Flight"

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title

    # Step 1: Time Confirmation
    if session.completed_steps.get(title, 0) < 1:
      confirmed = False
      while not confirmed:
        time_proposal = await self._generate_text(
            f"Propose a single logical departure time for the flight '{title}'"
            " (assume origin is France). Keep it under 10 words."
        )

        yield self._create_options_event(
            ctx,
            title,
            options=[f"Confirm {time_proposal}", "Try Another Time"],
            message=(
                f"I found a flight at {time_proposal}. Please confirm or"
                " request another time."
            ),
            ui_type="time_selection",
        )

        if title not in session.pause_events:
          session.pause_events[title] = asyncio.Event()
        else:
          session.pause_events[title].clear()

        await session.pause_events[title].wait()

        response = session.results.get(title)
        if response and "Confirm" in response:
          confirmed = True
        else:
          session.results[title] = None
          yield self._create_text_event(
              ctx, "Searching for alternative flight times...", author=title
          )
          await asyncio.sleep(2)
      session.completed_steps[title] = 1

    # Step 2: Seat Selection
    if session.completed_steps.get(title, 0) < 2:
      yield self._create_options_event(
          ctx,
          title,
          options=["1A", "1B", "2A", "2B"],
          message=f"Flight time confirmed! Please select a seat for {title}.",
      )

      session.pause_events[title].clear()
      await session.pause_events[title].wait()

      selected_seat = session.results.get(title) or "Unknown"

      confirmed_text = await self._generate_text(
          "Generate a SINGLE realistic status line for confirming the flight"
          f" '{title}' with seat {selected_seat}. Do NOT include flight details"
          " in the output, just the status."
      )
      yield self._create_text_event(ctx, confirmed_text, author=title)
      yield self._create_end_event(ctx, author=title)
      await asyncio.sleep(1)
      session.completed_steps[title] = 2


class FlightSimulationAgent(BaseSimulationAgent):
  name: str = "flight"
  items: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleFlightSimulationAgent(
            name=f"single_flight_{i}",
            item_title=item.get("title", "Flight"),
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(name="flights_parallel", sub_agents=sub_agents)

    async for event in parallel_agent.run_async(ctx):
      yield event


class SingleHotelSimulationAgent(BaseSimulationAgent):
  name: str = "single_hotel"
  item_title: str = "Hotel"
  full_itinerary: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title

    prompt = f"""
    You are a travel agent booking a hotel.
    Generate a brief justification (1-2 sentences) for choosing the hotel '{title}' for the user.
    Mention that you are booking it for the correct number of nights based on the dates (assume 3 nights if dates are missing).
    Explain why you chose it (e.g., proximity to other locations in the itinerary, reviews, or user preferences).
    Context: Full itinerary is {self.full_itinerary}
    """
    justification = await self._generate_text(prompt)

    status_text = f"{justification} Please confirm reservation."
    yield self._create_text_event(ctx, status_text, author=title)

    # HITL Pause
    pause_key = title
    if pause_key not in session.pause_events:
      session.pause_events[pause_key] = asyncio.Event()

    await session.pause_events[pause_key].wait()

    response = session.results.get(pause_key)
    if response == "Confirmed":
      confirmed_text = await self._generate_text(
          "Generate a SINGLE realistic status line for confirming the hotel"
          f" reservation for '{title}'. Do NOT include the hotel name in the"
          " output, just the status (e.g., 'Reservation confirmed', 'Room"
          " secured')."
      )
      yield self._create_text_event(ctx, confirmed_text, author=title)
    else:
      yield self._create_text_event(
          ctx, f"Reservation for '{title}' was not confirmed.", author=title
      )

    yield self._create_end_event(ctx, author=title)
    await asyncio.sleep(1)


class HotelSimulationAgent(BaseSimulationAgent):
  name: str = "hotel"
  items: list[dict] = []
  full_itinerary: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleHotelSimulationAgent(
            name=f"single_hotel_{i}",
            item_title=item.get("title", "Hotel"),
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(name="hotels_parallel", sub_agents=sub_agents)

    async for event in parallel_agent.run_async(ctx):
      yield event


class SingleMuseumSimulationAgent(BaseSimulationAgent):
  name: str = "single_museum"
  item_title: str = "Museum"
  full_itinerary: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title

    prompt = f"""
    You are a travel agent booking museum tickets.
    Generate a brief justification (1-2 sentences) for reserving tickets for the museum '{title}'.
    Propose a logical time slot for the visit (e.g., morning or afternoon) and ask user to confirm.
    Explain why you chose it (e.g., proximity to other events in the itinerary, popularity, or specific exhibitions).
    Context: Full itinerary is {self.full_itinerary}
    """
    justification = await self._generate_text(prompt)

    status_text = f"{justification} Please confirm tickets."
    yield self._create_text_event(ctx, status_text, author=title)

    # HITL Pause
    pause_key = title
    if pause_key not in session.pause_events:
      session.pause_events[pause_key] = asyncio.Event()

    await session.pause_events[pause_key].wait()

    response = session.results.get(pause_key)
    if response == "Confirmed":
      confirmed_text = await self._generate_text(
          "Generate a SINGLE realistic status line for confirming tickets for"
          f" '{title}'. Do NOT include the museum name in the output, just the"
          " status (e.g., 'Tickets confirmed', 'Reservation secured')."
      )
      yield self._create_text_event(ctx, confirmed_text, author=title)
    else:
      yield self._create_text_event(
          ctx, f"Tickets for '{title}' were not confirmed.", author=title
      )

    await asyncio.sleep(1)
    yield self._create_end_event(ctx, author=title)


class MuseumSimulationAgent(BaseSimulationAgent):
  name: str = "museum"
  items: list[dict] = []
  full_itinerary: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleMuseumSimulationAgent(
            name=f"single_museum_{i}",
            item_title=item.get("title", "Museum"),
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(name="museums_parallel", sub_agents=sub_agents)

    async for event in parallel_agent.run_async(ctx):
      yield event

    yield self._create_end_event(ctx, author=self.name)


class SingleRestaurantSimulationAgent(BaseSimulationAgent):
  name: str = "single_restaurant"
  item_title: str = "Restaurant"
  full_itinerary: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    session = sessions[ctx.session.id]
    title = self.item_title

    prompt = f"""
    You are a travel agent booking a restaurant.
    Generate a brief justification (1-2 sentences) for choosing the restaurant '{title}' for the user.
    Explain why you chose it (e.g., proximity to other locations in the itinerary, cuisine, or reviews).
    Context: Full itinerary is {self.full_itinerary}
    """
    justification = await self._generate_text(prompt)

    # Ask for people count using options UI
    yield self._create_options_event(
        ctx,
        title,
        options=["1", "2", "3", "4+"],
        message=(
            f"{justification} Please select the number of people for the"
            " reservation."
        ),
        ui_type="ticket_selection",
    )

    if title not in session.pause_events:
      session.pause_events[title] = asyncio.Event()
    else:
      session.pause_events[title].clear()

    await session.pause_events[title].wait()

    people_count = session.results.get(title) or "Unknown"

    confirmed_text = await self._generate_text(
        "Generate a SINGLE realistic status line for confirming a table for"
        f" {people_count} people at '{title}'. Do NOT include the restaurant"
        " name in the output, just the status (e.g., 'Table reserved for"
        f" {people_count}', 'Reservation secured')."
    )
    yield self._create_text_event(ctx, confirmed_text, author=title)
    yield self._create_end_event(ctx, author=title)
    await asyncio.sleep(1)


class RestaurantSimulationAgent(BaseSimulationAgent):
  name: str = "restaurant"
  items: list[dict] = []
  full_itinerary: list[dict] = []

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if not self.items:
      return

    sub_agents = [
        SingleRestaurantSimulationAgent(
            name=f"single_restaurant_{i}",
            item_title=item.get("title", "Restaurant"),
            full_itinerary=self.full_itinerary,
        )
        for i, item in enumerate(self.items)
    ]
    parallel_agent = ParallelAgent(
        name="restaurants_parallel", sub_agents=sub_agents
    )

    async for event in parallel_agent.run_async(ctx):
      yield event


class ItineraryOrchestratorAgent(BaseSimulationAgent):
  name: str = "booking"

  async def _run_async_impl(
      self, ctx: InvocationContext
  ) -> AsyncGenerator[Event, None]:
    if ctx.session.id not in sessions:
      sessions[ctx.session.id] = SessionState()

    user_text = ""
    if ctx.user_content and ctx.user_content.parts:
      user_text = ctx.user_content.parts[0].text

    try:
      data = json.loads(user_text)
      itinerary = data.get("itinerary", [])
      print(f"DEBUG: Parsed itinerary: {itinerary}")
    except json.JSONDecodeError:
      yield self._create_text_event(
          ctx, "Failed to parse itinerary. Running all agents as default."
      )
      itinerary = [
          {"type": "TRANSPORTATION"},
          {"type": "CULTURE"},
          {"type": "FOOD_AND_DRINK"},
      ]
      print("DEBUG: Failed to parse itinerary, using default.")

    sub_agents = []

    # Handle Hotels group
    hotel_items = [e for e in itinerary if e.get("type") == "ACCOMMODATION"]
    if hotel_items:
      sub_agents.append(HotelSimulationAgent(items=hotel_items, full_itinerary=itinerary))

    # Handle Museums group
    museum_items = [
        e for e in itinerary if e.get("type") in ["CULTURE", "ACTIVITY"]
    ]
    if museum_items:
      sub_agents.append(MuseumSimulationAgent(items=museum_items, full_itinerary=itinerary))

    # Handle Restaurants group
    restaurant_items = [
        e for e in itinerary if e.get("type") == "FOOD_AND_DRINK"
    ]
    if restaurant_items:
      sub_agents.append(RestaurantSimulationAgent(items=restaurant_items, full_itinerary=itinerary))

    # Handle Flights group
    flight_items = [e for e in itinerary if e.get("type") == "TRANSPORTATION"]
    if flight_items:
      sub_agents.append(FlightSimulationAgent(items=flight_items))

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


class BookingAgentLoader(BaseAgentLoader):

  def load_agent(self, agent_name: str) -> BaseAgent:
    if agent_name == "booking":
      return ItineraryOrchestratorAgent()
    raise ValueError(f"Unknown agent: {agent_name}")

  def list_agents(self) -> list[str]:
    return ["booking"]


app = get_fast_api_app(
    agents_dir=".",
    agent_loader=BookingAgentLoader(),
    web=False,
    auto_create_session=True,
)


class ResponseData(pydantic.BaseModel):
  seat: str | None = None
  time: str | None = None
  tickets: str | None = None
  people: str | None = None
  confirmed: bool | None = None


@app.post("/respond")
async def respond(session_id: str, data: ResponseData, agent_id: str | None = None):
  if session_id not in sessions:
    raise fastapi.HTTPException(status_code=404, detail="Session not found")

  session = sessions[session_id]
  
  # For backward compatibility or default flight use flight_Flight
  pause_key = agent_id or "flight_Flight"
  
  if data.seat:
    session.results[pause_key] = data.seat
  elif data.time:
    session.results[pause_key] = data.time
  elif data.tickets:
    session.results[pause_key] = data.tickets
  elif data.people:
    session.results[pause_key] = data.people
  elif data.confirmed:
    session.results[pause_key] = "Confirmed"

  if pause_key in session.pause_events:
    session.pause_events[pause_key].set()
  else:
    raise fastapi.HTTPException(status_code=404, detail=f"Pause for {pause_key} not active")

  return {"status": "success", "message": "User response received"}


for route in app.routes:
  path = getattr(route, "path", "N/A")
  methods = getattr(route, "methods", "N/A")
  print(f"DEBUG: Bound route: {path} methods: {methods}")


@app.get("/stream")
def get_stream_token(auth_only: bool = False):
  """Returns an OIDC token for direct streaming access."""
  audience = "https://jetpacker-server-254502043090.us-central1.run.app"
  url = f"http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience={audience}"

  try:
    req = urllib.request.Request(url)
    req.add_header("Metadata-Flavor", "Google")
    token = urllib.request.urlopen(req).read().decode("utf-8")
    return {"token": token, "url": f"{audience}/run_sse"}
  except Exception as e:
    raise fastapi.HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
  uvicorn.run(app, host="0.0.0.0", port=8000)
