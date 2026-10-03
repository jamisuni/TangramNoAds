#!/usr/bin/env python3
"""
V-07: No junk file or folder names in the repo (lesson of 2026-10-02: a bash redirect of `> C:\\Users\\...` lost its
backslashes and landed as a junk file named `C:Users...`; MSYS also maps `:` and `"` into private-use characters).

Fails when any file or folder name contains `:` or `"`, a private-use character (U+E000..U+F8FF, which covers the
MSYS colon substitute U+F03A and U+F022), or starts with `C:` or `cp `. Skips `.git/`, `build/` and `.gradle/` trees.
Exit: 0 = pass, 1 = check failed
"""

import os
import sys
from pathlib import Path

SKIP_DIRS = {".git", "build", ".gradle"}
BAD_CHARS = {":", '"'}


def bad_reason(name):
    """Returns why a single path component is junk, or None."""
    for ch in name:
        if ch in BAD_CHARS:
            return f"contains {ch!r}"
        if 0xE000 <= ord(ch) <= 0xF8FF:
            return f"contains private-use U+{ord(ch):04X}"
    if name.startswith("C:"):
        return "starts with 'C:'"
    if name.startswith("cp "):
        return "starts with 'cp '"
    return None


def find_junk(root):
    """Returns a list of (relative path, reason) for every junk name under root."""
    root = Path(root)
    found = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in list(dirnames) + filenames:
            why = bad_reason(name)
            if why:
                rel = (Path(dirpath) / name).relative_to(root).as_posix()
                found.append((rel.encode("unicode_escape").decode("ascii"), why))
    return sorted(found)


def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.cwd()
    if not root.is_dir():
        print(f"V-07 FAIL project root not found: {root}", file=sys.stderr)
        sys.exit(1)
    junk = find_junk(root)
    if junk:
        for rel, why in junk:
            print(f"V-07 FAIL junk name {rel}: {why}")
        sys.exit(1)
    print("V-07 PASS")
    sys.exit(0)


if __name__ == "__main__":
    main()
