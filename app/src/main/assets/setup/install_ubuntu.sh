if [ -d "$PREFIX/var/lib/proot-distro/installed-rootfs/ubuntu" ]; then
  echo "ubuntu already present; nothing to download"
  exit 0
fi
proot-distro install ubuntu
