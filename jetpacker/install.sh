#!/bin/bash
# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# install.sh
# Deploys the JetPacker server locally, sets up port forwarding, and installs the app.

set -e

echo "=================================================================="
echo " IMPORTANT: JetPacker ML features require Nano 4 to be installed."
echo " Please see the developer guide for setup instructions."
echo "=================================================================="
echo ""

# Check for adb
if ! command -v adb &> /dev/null; then
    echo "Error: adb is not found in your PATH."
    echo "Please ensure Android SDK Platform-Tools are installed."
    exit 1
fi

echo "[1/4] Installing JetPacker app on the connected device..."
adb install -r app.apk

echo "[2/4] Setting up port forwarding (tcp:8000 -> tcp:8000)..."
adb reverse tcp:8000 tcp:8000

echo "[3/4] Setting up Python virtual environment..."
python3 -m venv .venv
source .venv/bin/activate

echo "Installing server dependencies inside virtualenv..."
python3 -m pip install --quiet --extra-index-url https://pypi.org/simple fastapi uvicorn pydantic

echo "[4/4] Starting local server..."
echo "The server will run on port 8000. Press Ctrl+C to stop."
cd server
python3 -m uvicorn main:app --host 0.0.0.0 --port 8000
