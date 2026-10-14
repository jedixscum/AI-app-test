
#!/data/data/com.termux/files/usr/bin/bash
set -eu

export PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
export PATH="$PREFIX/bin:$PATH"

if ! command -v proot-distro >/dev/null 2>&1; then
    echo "proot-distro command not found"
    exit 1
fi

ROOTFS="$PREFIX/var/lib/proot-distro/installed-rootfs/ubuntu"

if [ ! -d "$ROOTFS" ]; then
    echo "Ubuntu rootfs not found at: $ROOTFS"
    echo "Checking installed distributions:"
    proot-distro list || true
    exit 1
fi

echo "Ubuntu rootfs found. Testing login..."
timeout 90 proot-distro login ubuntu -- cat /etc/os-release
