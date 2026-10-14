health() { curl -fsS --max-time 2 http://127.0.0.1:8080/healthz >/dev/null 2>&1; }
if health; then echo "already running"; exit 0; fi
mkdir -p ~/projects ~/.pocketdev/logs ~/.pocketdev/state
termux-wake-lock 2>/dev/null || true
nohup code-server ~/projects > ~/.pocketdev/logs/code-server.log 2>&1 &
echo $! > ~/.pocketdev/state/code-server.pid
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  sleep 1
  if health; then echo "started"; exit 0; fi
done
echo "code-server did not become healthy within 20s"
tail -n 20 ~/.pocketdev/logs/code-server.log
exit 1
