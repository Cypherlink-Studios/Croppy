#!/usr/bin/env python3
"""Audit script for Minecraft Java 1.21.8 Resource Packs (Format 64).

Validates Strict Mode JSON, valid lowercase namespaced paths [a-z0-9_.-],
and flags obsolete patterns such as 'overrides' in models/item/ or loose uniforms in shaders.
"""

import json
import os
import re
import sys

VALID_NAME_RE = re.compile(r"^[a-z0-9_.-]+$")
LOOSE_UNIFORM_RE = re.compile(r"^\s*uniform\s+(mat4|vec[234]|float|int)\s+(ModelViewMat|ProjMat|ColorModulator|ScreenSize)\b", re.MULTILINE)

def audit_pack(root_dir: str) -> int:
    errors = []
    warnings = []

    if not os.path.isdir(root_dir):
        print(f"[ERROR] Specified path '{root_dir}' is not a directory.")
        return 1

    # 1. pack.mcmeta inspection
    mcmeta_path = os.path.join(root_dir, "pack.mcmeta")
    if not os.path.isfile(mcmeta_path):
        errors.append("CRITICAL: 'pack.mcmeta' not found in resource pack root.")
    else:
        try:
            with open(mcmeta_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            pack_obj = data.get("pack", {})
            pack_format = pack_obj.get("pack_format")
            if pack_format != 64:
                warnings.append(f"'pack.mcmeta' pack_format is {pack_format}. Minecraft 1.21.8 expects 64.")
            supported = pack_obj.get("supported_formats")
            if isinstance(supported, dict):
                max_inc = supported.get("max_inclusive")
                if max_inc is not None and max_inc > 64:
                    errors.append(
                        f"'pack.mcmeta' supported_formats.max_inclusive is {max_inc} (> 64). "
                        "Declaring versions newer than 64 requires Minecraft's modern 'min_format' and 'max_format' fields."
                    )
            elif isinstance(supported, list) and len(supported) == 2:
                if supported[1] > 64:
                    errors.append(
                        f"'pack.mcmeta' supported_formats max is {supported[1]} (> 64). "
                        "Declaring versions newer than 64 requires Minecraft's modern 'min_format' and 'max_format' fields."
                    )
        except json.JSONDecodeError as exc:
            errors.append(f"STRICT JSON PARSER ERROR in 'pack.mcmeta': {exc}")
        except Exception as exc:
            errors.append(f"Unable to read 'pack.mcmeta': {exc}")

    # 2. Tree validation under assets/
    assets_dir = os.path.join(root_dir, "assets")
    if not os.path.isdir(assets_dir):
        warnings.append("No 'assets/' directory found in pack root.")
    else:
        for dirpath, dirnames, filenames in os.walk(assets_dir):
            # Check directory names
            for dname in dirnames:
                if not VALID_NAME_RE.match(dname):
                    rel = os.path.relpath(os.path.join(dirpath, dname), root_dir)
                    errors.append(f"INVALID DIRECTORY NAME (uppercase or illegal characters): '{rel}'")

            # Check filenames
            for fname in filenames:
                fpath = os.path.join(dirpath, fname)
                rel = os.path.relpath(fpath, root_dir)
                norm_rel = rel.replace("\\", "/")

                if not VALID_NAME_RE.match(fname):
                    errors.append(f"INVALID FILENAME (uppercase or illegal characters): '{rel}'")

                # Strict JSON validation
                if fname.endswith((".json", ".mcmeta")):
                    try:
                        with open(fpath, "r", encoding="utf-8") as f:
                            content = json.load(f)

                        # Flag obsolete 'overrides' in models/item/
                        if "/models/item/" in norm_rel and isinstance(content, dict) and "overrides" in content:
                            warnings.append(
                                f"OBSOLETE PATTERN: '{rel}' uses 'overrides'. "
                                "In 1.21.4+, use 'assets/<namespace>/items/<item>.json' definitions instead."
                            )
                    except json.JSONDecodeError as exc:
                        errors.append(f"STRICT JSON PARSER ERROR in '{rel}': {exc}")
                    except Exception as exc:
                        errors.append(f"Error reading file '{rel}': {exc}")

                # Shaders loose uniforms check
                if fname.endswith((".vsh", ".fsh")):
                    try:
                        with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                            glsl = f.read()
                        if LOOSE_UNIFORM_RE.search(glsl):
                            errors.append(
                                f"OBSOLETE GLSL SYNTAX in '{rel}': Loose standard uniforms detected. "
                                "Minecraft 1.21.6+ requires Uniform Blocks (UBOs) via #moj_import."
                            )
                    except Exception as exc:
                        warnings.append(f"Error checking shader '{rel}': {exc}")

    # Print summary
    print(f"\n=======================================================")
    print(f"  Minecraft 1.21.8 Resource Pack Audit: {root_dir}")
    print(f"=======================================================")
    print(f"Critical Errors: {len(errors)}")
    print(f"Warnings:        {len(warnings)}\n")

    for err in errors:
        print(f"  [X] {err}")
    for warn in warnings:
        print(f"  [!] {warn}")

    if not errors and not warnings:
        print("  [OK] Pack structure and JSON files strictly comply with Minecraft 1.21.8 specifications.")
    print("=======================================================\n")

    return 1 if errors else 0

if __name__ == "__main__":
    target_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    sys.exit(audit_pack(target_dir))
