#!/usr/bin/env python3
"""
V-06: Verify that module dependencies follow G-06 (slices meet only through kernel, contracts, app).
Exit: 0 = pass, 1 = check failed
"""

import re
import sys
from pathlib import Path


# Allowed main-scope dependencies per module (architecture.md §2 G-06)
ALLOWED_DEPS = {
    "kernel": set(),
    "contracts": {"kernel"},
    "content": {"kernel", "contracts"},
    "store": {"kernel", "contracts"},
    "play": {"kernel", "contracts"},
    "browse": {"kernel", "contracts"},
    "time": {"kernel", "contracts"},
    "settings": {"kernel", "contracts"},
    "devtools": {"kernel", "contracts"},
    "app": {"kernel", "contracts", "content", "store", "play", "browse", "time", "settings", "devtools"},
}


def parse_settings_gradle(project_root):
    """
    Parse settings.gradle.kts and extract module names from include(":x") directives.
    Returns a set of module names.
    """
    settings_file = Path(project_root) / "settings.gradle.kts"
    if not settings_file.exists():
        return set()

    modules = set()
    with open(settings_file, "r", newline="\n") as f:
        content = f.read()

    # Find all include(":...") directives
    pattern = r'include\s*\(\s*":\s*([^"]+)"\s*\)'
    for match in re.finditer(pattern, content):
        modules.add(match.group(1))

    return modules


def parse_build_gradle(build_file):
    """
    Parse build.gradle.kts and extract project() dependencies in main scope.
    Returns a set of module names.
    Raises exception if unparseable project references are found in main scope.
    """
    if not build_file.exists():
        return set()

    dependencies = set()
    with open(build_file, "r", newline="\n") as f:
        lines = f.readlines()

    # Remove comments
    in_block_comment = False
    filtered_lines = []
    for line in lines:
        # Handle block comments
        if "/*" in line:
            in_block_comment = True
        if "*/" in line:
            in_block_comment = False
            continue

        if in_block_comment:
            continue

        # Remove line comments
        if "//" in line:
            line = line[:line.index("//")]

        filtered_lines.append(line)

    content = "\n".join(filtered_lines)

    # Match dependency declarations: config(project(":module"))
    # Main scope: any that don't start with "test", "androidTest"
    pattern = r'(implementation|api|debugImplementation|releaseImplementation|runtimeOnly|compileOnly)\s*\(\s*project\s*\(\s*":\s*([^"]+)"\s*\)\s*\)'

    for match in re.finditer(pattern, content):
        config = match.group(1)
        module = match.group(2)

        # Only main scope (test/androidTest are allowed for test scope)
        if not config.startswith("test") and not config.startswith("androidTest"):
            dependencies.add(module)

    # Check for unparseable project references: projects.x or project(path=...) in main scope
    # Fail closed: if we see these patterns in main-scope configs, it's an error
    main_scope_configs = r'(implementation|api|debugImplementation|releaseImplementation|runtimeOnly|compileOnly)'

    # Find all main-scope dependency blocks
    main_dep_pattern = main_scope_configs + r'\s*\(\s*([^)]+)\s*\)'

    for match in re.finditer(main_dep_pattern, content):
        config = match.group(1)
        dep_content = match.group(2)

        # Skip test scope
        if config.startswith("test") or config.startswith("androidTest"):
            continue

        # Check for projects.x accessor
        if re.search(r'projects\s*\.\s*\w+', dep_content):
            print(f"V-06 FAIL unparseable dependency format: {config}({dep_content.strip()}) - use project(\":module\") format")
            return None  # Signal error

        # Check for project(path = ":x") format
        if re.search(r'project\s*\(\s*path\s*=', dep_content):
            print(f"V-06 FAIL unparseable dependency format: {config}({dep_content.strip()}) - use project(\":module\") format")
            return None  # Signal error

    return dependencies


def check_module_dependencies(module_name, build_file, allowed_deps):
    """
    Check if a module's dependencies are allowed.
    Returns True if valid, False otherwise.
    """
    deps = parse_build_gradle(build_file)

    # None indicates a parsing error was already printed
    if deps is None:
        return False

    if module_name not in allowed_deps:
        print(f"V-06 FAIL unknown module: {module_name}")
        return False

    allowed = allowed_deps[module_name]

    for dep in deps:
        if dep not in allowed:
            print(f"V-06 FAIL {module_name} -> {dep}")
            return False

    return True


def main():
    if len(sys.argv) < 2:
        project_root = Path.cwd()
    else:
        project_root = Path(sys.argv[1])

    if not project_root.is_dir():
        print(f"V-06 FAIL project root not found: {project_root}", file=sys.stderr)
        sys.exit(1)

    # Parse settings.gradle.kts to get the module list
    modules = parse_settings_gradle(project_root)

    # Also add modules that have build files but aren't explicitly included
    # (e.g., "kernel", "app" which are typically not in include())
    for potential_module in project_root.iterdir():
        if potential_module.is_dir() and (potential_module / "build.gradle.kts").exists():
            module_name = potential_module.name
            if module_name not in [".swdev", "gradle", "tools"]:
                modules.add(module_name)

    all_pass = True

    for module_name in sorted(modules):
        # Skip non-module directories
        if module_name in [".gradle", ".idea", "build"]:
            continue

        module_path = project_root / module_name
        if not module_path.is_dir():
            continue

        build_file = module_path / "build.gradle.kts"

        # Check if the module is in the allowed list
        if module_name not in ALLOWED_DEPS:
            # Unknown module; still check that it has a build file
            if not build_file.exists():
                print(f"V-06 FAIL {module_name}: no build.gradle.kts found")
                all_pass = False
            else:
                # Unknown module, but it exists - this is an error in the design
                print(f"V-06 FAIL unknown module {module_name} (not in ALLOWED_DEPS)")
                all_pass = False
            continue

        if build_file.exists():
            if not check_module_dependencies(module_name, build_file, ALLOWED_DEPS):
                all_pass = False

    if all_pass:
        print("V-06 PASS")
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
