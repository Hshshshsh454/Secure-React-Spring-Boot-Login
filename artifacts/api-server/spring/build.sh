#!/usr/bin/env bash
set -euo pipefail

JAVAC_PATH="$(readlink -f "$(command -v javac)")"
export JAVA_HOME="$(dirname "$(dirname "$JAVAC_PATH")")"

exec mvn -f artifacts/api-server/spring/pom.xml package
