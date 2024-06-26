#!/usr/bin/env bash
set -e

CURRENT_BRANCH="$( git branch --show-current )"

if [[ "$CURRENT_BRANCH" == "develop" || "$CURRENT_BRANCH" == "main" ]]; then
  EFFECTIVE_BRANCH_NAME="$CURRENT_BRANCH"

else
  case "$( since project version --current --log-level=info )" in
  v3.*)
    EFFECTIVE_BRANCH_NAME="main"
    ;;
  v2.*)
    EFFECTIVE_BRANCH_NAME="release/2.x"
    ;;
  *)
    EFFECTIVE_BRANCH_NAME="dev"
    ;;
  esac
fi

echo "$EFFECTIVE_BRANCH_NAME"
