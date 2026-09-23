#!/usr/bin/env bash
# OmniAgent GitHub Push Automation Script
set -e

REPO_NAME="OmniAgent"

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: ./push-to-github.sh <GITHUB_USERNAME> <GITHUB_PERSONAL_ACCESS_TOKEN> [REPO_NAME]"
  echo ""
  echo "Example:"
  echo "  ./push-to-github.sh octocat ghp_xxxxxxxxxxxxxxxxxxxx OmniAgent"
  echo ""
  echo "Or create an empty repository on GitHub first, then run:"
  echo "  git remote add origin https://<USERNAME>:<TOKEN>@github.com/<USERNAME>/OmniAgent.git"
  echo "  git push -u origin main"
  exit 1
fi

GITHUB_USER="$1"
GITHUB_TOKEN="$2"
if [ ! -z "$3" ]; then
  REPO_NAME="$3"
fi

echo "=== 1. Checking / Creating remote GitHub repository '$REPO_NAME' ==="
# Try to create repository via GitHub REST API
CREATE_RES=$(curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/user/repos \
  -d "{\"name\":\"$REPO_NAME\",\"description\":\"Autonomous Android OS agentic assistant that carries out any task across any app via simple voice commands.\",\"private\":false}")

echo "Response received from GitHub API."

echo "=== 2. Configuring Git Remote ==="
cd "$(dirname "$0")"
git remote remove origin 2>/dev/null || true
git remote add origin "https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/${GITHUB_USER}/${REPO_NAME}.git"

echo "=== 3. Pushing branch 'main' to GitHub ==="
git branch -M main
git push -u origin main --force

echo ""
echo "🎉 SUCCESS! Repository pushed to: https://github.com/${GITHUB_USER}/${REPO_NAME}"
