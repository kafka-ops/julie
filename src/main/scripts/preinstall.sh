#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail
function default_user_creation_if_required {
  if id "julie-kafka" &>/dev/null;
  then
    echo "User julie-kafka already exist"
  else
    adduser -M -r julie-kafka
  fi
}

function verify_java_version_min_11 {
    current_version=$(java -version 2>&1 | tr "\n" " " | cut -d " " -f 3  | tr -d '"' | cut -d "." -f 1)
    if (( $current_version < 11 ))
    then
      return 1;
    fi
    return 0
}


default_user_creation_if_required

# Verify the existence of java command, if not there verification should exit already
type -P java &> /dev/null || { echo "java not found"; exit 1; }

verify_java_version_min_11
exit_code=$?

exit $exit_code