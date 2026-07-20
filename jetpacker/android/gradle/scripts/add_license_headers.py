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

import os
import sys

KOTLIN_HEADER = """/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"""

SCRIPT_HEADER = """# Copyright 2026 The Android Open Source Project
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

"""

XML_HEADER = """<!--
  Copyright 2026 The Android Open Source Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

"""

def process_file(file_path):
    # Handle .gradle.kts correctly
    if file_path.endswith('.gradle.kts'):
        ext = '.gradle.kts'
    else:
        _, ext = os.path.splitext(file_path)
        ext = ext.lower()
    
    if ext not in ['.kt', '.py', '.sh', '.xml', '.gradle.kts', '.gradle']:
        return

    # Skip gradle wrapper
    if 'gradlew' in file_path:
        return

    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()

    # Check if copyright notice is already present
    if "The Android Open Source Project" in content or "Copyright (C)" in content or "Copyright ©" in content or "Copyright 2015" in content:
        print(f"Skipping (already has header): {file_path}")
        return

    print(f"Adding header to: {file_path}")

    if ext in ['.kt', '.gradle.kts', '.gradle']:
        new_content = KOTLIN_HEADER + content
    elif ext in ['.py', '.sh']:
        # If it has a shebang, insert header after it
        if content.startswith('#!'):
            lines = content.split('\n')
            shebang = lines[0]
            rest = '\n'.join(lines[1:])
            new_content = shebang + '\n' + SCRIPT_HEADER + rest
        else:
            new_content = SCRIPT_HEADER + content
    elif ext == '.xml':
        # If it has an xml declaration, insert header after it
        if content.strip().startswith('<?xml'):
            lines = content.split('\n')
            xml_decl = lines[0]
            rest = '\n'.join(lines[1:])
            new_content = xml_decl + '\n' + XML_HEADER + rest
        else:
            new_content = XML_HEADER + content
            
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

def main():
    # Find the root directory of the repository
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, "..", ".."))
    exclude_dirs = {'.git', '.gradle', 'build', '.idea', 'third_party'}

    for dirpath, dirnames, filenames in os.walk(root_dir):
        # Modifying dirnames in-place will prevent os.walk from visiting excluded directories
        dirnames[:] = [d for d in dirnames if d not in exclude_dirs]
        
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            process_file(file_path)

if __name__ == "__main__":
    main()
