#!/usr/bin/env bash

# See the printHelp function for instructions on using this script.
# You can run ./merge_upstream.sh -h to view the output in your terminal.

# Capabilities of this script:
# - Pull remote branch state (usually this should be the upstream OpenTripPlanner dev-2.x branch).
# - Replace upstream CI actions with Digitransit setup.
# - If configured, push the result (upstream + local modifications) to Digitransit OTP v2 (default) or custom-release branch.

set -euo pipefail

DEVBRANCH=v2
REMOTE_REPO=$(git remote -v | grep -i "hsldevcom/OpenTripPlanner" | grep "push" | awk '{print $1;}')
DEFAULT_REMOTE=$(git config checkout.defaultRemote)
OTP_BASE=""
FORCE_PUSH_TO_BRANCH=""
CUSTOM_RELEASE=""

# bash colored output control characters
BASH_GREEN="\e[32m"
BASH_YELLOW="\e[33m"
BASH_RED="\e[31m"
BASH_ENDFORMAT="\e[0m"
BOLD="\e[1m"

echo_green() {
  echo -e "$BASH_GREEN$1$BASH_ENDFORMAT"
}

echo_yellow() {
  echo -e "$BASH_YELLOW$1$BASH_ENDFORMAT"
}

echo_red() {
  echo -e "$BASH_RED$1$BASH_ENDFORMAT"
}

echo_bold() {
  echo -e "$BOLD$1$BASH_ENDFORMAT"
}

function main() {
  setup "$@"
  resetDevelop
  configDigitransitCI
  logSuccess
}

function setup() {
  if [[ "$REMOTE_REPO" != "origin" ]]; then
    printHelp
    echo ""
    echo "The origin remote does not point to hsldevcom/OpenTripPlanner"
    exit 1
  fi

  if [[ "$DEFAULT_REMOTE" != "origin" ]]; then
    printHelp
    echo ""
    echo "The default remote is not set to origin"
    exit 1
  fi

  # Parse -b (mandatory), -p, and -c options and set appropriate variables when an option is given.
  while getopts "b:pc" opt; do
    case "$opt" in
    b)
      OTP_BASE="$OPTARG"
      ;;
    p)
      FORCE_PUSH_TO_BRANCH="-p"
      ;;
    c)
      CUSTOM_RELEASE="-c"
      DEVBRANCH="custom-release"
      ;;
    *)
      printHelp
      exit 1
      ;;
    esac
  done

  # If no OTP base branch/pathspec is given.
  if [[ -z "$OTP_BASE" ]]; then
    printHelp
    exit 1
  fi

  echo ""
  echo "Options: ${FORCE_PUSH_TO_BRANCH} ${CUSTOM_RELEASE}"
  echo "Git base branch/commit: ${OTP_BASE}"
  echo "Digitransit development branch: ${DEVBRANCH}"
  echo "Digitransit remote repo(pull/push): ${REMOTE_REPO}"
  echo ""

  if git diff-index --quiet HEAD --; then
    echo ""
    echo "OK - No local changes, prepare to checkout '${DEVBRANCH}'"
    echo ""
  else
    echo ""
    echo_red "You have local modification, the script will abort. Nothing done!"
    echo ""
    exit 2
  fi

  git fetch --all
}

function resetDevelop() {
  echo ""
  echo_bold "## ------------------------------------------------------------------------------------- ##"
  echo_bold "##   RESET '${DEVBRANCH}' TO '${OTP_BASE}'"
  echo_bold "## ------------------------------------------------------------------------------------- ##"
  echo ""
  echo "Would you like to reset the '${DEVBRANCH}' to '${OTP_BASE}'? "
  echo ""

  whatDoYouWant

  echo ""
  echo "Checkout '${DEVBRANCH}'"
  git checkout ${DEVBRANCH}

  echo ""
  echo "Reset '${DEVBRANCH}' branch to '${OTP_BASE}' (hard)"
  git reset --hard "${OTP_BASE}"
  echo ""
}

function configDigitransitCI() {
  git checkout "${DEVBRANCH}"
  rm -rf .github
  git checkout origin/digitransit_ext_config .github
  git commit -a -m "Configure Digitransit CI actions"
  if [[ -n "${FORCE_PUSH_TO_BRANCH}" ]]; then
    echo ""
    echo "Force pushing contents to '${DEVBRANCH}'"
    git push -f
  fi
}

function logSuccess() {
  echo ""
  echo_green "## ------------------------------------------------------------------------------------- ##"
  echo_green "##   UPSTREAM MERGE DONE  --  SUCCESS"
  echo_green "## ------------------------------------------------------------------------------------- ##"
  echo "   - '${REMOTE_REPO}/${DEVBRANCH}' reset to '${OTP_BASE}'"
  echo "   - 'digitransit_ext_config' CI features added"
  echo ""
  echo ""
}

function whatDoYouWant() {
  echo ""
  ANSWER=""

  read -r -p "Do you want to continue: [y:Yes or n:No]?" ANSWER

  # If the answer isn't yes, then exit the script.
  if [[ ! "${ANSWER,,}" =~ ^(y|yes)$ ]]; then
    exit 0
  fi
}

function printHelp() {
  echo ""
  echo -e "This script requires one argument, the base ${BOLD}branch${BASH_ENDFORMAT} or ${BOLD}commit${BASH_ENDFORMAT} to use for the output branch."
  echo "The output branch, v2 (default) or custom-release, is reset to this commit."
  echo "Changes are force pushed to the remote git repo if the -p flag is used."
  echo ""
  echo "Make sure origin is set to hsldevcom/OpenTripPlanner and that it is the default repository (git config checkout.defaultRemote origin)."
  echo "You also need to set up upstream OTP as a remote."
  echo ""
  echo_bold "Options:"
  echo ""
  echo "  MANDATORY:"
  echo "    -b : The base branch (or pathspec) to use for the output. Given as an argument to 'git reset --hard <argument>'."
  echo "  OPTIONAL:"
  echo "    -p : Force push to the selected branch, v2 (default) or custom-release, at the end of the script."
  echo "    -c : Use the custom-release branch instead of v2 for the output of this script."
  echo ""
  echo_bold "Usage:"
  echo ""
  echo " $ ./merge_upstream.sh -b upstream/dev-2.x"
  echo " $ ./merge_upstream.sh -b upstream/dev-2.x -p -c"
  echo ""
}

main "$@"
