#!/usr/bin/env python3
"""
V-05: Verify that every module has equal string-resource key parity in en (values/) and fi (values-fi/).
Exit: 0 = pass (or no strings), 1 = key mismatch
"""

import sys
import os
import xml.etree.ElementTree as ET
from pathlib import Path
from collections import defaultdict


def collect_string_keys(res_dir, lang):
    """
    Collect string/plurals/string-array keys from all XML files in values*/ (lang="en") or values-fi/ (lang="fi").
    Returns a set of (tag, name) tuples, skipping translatable="false".
    Raises exception if any XML file cannot be parsed.
    """
    keys = set()

    # Map language to directory pattern
    if lang == "en":
        dir_pattern = "values"
    elif lang == "fi":
        dir_pattern = "values-fi"
    else:
        return keys

    res_path = Path(res_dir)
    if not res_path.exists():
        return keys

    # Find the values directory
    values_dirs = [d for d in res_path.iterdir() if d.is_dir() and d.name == dir_pattern]

    for values_dir in values_dirs:
        # Find all XML files in the directory
        xml_files = sorted(values_dir.glob("*.xml"))

        for xml_file in xml_files:
            try:
                tree = ET.parse(xml_file)
                root = tree.getroot()

                # Look for string, plurals, and string-array elements
                for elem in root:
                    tag = elem.tag
                    if tag in ("string", "plurals", "string-array"):
                        name = elem.get("name")
                        translatable = elem.get("translatable", "true")

                        # Skip if translatable="false"
                        if translatable == "false":
                            continue

                        if name:
                            keys.add((tag, name))
            except ET.ParseError as e:
                raise ValueError(f"Parse error in {xml_file}: {e}")
            except Exception as e:
                raise ValueError(f"Error reading {xml_file}: {e}")

    return keys


def check_module(module_path):
    """
    Check a module for string parity.
    Returns True if all keys match, False if there's a mismatch or parse error.
    """
    # Find all res directories in the module (e.g., src/main/res, src/debug/res, etc.)
    res_dirs = list(Path(module_path).glob("src/*/res"))

    if not res_dirs:
        # No res directory, pass vacuously
        return True

    has_error = False

    for res_dir in res_dirs:
        try:
            en_keys = collect_string_keys(res_dir, "en")
            fi_keys = collect_string_keys(res_dir, "fi")
        except ValueError as e:
            print(f"V-05 FAIL {e}")
            has_error = True
            continue

        # Both empty is OK (no strings defined)
        if not en_keys and not fi_keys:
            continue

        # Check for mismatches
        missing_in_fi = en_keys - fi_keys
        missing_in_en = fi_keys - en_keys

        if missing_in_fi or missing_in_en:
            has_error = True
            module_name = Path(module_path).name

            for tag, name in sorted(missing_in_fi):
                print(f"V-05 FAIL {module_name}: {tag} {name} missing in values-fi/")

            for tag, name in sorted(missing_in_en):
                print(f"V-05 FAIL {module_name}: {tag} {name} missing in values/")

    return not has_error


def main():
    if len(sys.argv) < 2:
        project_root = Path.cwd()
    else:
        project_root = Path(sys.argv[1])

    if not project_root.is_dir():
        print(f"V-05 FAIL project root not found: {project_root}", file=sys.stderr)
        sys.exit(1)

    # Find all modules (directories with build.gradle.kts)
    modules = []
    for potential_module in project_root.iterdir():
        if potential_module.is_dir() and (potential_module / "build.gradle.kts").exists():
            modules.append(potential_module)

    if not modules:
        # No modules found, pass vacuously
        print("V-05 PASS")
        sys.exit(0)

    all_pass = True
    for module in sorted(modules):
        if not check_module(module):
            all_pass = False

    if all_pass:
        print("V-05 PASS")
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
