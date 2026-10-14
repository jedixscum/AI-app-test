PIDF=~/.pocketdev/state/keepalive.pid
STARTF=~/.pocketdev/state/keepalive.started
[ -f "$PIDF" ] || { echo "no test has been started"; exit 2; }
NOW=$(date +%s)
START=$(cat "$STARTF" 2>/dev/null || echo "$NOW")
MIN=$(( (NOW - START) / 60 ))
if kill -0 "$(cat "$PIDF")" 2>/dev/null; then
  echo "alive after $MIN min"
  exit 0
else
  echo "KILLED: the test process is gone after $MIN min"
  exit 1
fi
