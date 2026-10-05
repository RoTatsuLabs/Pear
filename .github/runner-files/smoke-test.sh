#!/usr/bin/env bash
set -e

adb install app-smoke-test.apk
adb logcat -c
adb shell monkey -p yos.music.player -c android.intent.category.LAUNCHER 1
sleep 20

if ! adb shell pidof yos.music.player > /dev/null; then
  echo "::error::Pear process is not running 20s after launch - it likely crashed on startup."
  adb logcat -d | tail -n 200
  exit 1
fi

if adb logcat -d | grep -qE "FATAL EXCEPTION|Fatal signal|thread\.cc.*runtime aborting"; then
  echo "::error::Found a fatal crash in logcat after launch."
  adb logcat -d | grep -E "FATAL EXCEPTION|Fatal signal|thread\.cc.*runtime aborting" -A 30
  exit 1
fi
