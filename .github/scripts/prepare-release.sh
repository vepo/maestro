#!/usr/bin/env bash
# Bumps SNAPSHOT → release, tags v<version>, then bumps patch and restores -SNAPSHOT.
set -euo pipefail

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

CURRENT="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
if [[ "${CURRENT}" != *-SNAPSHOT ]]; then
  echo "Expected a SNAPSHOT version on main, got: ${CURRENT}" >&2
  exit 1
fi

RELEASE="${CURRENT%-SNAPSHOT}"
TAG="v${RELEASE}"

IFS='.' read -r major minor patch <<< "${RELEASE}"
NEXT="${major}.${minor}.$((patch + 1))-SNAPSHOT"

set_version() {
  mvn -B org.codehaus.mojo:versions-maven-plugin:2.18.0:set \
    -DnewVersion="${1}" \
    -DgenerateBackupPoms=false \
    -DprocessAllModules=true
  mvn -B org.codehaus.mojo:versions-maven-plugin:2.18.0:commit \
    -DgenerateBackupPoms=false
}

set_version "${RELEASE}"
git add -A
git commit -m "Prepare release ${RELEASE}"

git tag -a "${TAG}" -m "Release ${RELEASE}"

set_version "${NEXT}"
git add -A
git commit -m "Prepare for next development iteration ${NEXT}"

git push origin HEAD
git push origin "${TAG}"

{
  echo "release_version=${RELEASE}"
  echo "tag=${TAG}"
  echo "next_snapshot=${NEXT}"
} >> "${GITHUB_OUTPUT}"

echo "Tagged ${TAG}; main is now ${NEXT}"
