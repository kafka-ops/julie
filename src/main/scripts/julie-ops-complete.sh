#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail

function __julie-ops () {
	local base_ops="--brokers|--clientConfig|--dryRun|--help|--overridingClientConfig|--plans|--quite|--topology|--validate|--version"
        local cur=${COMP_WORDS[COMP_CWORD]}
	COMPREPLY=($(IFS='|' compgen -S ' ' -W "$base_ops" -- $cur ) )
}

complete -A alias -F __julie-ops	julie-ops
