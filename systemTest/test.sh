#!/bin/sh

set -xeu

base_url=${TEST_BASE_URL:-"http://localhost:8080"}

status_code=`curl -s -o /dev/null -w '%{http_code}' $base_url/-/health/all`
echo "status code: $status_code"
test $status_code -eq 200
