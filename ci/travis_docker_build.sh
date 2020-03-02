#!/bin/bash

set -o nounset
set -o errexit

echo "[travis_docker_build] running base docker build"
docker build --build-arg TRAVIS=${TRAVIS} --build-arg TRAVIS_JOB_ID=${TRAVIS_JOB_ID} --target Build .

# push docker images when not executing a PR build
if [[ "${TRAVIS_PULL_REQUEST}" = "false" ]]; then
  docker_tag="${TRAVIS_TAG:-latest}"
  if [[ "${docker_tag}" != "latest" ]]; then
    echo "[travis_docker_build] Uploading maven artifacts to bintray"
    docker build --build-arg BINTRAY_USER=${BINTRAY_USER} --build-arg BINTRAY_KEY=${BINTRAY_KEY} \
                 --build-arg BINTRAY_PUBLISH=true --build-arg APP_VERSION=${docker_tag} --target BintrayPublish .
  fi

  echo "[travis_docker_build] Packaging docker images for tag ${docker_tag}"
  docker build --build-arg TRAVIS=${TRAVIS} --build-arg TRAVIS_JOB_ID=${TRAVIS_JOB_ID} --target Package \
               --tag ${TRAVIS_REPO_SLUG}:${docker_tag} --tag quay.io/${TRAVIS_REPO_SLUG}:${docker_tag} .

  echo "[travis_docker_build] dockerhub push for tag ${docker_tag}"
  docker login -u="${DOCKERHUB_USER}" -p="${DOCKERHUB_PASS}"
  docker push ${TRAVIS_REPO_SLUG}:${docker_tag}

  echo "[travis_docker_build] quay.io push for tag ${docker_tag}"
  docker login -u="${QUAYIO_USER}" -p="${QUAYIO_PASS}" quay.io
  docker push quay.io/${TRAVIS_REPO_SLUG}:${docker_tag}
else
  echo "[travis_docker_build] Skipping Docker publish for PR build"
fi
