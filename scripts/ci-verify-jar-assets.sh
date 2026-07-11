#!/usr/bin/env bash
# Verifies that the packaged Spring Boot JAR under target/ embeds the built
# React frontend. Shared by the backend job (which packages the JAR) and the
# e2e-playwright job (which downloads it as a build artifact).
set -euo pipefail

JAR_FILE=$(find target -maxdepth 1 -name 'lift-simulator-*.jar' ! -name '*.original' | head -n 1)
test -n "$JAR_FILE"
echo "Verifying frontend assets in $JAR_FILE"
jar tf "$JAR_FILE" | grep '^BOOT-INF/classes/static/index.html$'
jar tf "$JAR_FILE" | grep '^BOOT-INF/classes/static/assets/'
