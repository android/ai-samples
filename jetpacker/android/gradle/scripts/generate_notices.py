#!/usr/bin/env python3
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

import json
import os
import sys

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, "..", ".."))
    json_path = os.path.join(root_dir, "build", "reports", "dependency-license", "licenses.json")
    output_paths = [
        os.path.join(root_dir, "THIRD_PARTY_NOTICES"),
        os.path.join(root_dir, "app/src/main/assets/THIRD_PARTY_NOTICES")
    ]

    if not os.path.exists(json_path):
        print(f"Error: {json_path} does not exist.", file=sys.stderr)
        sys.exit(1)

    with open(json_path, 'r') as f:
        try:
            data = json.load(f)
        except (json.JSONDecodeError, ValueError) as e:
            print(f"Error: Failed to parse JSON from {json_path}: {e}", file=sys.stderr)
            sys.exit(1)

    dependencies = data.get("dependencies", [])
    
    # Group by module name to avoid duplicate listings if multiple versions are fetched
    unique_deps = {}
    for dep in dependencies:
        name = dep.get("moduleName")
        version = dep.get("moduleVersion")
        license_name = dep.get("moduleLicense")
        license_url = dep.get("moduleLicenseUrl")
        url = dep.get("moduleUrl")

        if not name:
            continue

        key = (name, version)
        unique_deps[key] = {
            "name": name,
            "version": version,
            "license": license_name or "Unknown License",
            "license_url": license_url or "",
            "url": url or ""
        }

    sorted_deps = sorted(unique_deps.values(), key=lambda x: x["name"].lower())

    report_lines = []
    report_lines.append("========================================================================")
    report_lines.append("THIRD-PARTY NOTICES AND LICENSES")
    report_lines.append("========================================================================")
    report_lines.append("")
    report_lines.append("This distribution packages various third-party open-source components.")
    report_lines.append("Below is a list of these dependencies, including version, license type,")
    report_lines.append("and associated URLs.")
    report_lines.append("")
    report_lines.append("------------------------------------------------------------------------")
    report_lines.append("")

    for idx, dep in enumerate(sorted_deps, 1):
        report_lines.append(f"{idx}. {dep['name']} (version {dep['version']})")
        report_lines.append(f"   Project URL: {dep['url']}")
        report_lines.append(f"   License:     {dep['license']}")
        if dep['license_url']:
            report_lines.append(f"   License URL: {dep['license_url']}")
        report_lines.append("")

    report_lines.append("------------------------------------------------------------------------")
    report_lines.append("End of Third-Party Notices")

    report_content = "\n".join(report_lines)

    for path in output_paths:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            f.write(report_content)
        print(f"Successfully generated THIRD-PARTY NOTICES at: {path}")

if __name__ == "__main__":
    main()
