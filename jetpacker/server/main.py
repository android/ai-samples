from __future__ import annotations

import inspect
import os
from typing import Any
import urllib.request

import fastapi
from google.adk.cli.fast_api import get_fast_api_app
from google.adk.cli.utils.base_agent_loader import BaseAgentLoader
import uvicorn

from agents.base import SessionState, sessions
from agents.orchestrator import ItineraryOrchestratorAgent
from schemas import (
    ActivityOrDiningProposal,
    FlightConfigProposal,
    FlightOptionSchema,
    HotelConfigProposal,
    HotelRoomOptionSchema,
    ResponseData,
)


class BookingAgentLoader(BaseAgentLoader):

  def load_agent(self, agent_name: str) -> Any:
    return ItineraryOrchestratorAgent()


# Inspect ADK's get_fast_api_app signature to support both old and new signatures
_adk_sig = inspect.signature(get_fast_api_app)
_adk_kwargs = {}
if "agents_dir" in _adk_sig.parameters:
  _adk_kwargs["agents_dir"] = os.path.dirname(os.path.abspath(__file__))
if "session_service_uri" in _adk_sig.parameters:
  _adk_kwargs["session_service_uri"] = None
if "loader" in _adk_sig.parameters:
  _adk_kwargs["loader"] = BookingAgentLoader()

app = get_fast_api_app(**_adk_kwargs)


@app.post("/respond")
@app.post("/v2/respond")
async def respond(
    session_id: str, data: ResponseData, agent_id: str | None = None
) -> dict[str, str]:
  if session_id not in sessions:
    raise fastapi.HTTPException(status_code=404, detail="Session not found")

  session = sessions[session_id]
  pause_key = agent_id or "flight_Flight"

  # Store structured payload so A2UI v0.9 agents can read clientDataModel,
  # while also supporting legacy string expectations.
  if (
      data.clientDataModel is not None
      or data.selectionType
      in (
          "time",
          "confirm_flight_config",
          "hotel_room",
          "confirm_hotel_config",
      )
      or data.selectedFlightId is not None
      or data.selectedRoomId is not None
      or data.selectedSeat is not None
      or data.checkInTime is not None
      or data.floorPreference is not None
      or data.priorityBoarding is not None
      or data.extraBag is not None
      or data.breakfastIncluded is not None
      or data.lateCheckout is not None
  ):
    session.results[pause_key] = data.model_dump(exclude_none=True)
  elif data.seat:
    session.results[pause_key] = data.seat
  elif data.selectedSeat:
    session.results[pause_key] = data.selectedSeat
  elif data.time:
    session.results[pause_key] = data.time
  elif data.selectedOption:
    session.results[pause_key] = data.selectedOption
  else:
    session.results[pause_key] = "confirmed"

  if pause_key in session.pause_events:
    session.pause_events[pause_key].set()
  else:
    raise fastapi.HTTPException(
        status_code=404, detail=f"Pause for {pause_key} not active"
    )

  return {"status": "success", "message": "User response received"}


@app.get("/stream")
@app.get("/v2/stream")
def get_stream_token(auth_only: bool = False) -> dict[str, str]:
  """Returns an OIDC token for direct streaming access."""
  del auth_only  # Unused parameter kept for gateway compatibility
  audience = os.environ.get(
      "CLOUD_RUN_AUDIENCE",
      "https://YOUR_CLOUD_RUN_SERVICE_URL",
  )
  url = (
      "http://metadata.google.internal/computeMetadata/v1/instance/"
      f"service-accounts/default/identity?audience={audience}"
  )

  try:
    req = urllib.request.Request(url)
    req.add_header("Metadata-Flavor", "Google")
    token = urllib.request.urlopen(req, timeout=1.5).read().decode("utf-8")
    return {"token": token, "url": f"{audience}/run_sse"}
  except Exception as e:
    # If metadata server fails or running unauthenticated/local, provide fallback
    return {"token": "dev_stream_token", "url": f"{audience}/run_sse"}


if __name__ == "__main__":
  uvicorn.run(app, host="0.0.0.0", port=8000)
