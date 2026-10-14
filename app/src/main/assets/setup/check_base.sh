for c in git node npm python curl openssl proot-distro; do
  command -v "$c" >/dev/null 2>&1 || { echo "missing: $c"; exit 1; }
done
echo "git $(git --version | cut -d' ' -f3), node $(node --version), python $(python --version 2>&1 | cut -d' ' -f2)"
