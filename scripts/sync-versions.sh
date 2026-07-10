#!/usr/bin/env bash
set -euo pipefail

MODE="sync"
POM_SOURCE="worktree"
while [ $# -gt 0 ]; do
  case "$1" in
    --check)
      MODE="check"
      ;;
    --staged-pom)
      POM_SOURCE="staged"
      ;;
    *)
      echo "Usage: $0 [--check] [--staged-pom]" >&2
      exit 2
      ;;
  esac
  shift
done

cd "$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

if command -v python3 >/dev/null 2>&1; then
  PYTHON=python3
elif command -v python >/dev/null 2>&1; then
  PYTHON=python
else
  echo "ERROR: python3/python not found." >&2
  exit 1
fi

if [ "$POM_SOURCE" = "staged" ]; then
  POM_XML=$(git show :pom.xml)
else
  POM_XML=$(cat pom.xml)
fi

VERSION=$(POM_XML="$POM_XML" "$PYTHON" - <<'PY'
import os, xml.etree.ElementTree as ET
root = ET.fromstring(os.environ['POM_XML'])
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
version = root.find('m:version', ns)
if version is None or not version.text:
    raise SystemExit('pom.xml project version not found')
print(version.text)
PY
)

FILES=(README.md frontend/README.md)
while IFS= read -r -d '' f; do
  FILES+=("$f")
done < <(find docs -maxdepth 1 -type f -name '*.md' -print0 | sort -z)

if [ "$MODE" = "check" ]; then
  "$PYTHON" - "$VERSION" "${FILES[@]}" <<'PY'
import json, re, sys
from pathlib import Path
version = sys.argv[1]
files = [Path(p) for p in sys.argv[2:]]
failed = False

def fail(msg):
    global failed
    print(f"ERROR: {msg}", file=sys.stderr)
    failed = True

readme = Path('README.md').read_text()
if f"Current version: **{version}**" not in readme:
    fail(f"README.md Current version line does not match pom.xml ({version})")

jar_re = re.compile(r'lift-simulator-(\d+\.\d+\.\d+)\.jar')
for path in files:
    text = path.read_text()
    for match in jar_re.finditer(text):
        if match.group(1) != version:
            fail(f"{path} contains stale JAR version {match.group(1)} (expected {version})")

pkg_version = json.loads(Path('frontend/package.json').read_text()).get('version')
if pkg_version != version:
    fail(f"frontend/package.json version ({pkg_version}) != pom.xml ({version})")

lock = Path('frontend/package-lock.json')
if lock.exists():
    data = json.loads(lock.read_text())
    if data.get('version') != version:
        fail(f"frontend/package-lock.json root version ({data.get('version')}) != pom.xml ({version})")
    pkg_root = data.get('packages', {}).get('', {})
    if pkg_root.get('version') != version:
        fail(f"frontend/package-lock.json package root version ({pkg_root.get('version')}) != pom.xml ({version})")

if failed:
    sys.exit(1)
print(f"All version references are consistent with pom.xml ({version}).")
PY
  exit $?
fi

"$PYTHON" - "$VERSION" "${FILES[@]}" <<'PY'
import re, sys
from pathlib import Path
version = sys.argv[1]
for name in sys.argv[2:]:
    path = Path(name)
    text = path.read_text()
    original = text
    if name == 'README.md':
        text = re.sub(r'Current version: \*\*\d+\.\d+\.\d+\*\*', f'Current version: **{version}**', text)
    text = re.sub(r'lift-simulator-\d+\.\d+\.\d+\.jar', f'lift-simulator-{version}.jar', text)
    if text != original:
        path.write_text(text)
PY

if command -v npm >/dev/null 2>&1; then
  npm --prefix frontend version "$VERSION" --no-git-tag-version --allow-same-version --silent >/dev/null
else
  "$PYTHON" - "$VERSION" <<'PY'
import json, sys
from pathlib import Path
version = sys.argv[1]
for filename in ['frontend/package.json', 'frontend/package-lock.json']:
    path = Path(filename)
    if not path.exists():
        continue
    data = json.loads(path.read_text())
    data['version'] = version
    if filename.endswith('package-lock.json') and 'packages' in data and '' in data['packages']:
        data['packages']['']['version'] = version
    path.write_text(json.dumps(data, indent=2) + '\n')
PY
fi

echo "Version references synced to ${VERSION}."
