[ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/ubuntu" ] || { echo "ubuntu rootfs not found"; exit 1; }
timeout 90 proot-distro login ubuntu -- cat /etc/os-release | head -n 2
