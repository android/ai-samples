from __future__ import annotations

import sys
from typing import Any


def __getattr__(name: str) -> Any:
  if name == "root_agent":
    if "main" in sys.modules:
      return sys.modules["main"].ItineraryOrchestratorAgent()
    import main

    return main.ItineraryOrchestratorAgent()
  raise AttributeError(f"module '{__name__}' has no attribute '{name}'")
