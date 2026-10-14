if [ -f ~/.pocketdev/state/code-server.pid ]; then
  kill "$(cat ~/.pocketdev/state/code-server.pid)" 2>/dev/null || true
  rm -f ~/.pocketdev/state/code-server.pid
fi
# The [c] keeps pkill from matching this script's own command line.
pkill -f "[c]ode-server" 2>/dev/null || true
sleep 1
echo "stopped"
