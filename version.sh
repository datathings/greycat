#!/usr/bin/env bash

# shellcheck disable=SC2155
export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")
export GIT_LAST_TAG=`git describe --abbrev=0 --tags`
export GIT_COMMITS_SINCE_LAST_TAG=`git rev-list ${GIT_LAST_TAG}..HEAD --count`
export GIT_COMMIT_ID=`git rev-parse --short HEAD`
export REPO_VERSION="${GIT_LAST_TAG}.${GIT_COMMITS_SINCE_LAST_TAG}"
echo "VERSION=${REPO_VERSION}"

mvn --no-transfer-progress versions:set -DnewVersion="${REPO_VERSION}"