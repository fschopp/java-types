#!/bin/bash
set -e
set -x

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd)
if source "${ROOT}/should_deploy_gh_pages.sh"; then
    mvn site -B -P ghpages
fi
