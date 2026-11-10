#!/bin/bash
set -e

# Validate required variables are present
if [ -z "${REPO_URL}" ] || [ -z "${RUNNER_TOKEN}" ]; then
  echo "Error: REPO_URL and RUNNER_TOKEN environment variables must be set."
  exit 1
fi

# Set a default runner name if not provided
RUNNER_NAME=${RUNNER_NAME:-weeklyactivity-docker-runner}

echo "Configuring GitHub Actions Runner: ${RUNNER_NAME}..."

# Run the config script silently
./config.sh \
  --url "${REPO_URL}" \
  --token "${RUNNER_TOKEN}" \
  --name "${RUNNER_NAME}" \
  --work "_work" \
  --unattended \
  --replace

echo "GitHub Actions Runner registered successfully. Starting execution loop..."

# Start the runner execution agent
./run.sh
