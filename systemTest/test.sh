#!/bin/sh

set -xeu

base_url=${TEST_BASE_URL:-"http://localhost:8080"}
expected_git_revision=$GIT_REVISION

echo "=== health check ==="
status_code=`curl -s -o /dev/null -w '%{http_code}' $base_url/-/health/all`
echo "status code: $status_code"
test $status_code -eq 200

echo "=== Git revision ==="
x_rev_header=`curl --dump-header - $base_url/-/system-info | grep -e '^X-Rev:' | tr -d '\r'`
echo "X-Rev header: $x_rev_header"
test "$x_rev_header" = "X-Rev: $expected_git_revision"
