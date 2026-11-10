#!/usr/bin/env bash
set -eo pipefail

# ANSI color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}  Running Local CI Pre-Flight Quality Gate Check    ${NC}"
echo -e "${BLUE}====================================================${NC}"

# Resolve project root and gradlew
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$DIR"

# Ensure JDK is available (default to Android Studio JBR if JAVA_HOME not set)
if [ -z "$JAVA_HOME" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

echo -e "\n${BLUE}[1/5] Running KtLint formatting check...${NC}"
./gradlew ktlintCheck --no-daemon

echo -e "\n${BLUE}[2/5] Running Detekt static code analysis...${NC}"
./gradlew detekt --no-daemon

echo -e "\n${BLUE}[3/5] Running Android Lint...${NC}"
./gradlew lintDebug --no-daemon

echo -e "\n${BLUE}[4/5] Running Unit Tests & JaCoCo Coverage...${NC}"
./gradlew testDebugUnitTest jacocoTestReport --no-daemon

echo -e "\n${BLUE}[5/5] Assembling Debug APK...${NC}"
./gradlew assembleDebug --no-daemon

echo -e "\n${GREEN}====================================================${NC}"
echo -e "${GREEN}  ✓ ALL LOCAL CI QUALITY GATES PASSED SUCCESSFULLY! ${NC}"
echo -e "${GREEN}====================================================${NC}"
