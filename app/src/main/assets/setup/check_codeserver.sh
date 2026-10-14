command -v code-server >/dev/null 2>&1 || { echo "code-server not installed"; exit 1; }
code-server --version | head -n 1
