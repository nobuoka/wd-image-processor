#!/bin/bash

set -xe

pid=0
terminating=0

function on_termination_requested () {
  terminating=1
  terminate_if_needed
}

function terminate_if_needed () {
  echo "pid: $pid, terminating: $terminating"

  if [ $pid -ne 0 -a $terminating -ne 0 ]; then
    kill $pid
    wait $pid
    s=$!
    if [ $s -eq 0 -o $s -eq 143 ]; then
      exit 0
    else
      exit $s
    fi
  fi
}

trap 'on_termination_requested' 2 3 15

./build/install/wdip/bin/wdip start &
pid=$!
terminate_if_needed
echo $pid
wait $pid
