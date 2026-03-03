import os
import json
from firebase_admin import initialize_app, app_check
from firebase_functions import https_fn, options
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
from google import genai
from google.genai import types

initialize_app()

# 1. Internal Data (Fake Database)
USER_DB = {
    "user123": {"base_budget": 50.0, "role": "Senior Lead"}
}

# 2. Server-Side Tools
def get_private_budget(user_id: str) -> float:
    """Returns the user's private base budget."""
    return USER_DB.get(user_id, {}).get("base_budget", 0.0)

def check_travel_policy(city: str) -> float:
    """Returns an extra allowance based on the travel city."""
    if city.lower() == "san francisco":
        return 25.0
    elif city.lower() == "new york":
        return 20.0
    return 0.0

# 3. Tool Declarations for the Agent
server_tools = [
     types.FunctionDeclaration(
        name="get_private_budget",
        description="Returns the user's private base budget.",
        parameters=types.Schema(
            type=types.Type.OBJECT,
            properties={"user_id": types.Schema(type=types.Type.STRING)},
            required=["user_id"]
        )
    ),
    types.FunctionDeclaration(
        name="check_travel_policy",
        description="Returns an extra allowance based on the travel city.",
        parameters=types.Schema(
            type=types.Type.OBJECT,
            properties={"city": types.Schema(type=types.Type.STRING)},
            required=["city"]
        )
    )
]

client_tools = [
    types.FunctionDeclaration(
        name="get_device_location",
        description="Gets the current location of the user's Android device (city name).",
        parameters=types.Schema(type=types.Type.OBJECT, properties={})
    ),
    types.FunctionDeclaration(
        name="request_user_approval",
        description="Requests approval from the user on their device before finalizing.",
        parameters=types.Schema(
            type=types.Type.OBJECT,
            properties={"message": types.Schema(type=types.Type.STRING, description="The message to show to the user")},
            required=["message"]
        )
    ),
    types.FunctionDeclaration(
        name="open_maps",
        description="Opens the maps application to search for a location.",
        parameters=types.Schema(
            type=types.Type.OBJECT,
            properties={"location_name": types.Schema(type=types.Type.STRING)},
            required=["location_name"]
        )
    )
]

all_tools = [types.Tool(function_declarations=client_tools + server_tools)]

system_instruction = """
You are an Agentic Expense Auditor.
1. When a user asks about an expense, first look up their private budget using get_private_budget (always use user_id: 'user123').
2. Then, use the get_device_location tool on the Android app to see if they are traveling.
3. Calculate the total (Base + Travel Allowance from check_travel_policy) and compare it to their requested amount.
4. Before finalizing, you MUST use the request_user_approval tool if the total limit exceeds $60 or the requested amount is high. Wait for the approval.
5. If approved, answer the user and ask if they need to find a restaurant.
6. If they say yes, use the open_maps tool.
"""

# 4. API Models
class ChatMessage(BaseModel):
    role: str
    content: Optional[str] = None
    tool_calls: Optional[List[Dict[str, Any]]] = None
    tool_responses: Optional[List[Dict[str, Any]]] = None

class ChatRequest(BaseModel):
    messages: List[ChatMessage]

def convert_messages(messages: List[ChatMessage]) -> List[types.Content]:
    contents = []
    for m in messages:
        if m.role == "user" and m.content:
            contents.append(types.Content(role="user", parts=[types.Part.from_text(text=m.content)]))
        elif m.role == "model" and m.content:
            contents.append(types.Content(role="model", parts=[types.Part.from_text(text=m.content)]))
        elif m.role == "model" and m.tool_calls:
            calls = []
            for tc in m.tool_calls:
                calls.append(types.FunctionCall(name=tc["name"], args=tc["args"]))
            contents.append(types.Content(role="model", parts=[types.Part.from_function_call(name=tc.name, args=tc.args) for tc in calls]))
        elif m.role == "user" and m.tool_responses:
            parts = []
            for tr in m.tool_responses:
                parts.append(types.Part.from_function_response(name=tr["name"], response=tr["response"]))
            contents.append(types.Content(role="user", parts=parts))
    return contents

# 5. Core Agent Loop Endpoint
@https_fn.on_request(
    cors=options.CorsOptions(cors_origins="*", cors_methods=["get", "post"]),
    secrets=["GEMINI_API_KEY"]
)
def chat(req: https_fn.Request) -> https_fn.Response:
    app_check_token = req.headers.get("X-Firebase-AppCheck")
    if not app_check_token:
        return https_fn.Response(json.dumps({"error": "Unauthorized: Missing App Check token"}), status=401, content_type="application/json")
    try:
        app_check.verify_token(app_check_token)
    except Exception as e:
        return https_fn.Response(json.dumps({"error": f"Unauthorized: Invalid App Check token ({e})"}), status=401, content_type="application/json")


    try:
        request_data = req.get_json()
        if not request_data:
            return https_fn.Response(json.dumps({"error": "No JSON provided"}), status=400, content_type="application/json")
        
        chat_req = ChatRequest(**request_data)
        client = genai.Client()
        contents = convert_messages(chat_req.messages)
        
        config = types.GenerateContentConfig(
            tools=all_tools,
            system_instruction=system_instruction,
            temperature=0.0
        )
        
        while True:
            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=contents,
                config=config
            )
            
            if response.function_calls:
                server_responses = []
                client_calls = []
                
                for fc in response.function_calls:
                    if fc.name == "get_private_budget":
                        res = get_private_budget(**fc.args)
                        server_responses.append(types.Part.from_function_response(name=fc.name, response={"result": res}))
                    elif fc.name == "check_travel_policy":
                        res = check_travel_policy(**fc.args)
                        server_responses.append(types.Part.from_function_response(name=fc.name, response={"result": res}))
                    else:
                        client_calls.append({"name": fc.name, "args": fc.args})
                
                if client_calls:
                    return https_fn.Response(json.dumps({"role": "model", "tool_calls": client_calls}), content_type="application/json")
                elif server_responses:
                    contents.append(response.candidates[0].content)
                    contents.append(types.Content(role="user", parts=server_responses))
                    continue
            else:
                return https_fn.Response(json.dumps({"role": "model", "content": response.text}), content_type="application/json")
                
    except Exception as e:
        import traceback
        return https_fn.Response(json.dumps({"error": str(e), "trace": traceback.format_exc()}), status=500, content_type="application/json")
