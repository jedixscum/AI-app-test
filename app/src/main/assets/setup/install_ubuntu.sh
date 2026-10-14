#test
#!/data/data/com.termux/files/usr/bin/bash
set -eu

export PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
export PATH="$PREFIX/bin:$PATH"

if ! command -v proot-distro >/dev/null 2>&1; then
    echo "proot-distro is missing. Run base setup first."
    exit 1
fi

if proot-distro login ubuntu -- true >/dev/null 2>&1; then
    echo "Ubuntu is already installed and working."
    exit 0
fi

echo "Installing Ubuntu..."
proot-distro install ubuntu

echo "Verifying Ubuntu installation..."
"$PREFIX/bin/bash" -c '
    test -d "$PREFIX/var/lib/proot-distro/installed-rootfs/ubuntu"
' || {
    echo "Installation finished, but Ubuntu rootfs was not found."
    proot-distro list || true
    exit 1
}

proot-distro login ubuntu -- cat /etc/os-release
