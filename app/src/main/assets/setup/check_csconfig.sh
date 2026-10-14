CFG=~/.config/code-server/config.yaml
[ -f "$CFG" ] || { echo "config missing"; exit 1; }
grep -q '^bind-addr: 127\.0\.0\.1:8080' "$CFG" || { echo "bind-addr is not 127.0.0.1:8080"; exit 1; }
grep -q '^auth: password' "$CFG" || { echo "password auth not enabled"; exit 1; }
grep -q '^password: .\+' "$CFG" || { echo "password not set"; exit 1; }
echo "code-server bound to loopback with password auth"
