#!/bin/sh
set -e
# wrapper will be created by GitHub Action if missing
if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
  # fallback: use system gradle (GitHub has it)
  exec gradle "$@"
fi
