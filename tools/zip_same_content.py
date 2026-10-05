#!/usr/bin/env python3
"""Compare two zip files: exit 0 if all entries except top-level META-INF/ are identical."""
import hashlib
import sys
import zipfile


def is_top_level_meta_inf(path):
    """Return True if path is directly under top-level META-INF/ (i.e., no parent dir before META-INF)."""
    parts = path.split("/")
    return len(parts) == 2 and parts[0] == "META-INF"


def get_duplicate_entries(zip_file):
    """Find duplicate entry names (excluding top-level META-INF/).

    Args:
        zip_file: An open ZipFile object

    Returns:
        The first duplicate entry name, or None if no duplicates.
    """
    seen = set()
    for entry in zip_file.infolist():
        name = entry.filename
        if not is_top_level_meta_inf(name):
            if name in seen:
                return name
            seen.add(name)
    return None


def compare_zips(zip_a_path, zip_b_path):
    """Compare two zip files.

    Returns:
        (True, None) if equal (excluding top-level META-INF/)
        (False, entry_name) if different; entry_name is the first differing entry
        (None, error_msg) on IO/usage error
    """
    try:
        with zipfile.ZipFile(zip_a_path, "r") as zip_a, zipfile.ZipFile(
            zip_b_path, "r"
        ) as zip_b:
            # Check for duplicate entries (excluding top-level META-INF/)
            dup_a = get_duplicate_entries(zip_a)
            if dup_a:
                return (False, f"duplicate entry: {dup_a}")

            dup_b = get_duplicate_entries(zip_b)
            if dup_b:
                return (False, f"duplicate entry: {dup_b}")

            names_a = set(zip_a.namelist())
            names_b = set(zip_b.namelist())

            # Filter out top-level META-INF entries
            names_a_filtered = {n for n in names_a if not is_top_level_meta_inf(n)}
            names_b_filtered = {n for n in names_b if not is_top_level_meta_inf(n)}

            # Check if the sets are the same
            if names_a_filtered != names_b_filtered:
                # Find the first entry that differs
                all_names = sorted(names_a_filtered | names_b_filtered)
                for name in all_names:
                    if name not in names_a_filtered:
                        return (False, name)
                    if name not in names_b_filtered:
                        return (False, name)

            # Check content of each entry
            for name in sorted(names_a_filtered):
                content_a = zip_a.read(name)
                content_b = zip_b.read(name)

                if content_a != content_b:
                    return (False, name)

            return (True, None)

    except FileNotFoundError as e:
        return (None, str(e))
    except zipfile.BadZipFile as e:
        return (None, str(e))
    except Exception as e:
        return (None, str(e))


def main(args):
    """Main entry point.

    Args:
        args: Command line arguments (should be ['A', 'B'])

    Returns:
        0 if zips are equal (excluding top-level META-INF/)
        1 if zips differ
        2 on usage or IO error
    """
    if len(args) != 2:
        print("Usage: zip_same_content.py <zip_a> <zip_b>", file=sys.stderr)
        return 2

    zip_a_path, zip_b_path = args
    equal, diff = compare_zips(zip_a_path, zip_b_path)

    if equal is None:
        # IO/usage error
        print(f"Error: {diff}", file=sys.stderr)
        return 2
    elif equal:
        # Zips are equal
        return 0
    else:
        # Zips differ
        print(diff)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
