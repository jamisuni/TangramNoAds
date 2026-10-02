#!/usr/bin/env python3
"""
V-01: Verify that the release merged manifest enforces G-01 (device-only, no permissions).
- Checks that merged manifest has zero <permission> / <uses-permission> / <uses-permission-sdk-23>
- Checks allowBackup="false"
- Checks dataExtractionRules present and properly configured
- Checks no EmojiCompatInitializer meta-data

Exit: 0 = pass, 1 = check failed, 2 = could not build
"""

import argparse
import subprocess
import sys
import os
import xml.etree.ElementTree as ET
from pathlib import Path


def build_release_manifest(project_root):
    """Build :app:processReleaseMainManifest and return the merged manifest path, or None on failure."""
    print("Building :app:processReleaseMainManifest...", file=sys.stderr)

    # On Windows, use cmd.exe /c to run gradlew.bat
    if sys.platform == "win32":
        cmd = ["cmd.exe", "/c", ".\\gradlew.bat", ":app:processReleaseMainManifest", "--console=plain"]
    else:
        cmd = ["./gradlew", ":app:processReleaseMainManifest", "--console=plain"]

    try:
        result = subprocess.run(
            cmd,
            cwd=project_root,
            capture_output=True,
            text=True,
            timeout=120,
        )
        if result.returncode != 0:
            print(f"Build failed:\n{result.stderr}", file=sys.stderr)
            return None
    except subprocess.TimeoutExpired:
        print("Build timeout", file=sys.stderr)
        return None
    except Exception as e:
        print(f"Build error: {e}", file=sys.stderr)
        return None

    # Find the newest AndroidManifest.xml under app/build/intermediates/merged_manifest*/release/
    build_dir = Path(project_root) / "app" / "build" / "intermediates"
    if not build_dir.exists():
        print("No build intermediates found", file=sys.stderr)
        return None

    # Look for manifests in release builds
    manifests = list(build_dir.glob("**/release/**/AndroidManifest.xml"))
    if not manifests:
        print("No merged manifest found", file=sys.stderr)
        return None

    # Filter to only those with merged_manifest in the path
    merged_manifests = [m for m in manifests if "merged_manifest" in str(m)]
    if not merged_manifests:
        merged_manifests = manifests  # Fallback to any manifest in release/

    # Return the newest one (by modification time)
    manifest_path = max(merged_manifests, key=lambda p: p.stat().st_mtime)
    print(f"Using manifest: {manifest_path}", file=sys.stderr)
    return str(manifest_path)


def check_manifest(manifest_path, res_dir):
    """
    Check the manifest and data extraction rules.
    Returns True if all checks pass, False otherwise.
    """
    if not os.path.exists(manifest_path):
        print(f"V-01 FAIL manifest not found: {manifest_path}")
        return False

    try:
        tree = ET.parse(manifest_path)
        root = tree.getroot()
    except Exception as e:
        print(f"V-01 FAIL could not parse manifest: {e}")
        return False

    # Define namespaces
    ns = {
        "android": "http://schemas.android.com/apk/res/android",
        "tools": "http://schemas.android.com/tools",
    }

    errors = []

    # Check for permission elements
    perms = root.findall("permission", ns)
    if perms:
        for perm in perms:
            name = perm.get("{%s}name" % ns["android"])
            errors.append(f"permission element found: {name}")

    # Check for uses-permission elements
    uses_perms = root.findall("uses-permission", ns)
    if uses_perms:
        for perm in uses_perms:
            name = perm.get("{%s}name" % ns["android"])
            errors.append(f"uses-permission element found: {name}")

    # Check for uses-permission-sdk-23 elements
    uses_perms_sdk23 = root.findall("uses-permission-sdk-23", ns)
    if uses_perms_sdk23:
        for perm in uses_perms_sdk23:
            name = perm.get("{%s}name" % ns["android"])
            errors.append(f"uses-permission-sdk-23 element found: {name}")

    # Check allowBackup
    app = root.find("application", ns)
    if app is None:
        errors.append("no application element")
    else:
        allow_backup = app.get("{%s}allowBackup" % ns["android"])
        if allow_backup is None:
            errors.append("android:allowBackup attribute missing")
        elif allow_backup.lower() != "false":
            errors.append(f"android:allowBackup is {allow_backup}, not false")

        # Check dataExtractionRules
        extraction_rules = app.get("{%s}dataExtractionRules" % ns["android"])
        if not extraction_rules:
            errors.append("android:dataExtractionRules attribute missing")
        else:
            # Extract the resource name (e.g., "@xml/data_extraction_rules" -> "data_extraction_rules")
            if extraction_rules.startswith("@xml/"):
                rule_name = extraction_rules[5:]
            else:
                rule_name = extraction_rules

            # Check the XML file
            rule_file = Path(res_dir) / "xml" / f"{rule_name}.xml"
            if not rule_file.exists():
                errors.append(f"data extraction rules file not found: {rule_file}")
            else:
                if not check_extraction_rules(rule_file):
                    errors.append(f"data extraction rules invalid: {rule_file}")

        # Check for EmojiCompatInitializer
        providers = app.findall("provider", ns)
        for provider in providers:
            meta_datas = provider.findall("meta-data", ns)
            for meta_data in meta_datas:
                name = meta_data.get("{%s}name" % ns["android"])
                if name == "androidx.emoji2.text.EmojiCompatInitializer":
                    errors.append("androidx.emoji2.text.EmojiCompatInitializer meta-data found")

    if errors:
        for error in errors:
            print(f"V-01 FAIL {error}")
        return False

    return True


def check_extraction_rules(rule_file):
    """
    Check that the data extraction rules file excludes all nine domains in both sections.
    Returns True if valid, False otherwise.
    """
    try:
        tree = ET.parse(rule_file)
        root = tree.getroot()
    except Exception:
        return False

    required_domains = {"root", "file", "database", "sharedpref", "external",
                        "device_root", "device_file", "device_database", "device_sharedpref"}

    for section in ["cloud-backup", "device-transfer"]:
        section_elem = root.find(section)
        if section_elem is None:
            return False

        excluded_domains = set()
        for exclude in section_elem.findall("exclude"):
            domain = exclude.get("domain")
            if domain:
                excluded_domains.add(domain)

        # Check if all required domains are excluded
        if not required_domains.issubset(excluded_domains):
            return False

    return True


def main():
    parser = argparse.ArgumentParser(description="Verify G-01 release manifest hardening")
    parser.add_argument("--manifest", help="Path to the manifest file (if not given, build it)")
    parser.add_argument("--res-dir", default="app/src/main/res",
                        help="Path to the res directory (default: app/src/main/res)")
    args = parser.parse_args()

    # Determine the project root
    if args.manifest:
        project_root = Path.cwd()
    else:
        # Assume we're in the project root
        project_root = Path.cwd()

    # Get the manifest path
    if args.manifest:
        manifest_path = args.manifest
    else:
        manifest_path = build_release_manifest(project_root)
        if manifest_path is None:
            sys.exit(2)

    # Resolve res_dir relative to project root if it's relative
    if not os.path.isabs(args.res_dir):
        res_dir = str(project_root / args.res_dir)
    else:
        res_dir = args.res_dir

    # Check the manifest
    if check_manifest(manifest_path, res_dir):
        print("V-01 PASS")
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
