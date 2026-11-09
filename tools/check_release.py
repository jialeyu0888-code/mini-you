"""Check a Mini YOU release candidate from the repository root."""

import argparse
import re
import subprocess
from pathlib import Path


TAG_PATTERN = re.compile(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")
GRADLE_FILE = Path("app/build.gradle.kts")
PACKAGE_ID = "com.miniyou.app"


def command_output(*args):
    return subprocess.check_output(args, encoding="utf-8")


def gradle_version(text):
    name = re.search(r'^\s*versionName\s*=\s*"([^"]+)"\s*$', text, re.MULTILINE)
    code = re.search(r"^\s*versionCode\s*=\s*([1-9][0-9]*)\s*$", text, re.MULTILINE)
    if not name or not code:
        raise ValueError(f"{GRADLE_FILE} must declare a literal versionName and positive versionCode")
    return name[1], int(code[1])


def check_notes(tag):
    path = Path("docs/releases") / f"{tag}.md"
    if not path.is_file():
        raise ValueError(f"Missing bilingual release notes: {path}")
    notes = path.read_text(encoding="utf-8-sig").strip()
    sections = re.fullmatch(r"## 中文\n(.*?)\n## English\n(.*)", notes, re.DOTALL)
    if not sections:
        raise ValueError(f"{path} must contain '## 中文' followed by '## English'")

    bullet_counts = []
    for section, language in zip(sections.groups(), (r"[\u3400-\u9fff]", r"[A-Za-z]{2,}")):
        lines = section.strip().splitlines()
        bullets = [line for line in lines if line.strip()]
        if not bullets or any(
            not line.startswith("- ") or not re.search(language, line[2:]) for line in bullets
        ):
            raise ValueError(f"{path} needs non-empty Chinese and English update bullets")
        bullet_counts.append(len(bullets))
    if bullet_counts[0] != bullet_counts[1]:
        raise ValueError(f"{path} must have one English bullet per Chinese bullet")


def check_version(tag):
    target = TAG_PATTERN.fullmatch(tag)
    if not target:
        raise ValueError("A stable release tag must use vMAJOR.MINOR.PATCH")
    name, code = gradle_version(GRADLE_FILE.read_text(encoding="utf-8"))
    if tag != f"v{name}":
        raise ValueError(f"{tag} does not match versionName {name}")

    target_version = tuple(map(int, target.groups()))
    for previous_tag in command_output("git", "tag", "--list", "v*").splitlines():
        previous = TAG_PATTERN.fullmatch(previous_tag)
        if not previous or previous_tag == tag:
            continue
        if tuple(map(int, previous.groups())) >= target_version:
            raise ValueError(f"{tag} must be newer than existing tag {previous_tag}")
        _, previous_code = gradle_version(
            command_output("git", "show", f"refs/tags/{previous_tag}:{GRADLE_FILE.as_posix()}")
        )
        if code <= previous_code:
            raise ValueError(f"versionCode {code} must exceed {previous_tag}'s versionCode {previous_code}")
    return name, code


def check_apk(path, aapt2, name, code):
    badging = command_output(aapt2, "dump", "badging", str(path))
    package = re.search(
        r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",
        badging,
        re.MULTILINE,
    )
    expected = (PACKAGE_ID, str(code), name)
    if not package or package.groups() != expected:
        found = package.groups() if package else "unreadable package metadata"
        raise ValueError(f"{path} has {found}. Expected {expected}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tag", help="Release tag, such as v0.2.3")
    parser.add_argument("--apk", type=Path, help="Also verify the APK package ID and versions")
    parser.add_argument("--aapt2", default="aapt2", help="Path to Android SDK's aapt2 executable")
    args = parser.parse_args()

    try:
        name, code = check_version(args.tag)
        check_notes(args.tag)
        if args.apk:
            check_apk(args.apk, args.aapt2, name, code)
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"Release check failed: {error}\n")
    print(f"{args.tag} passed release checks (versionCode {code})")


if __name__ == "__main__":
    main()
