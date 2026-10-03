#!/usr/bin/env python3
"""Check changelogs.md and pull release notes out of it.

Usage:
    validate_changelog.py                       check the file's structure
    validate_changelog.py --require-unreleased  also fail if [Unreleased] has no entries
    validate_changelog.py --extract 1.2.3       print the notes for version 1.2.3

The file looks like this:

    # Changelog

    ## [Unreleased]

    ### Changes
    * One entry per bullet.

    ### Fixes
    * Another entry.

    ## [1.2.3] - 2026-01-31
"""

import argparse
import re
import sys
from pathlib import Path

DEFAULT_PATH = Path(__file__).resolve().parents[2] / "changelogs.md"
SECTIONS = ("Changes", "Fixes")
VERSION_HEADING = re.compile(r"^## \[(\d+\.\d+\.\d+(?:-[0-9A-Za-z.]+)?)\] - (\d{4}-\d{2}-\d{2})$")


def parse(lines, errors):
    """Return a list of releases: {"name", "line", "body", "sections"}."""
    releases = []
    release = None
    section = None

    for number, line in enumerate(lines, start=1):
        if line != line.rstrip():
            errors.append(f"line {number}: trailing whitespace")

        if line.startswith("## "):
            name = None
            if line == "## [Unreleased]":
                name = "Unreleased"
            else:
                match = VERSION_HEADING.match(line)
                if match:
                    name = match.group(1)
            if name is None:
                errors.append(
                    f"line {number}: heading must be '## [Unreleased]' or '## [x.y.z] - YYYY-MM-DD'"
                )
            elif any(r["name"] == name for r in releases):
                errors.append(f"line {number}: [{name}] appears more than once")
            release = {"name": name or line, "line": number, "body": [], "sections": {}}
            releases.append(release)
            section = None
            continue

        if release is None:
            continue

        release["body"].append(line)

        if line.startswith("### "):
            title = line[4:]
            if title not in SECTIONS:
                errors.append(
                    f"line {number}: section '{title}' is not one of {', '.join(SECTIONS)}"
                )
            if title in release["sections"]:
                errors.append(f"line {number}: '{title}' appears twice in [{release['name']}]")
            section = release["sections"].setdefault(title, [])
        elif line.startswith("* "):
            if section is None:
                errors.append(f"line {number}: entry is outside a ### section")
            elif not line[2:].strip():
                errors.append(f"line {number}: empty entry")
            else:
                section.append(line)
        elif line.startswith("  ") and line.strip() and section:
            continue  # a wrapped line belonging to the entry above
        elif line.strip():
            errors.append(f"line {number}: expected '* ' entry, found '{line.strip()[:40]}'")

    return releases


def check(text, require_unreleased):
    errors = []
    lines = text.split("\n")

    if not text.endswith("\n"):
        errors.append("file must end with a newline")
    if lines and lines[-1] == "":
        lines.pop()

    first = next((line for line in lines if line.strip()), "")
    if first != "# Changelog":
        errors.append("first line must be '# Changelog'")

    releases = parse(lines, errors)

    if not releases or releases[0]["name"] != "Unreleased":
        errors.append("the first release heading must be '## [Unreleased]'")

    for release in releases:
        for title, entries in release["sections"].items():
            if not entries:
                errors.append(f"[{release['name']}] has an empty '{title}' section")

    if require_unreleased and releases and releases[0]["name"] == "Unreleased":
        if not any(releases[0]["sections"].values()):
            errors.append("[Unreleased] has no entries; add one for this change")

    return errors, releases


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--file", type=Path, default=DEFAULT_PATH, help="changelog path")
    parser.add_argument("--require-unreleased", action="store_true")
    parser.add_argument("--extract", metavar="VERSION")
    args = parser.parse_args()

    if not args.file.is_file():
        print(f"error: {args.file} not found", file=sys.stderr)
        return 1

    errors, releases = check(args.file.read_text(encoding="utf-8"), args.require_unreleased)
    for error in errors:
        print(f"error: {error}", file=sys.stderr)
    if errors:
        return 1

    if args.extract:
        wanted = args.extract.lstrip("v")
        for release in releases:
            if release["name"] == wanted:
                print("\n".join(release["body"]).strip())
                return 0
        print(f"error: no [{wanted}] section in {args.file.name}", file=sys.stderr)
        return 1

    print(f"{args.file.name} looks good")
    return 0


if __name__ == "__main__":
    sys.exit(main())
