#!/bin/bash

set -o nounset
set -o errexit

gradleCommand=$([[ -z "${APP_VERSION:+x}" ]] && echo "artifactoryPublish" || echo "bintrayUpload")

## check for required environment vars
missingVars=()
[[ -z "${JFROG_DEPLOY_USER+x}" ]] && missingVars+=(JFROG_DEPLOY_USER)
[[ -z "${JFROG_DEPLOY_KEY+x}" ]] && missingVars+=(JFROG_DEPLOY_KEY)

## publish and app version are only needed for tag builds
if [[ "${gradleCommand}" == "bintrayUpload" ]]; then
  [[ -z "${BINTRAY_PUBLISH+x}" ]] && missingVars+=(BINTRAY_PUBLISH)
fi

## exit early if env vars are missing
if [[ "${#missingVars[@]}" -gt 0 ]]; then
  echo "[travis-deploy] executing [${gradleCommand}] requires the environment vars [${missingVars[*]}]"
  exit 1
fi

## if we have the required env vars, run gradlew command
./gradlew --no-daemon "${gradleCommand}"
