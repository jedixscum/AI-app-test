mkdir -p ~/.config/code-server
CFG=~/.config/code-server/config.yaml
PW=""
if [ -f "$CFG" ]; then
  PW=$(grep '^password:' "$CFG" | head -n1 | sed 's/^password:[[:space:]]*//')
fi
if [ -z "$PW" ]; then
  PW=$(openssl rand -hex 16)
fi
cat > "$CFG" <<CFGEOF
bind-addr: 127.0.0.1:8080
auth: password
password: $PW
cert: false
CFGEOF
chmod 600 "$CFG"
echo "wrote $CFG (loopback only, password auth)"
