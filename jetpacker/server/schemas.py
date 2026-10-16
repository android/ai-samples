from __future__ import annotations

from typing import Any
import pydantic


class FlightOptionSchema(pydantic.BaseModel):
  id: str
  flightNumber: str
  departureTime: str
  arrivalTime: str
  duration: str
  fare: int
  badge: str
  aircraft: str


class FlightConfigProposal(pydantic.BaseModel):
  originCode: str
  originCity: str
  destCode: str
  destCity: str
  airline: str
  flightNumber: str
  aircraftName: str
  duration: str
  baseFare: int
  gate: str
  terminal: str
  boardingGroup: str
  rationale: str
  occupiedSeats: list[str]
  contestedSeat: str
  options: list[FlightOptionSchema]


class HotelRoomOptionSchema(pydantic.BaseModel):
  id: str
  name: str
  amenities: str
  rate: int
  badge: str


class HotelConfigProposal(pydantic.BaseModel):
  hotelName: str
  locationLabel: str
  checkInDate: str
  checkOutDate: str
  nights: int
  rationale: str
  liveUpdateAlert: str
  roomNumber: str
  confirmationCode: str
  suites: list[HotelRoomOptionSchema]


class ActivityOrDiningProposal(pydantic.BaseModel):
  prompt: str
  options: list[str]
  holdConfirmationPrompt: str
  confirmedDescription: str


class ResponseData(pydantic.BaseModel):
  model_config = pydantic.ConfigDict(extra="allow")

  seat: str | None = None
  selectedSeat: str | None = None
  time: str | None = None
  selectedOption: str | None = None
  selectedFlightId: str | None = None
  selectedRoomId: str | None = None
  checkInTime: str | None = None
  floorPreference: str | None = None
  breakfastIncluded: bool | None = None
  lateCheckout: bool | None = None
  confirmed: bool | None = None
  selectionType: str | None = None
  priorityBoarding: bool | None = None
  extraBag: bool | None = None
  totalFare: int | None = None
  clientDataModel: dict[str, Any] | None = None
