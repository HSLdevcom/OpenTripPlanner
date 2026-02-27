#!/usr/bin/env bash

# See the printHelp function for instructions on using this script.
# You can run ./merge_upstream.sh -h to view the output in your terminal.

# Capabilities of this script:
# - Pull remote branch state (usually this should be the upstream OpenTripPlanner dev-2.x branch).
# - Replace upstream CI actions with Digitransit setup.
# - If configured, push the result (upstream + local modifications) to Digitransit OTP v2 (default) or custom-release branch.

set -euo pipefail

SCRIPT_DIR=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)

DEFAULT_REMOTE=$(git config checkout.defaultRemote)
ORIGIN_REMOTE="origin"
HSLDEVCOM_REMOTE=$(git remote -v | grep -i "hsldevcom/OpenTripPlanner" | grep "push" | awk '{print $1;}')

OUTPUT_BRANCH=v2

OPTIONS=""
INCOMING_OTP_BASE=""
FORCE_PUSH_TO_BRANCH=""
DIFF_COMPARISON_BRANCH=""
SKIP_GIT_FETCH=""

# bash colored output control characters
BASH_GREEN="\e[32m"
BASH_YELLOW="\e[33m"
BASH_RED="\e[31m"
BASH_ENDFORMAT="\e[0m"
BOLD="\e[1m"

echo_green() {
  printf "$BASH_GREEN$1$BASH_ENDFORMAT\n"
}

echo_yellow() {
  printf "$BASH_YELLOW$1$BASH_ENDFORMAT\n"
}

echo_red() {
  printf "$BASH_RED$1$BASH_ENDFORMAT\n"
}

echo_bold() {
  printf "$BOLD$1$BASH_ENDFORMAT\n"
}

function main() {
  setup "$@"
  resetDevelop
  createChangelogDiffFile
  configDigitransitCI
  logSuccess
}

function setup() {
  checkOriginRemote

  # Parse -b (mandatory), -p, -c, -d, and -s options and set appropriate variables when an option is given.
  while getopts "b:pcd:s" opt; do
    case "$opt" in
    b)
      OPTIONS="$OPTIONS -b $OPTARG"
      INCOMING_OTP_BASE="$OPTARG"
      ;;
    p)
      OPTIONS="$OPTIONS -p"
      FORCE_PUSH_TO_BRANCH="true"
      ;;
    c)
      OPTIONS="$OPTIONS -c"
      OUTPUT_BRANCH="custom-release"
      ;;
    d)
      OPTIONS="$OPTIONS -d $OPTARG"
      DIFF_COMPARISON_BRANCH="$OPTARG"
      ;;
    s)
      OPTIONS="$OPTIONS -s"
      SKIP_GIT_FETCH="true"
      ;;
    *)
      printHelp
      exit 1
      ;;
    esac
  done

  checkDiffComparisonBranch
  checkIncomingOtpBase

  echo ""
  echo "Options:$OPTIONS"
  echo "Git base branch or commit object:         ${INCOMING_OTP_BASE}"
  if [[ -n "${FORCE_PUSH_TO_BRANCH}" ]]; then
    echo "Force push to branch:                     true"
  else
    echo "Force push to branch:                     false"
  fi
  echo "Diff comparison branch or commit object:  ${DIFF_COMPARISON_BRANCH}"
  if [[ -n "${SKIP_GIT_FETCH}" ]]; then
    echo "Skip git fetch:                           true"
  else
    echo "Skip git fetch:                           false"
  fi
  echo "Digitransit output branch:                ${OUTPUT_BRANCH}"
  echo "Digitransit remote repo:                  ${HSLDEVCOM_REMOTE}"
  echo ""

  if git diff-index --quiet HEAD --; then
    echo ""
    echo "OK - No local changes, prepare to checkout '${OUTPUT_BRANCH}'"
    echo ""
  else
    echo ""
    echo_red "You have local modification, the script will abort. Nothing done!"
    echo ""
    exit 2
  fi

  if [[ -n "${SKIP_GIT_FETCH}" ]]; then
    echo "Skipping 'git fetch --all'"
  else
    git fetch --all
  fi
}

function resetDevelop() {
  echo ""
  echo_bold "## ------------------------------------------------------------------------------------- ##"
  echo_bold "##   RESET '${OUTPUT_BRANCH}' BRANCH TO '${INCOMING_OTP_BASE}'"
  echo_bold "## ------------------------------------------------------------------------------------- ##"
  echo ""
  echo "Would you like to reset the '${OUTPUT_BRANCH}' branch to '${INCOMING_OTP_BASE}'? "
  echo ""

  whatDoYouWant

  echo ""
  echo "Checkout '${OUTPUT_BRANCH}'"
  git checkout ${OUTPUT_BRANCH}

  echo ""
  echo "Reset '${OUTPUT_BRANCH}' branch to '${INCOMING_OTP_BASE}' (hard)"
  git reset --hard "${INCOMING_OTP_BASE}"
  echo ""
}

function createChangelogDiffFile() {
  mkdir -p "$SCRIPT_DIR/digitransit"
  echo "# Digitransit OTP Release Summary" > "$SCRIPT_DIR/digitransit/RELEASE_CHANGELOG.md"
  python3 "$SCRIPT_DIR/script/changelog-diff.py" $DIFF_COMPARISON_BRANCH $INCOMING_OTP_BASE >> "$SCRIPT_DIR/digitransit/RELEASE_CHANGELOG.md"

  git add "$SCRIPT_DIR/digitransit"
  git commit -m "Create RELEASE_CHANGELOG.md"
}

function configDigitransitCI() {
  rm -rf .github
  git checkout origin/digitransit_ext_config .github
  git commit -a -m "Configure Digitransit CI actions"
  if [[ -n "${FORCE_PUSH_TO_BRANCH}" ]]; then
    echo ""
    echo "Force pushing contents to '${OUTPUT_BRANCH}'"
    git push -f
  fi
}

function logSuccess() {
  echo ""
  echo_green "## ------------------------------------------------------------------------------------- ##"
  echo_green "##   UPSTREAM MERGE DONE  --  SUCCESS"
  echo_green "## ------------------------------------------------------------------------------------- ##"
  echo "   - '${HSLDEVCOM_REMOTE}/${OUTPUT_BRANCH}' reset to '${INCOMING_OTP_BASE}'"
  echo "   - 'digitransit_ext_config' CI features added"
  echo ""
  echo ""
}

function whatDoYouWant() {
  echo ""
  ANSWER=""

  read -r -p "Do you want to continue: [y:Yes or n:No]? " ANSWER

  ANSWER_LOWERCASE=$(echo "$ANSWER" | tr '[:upper:]' '[:lower:]')
  # If the answer isn't yes, then exit the script.
  if [[ ! "${ANSWER_LOWERCASE}" =~ ^(y|ye|yes)$ ]]; then
    exit 0
  fi
}

function checkOriginRemote() {
  if [[ "$HSLDEVCOM_REMOTE" != "$ORIGIN_REMOTE" ]]; then
    printHelp
    echo ""
    echo "The origin remote does not point to hsldevcom/OpenTripPlanner"
    exit 1
  fi

  if [[ "$DEFAULT_REMOTE" != "$ORIGIN_REMOTE" ]]; then
    printHelp
    echo ""
    echo "The default remote is not set to origin"
    exit 1
  fi
}

function checkDiffComparisonBranch() {
  # If no diff comparison branch or commit object argument is given.
  if [[ -z "$DIFF_COMPARISON_BRANCH" ]]; then
    # Assign the latest release tag.
    DIFF_COMPARISON_BRANCH=$(curl -s https://api.github.com/repos/HSLdevcom/OpenTripPlanner/releases/latest | jq -r .tag_name)
  fi
}

function checkIncomingOtpBase() {
  # If no OTP base branch or commit object argument is given.
  if [[ -z "$INCOMING_OTP_BASE" ]]; then
    printHelp
    exit 1
  fi
}

function printHelp() {
  echo ""
  printf "This script requires one argument, the base ${BOLD}branch${BASH_ENDFORMAT} or ${BOLD}commit${BASH_ENDFORMAT} to use for the output branch.\n"
  echo "The output branch, v2 (default) or custom-release, is reset to this commit."
  echo "Changes are force pushed to the remote git repo if the -p flag is used."
  echo ""
  echo "Make sure origin is set to hsldevcom/OpenTripPlanner and that it is the default repository (git config checkout.defaultRemote origin)."
  echo "You also need to set up upstream OTP as a remote."
  echo ""
  echo_bold "Options:"
  echo ""
  echo "  MANDATORY:"
  echo "    -b : The base branch (or commit object) argument to use for the output. Given as an argument to 'git reset --hard <argument>'."
  echo "  OPTIONAL:"
  echo "    -p : Force push to the selected branch, v2 (default) or custom-release, at the end of the script."
  echo "    -c : Use the custom-release branch instead of v2 for the output of this script."
  echo "    -d : Define a changelog diff comparison branch (or commit object) that the new incoming changelog will be compared to."
  echo "         If nothing is specified, the latest Digitransit OTP release tag is used."
  echo "    -s : Skip the 'git fetch --all' command."
  echo ""
  echo_bold "Usage:"
  echo ""
  echo " $ ./merge_upstream.sh -b upstream/dev-2.x"
  echo " $ ./merge_upstream.sh -b upstream/dev-2.x -p -c"
  echo ""
}

main "$@"
