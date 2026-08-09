#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.13"
PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BOOTSTRAP_ROOT="$PROJECT_ROOT/.gradle-bootstrap"
ARCHIVE="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_HOME="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION"

mkdir -p "$BOOTSTRAP_ROOT"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "Lade Gradle $GRADLE_VERSION von services.gradle.org ..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    echo "curl oder wget wird benoetigt." >&2
    exit 1
  fi
  unzip -q -o "$ARCHIVE" -d "$BOOTSTRAP_ROOT"
fi

cd "$PROJECT_ROOT"
"$GRADLE_HOME/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION"
chmod +x ./gradlew
./gradlew --no-daemon testDebugUnitTest assembleDebug

echo "APK erstellt: $PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
