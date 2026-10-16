from __future__ import annotations

import sys
from typing import Any


def __getattr__(name: str) -> Any:
  if name == "root_agent":
    main_mod = sys.modules.get("main") or (
        sys.modules.get("__main__")
        if hasattr(sys.modules.get("__main__"), "ItineraryOrchestratorAgent")
        else None
    )
    if main_mod is not None:
      return main_mod.ItineraryOrchestratorAgent()
    import main

    return main.ItineraryOrchestratorAgent()
  raise AttributeError(f"module '{__name__}' has no attribute '{name}'")
