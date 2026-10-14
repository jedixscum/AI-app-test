grep '^password:' ~/.config/code-server/config.yaml | head -n1 | sed 's/^password:[[:space:]]*//'
