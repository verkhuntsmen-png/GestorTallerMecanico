#!/usr/bin/env sh
set -eu

MVN_VERSION="${MVN_VERSION:-3.9.11}"
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_DIR="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION"
MVN_BIN="$MVN_DIR/bin/mvn"

if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION-bin.tar.gz"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.tar.gz"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$URL" -O "$ARCHIVE"
  else
    echo "curl or wget is required to bootstrap Maven" >&2
    exit 1
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
fi

exec "$MVN_BIN" "$@"
