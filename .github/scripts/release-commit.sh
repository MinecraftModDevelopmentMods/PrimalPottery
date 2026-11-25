#!/usr/bin/env bash
# Commit the post-release version and changelog directly to current main.
# Usage: release-commit.sh 0.0.1-alpha 0.0.2 0.0.1-alpha
set -euo pipefail

die() { echo "$1" >&2; exit 1; }
[[ $# -eq 3 ]] || die "Usage: release-commit.sh <version> <next> <tag>"
version=$1
next=$2
tag=$3
here=$(dirname "$0")

[[ -z "$(git status --porcelain)" ]] || die "The release checkout must be clean"

# Keep using the tagged scripts even if main has changed since the release build.
work=$(mktemp -d)
trap 'rm -f "$work/"*.sh "$work/tag-changelog.md"; rmdir "$work"' EXIT
cp "$here/release-version.sh" "$here/release-changelog.sh" "$here/release-bump.sh" "$work/"
git show "${tag}:CHANGELOG.md" > "$work/tag-changelog.md"
git fetch origin main
git checkout --detach origin/main
bash "$work/release-bump.sh" "$version" "$next" CHANGELOG.md "$work/tag-changelog.md" gradle.properties
git diff --stat -- gradle.properties CHANGELOG.md
if git diff --quiet -- gradle.properties CHANGELOG.md; then
  echo "main already has the next version and changelog heading."
  exit 0
fi
git add gradle.properties CHANGELOG.md
git -c user.name='github-actions[bot]' -c user.email='41898282+github-actions[bot]@users.noreply.github.com' \
  commit -m "Set the development version to ${next} after publishing ${tag}."
# Never force-push: concurrent commits must survive. Rerunning the job fetches
# fresh main and computes the update again instead of replaying a stale edit.
git push origin HEAD:refs/heads/main || die "Could not update main. If main advanced, rerun this job; otherwise check the action's push permissions."
