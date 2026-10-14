mkdir -p ~/.pocketdev/state
nohup sleep 86400 > /dev/null 2>&1 &
echo $! > ~/.pocketdev/state/keepalive.pid
date +%s > ~/.pocketdev/state/keepalive.started
echo "test process started (pid $(cat ~/.pocketdev/state/keepalive.pid))"
