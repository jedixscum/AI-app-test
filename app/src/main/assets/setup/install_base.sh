export DEBIAN_FRONTEND=noninteractive
pkg update -y
pkg upgrade -y -o Dpkg::Options::="--force-confold"
pkg install -y git nodejs-lts python curl openssl-tool proot-distro
mkdir -p ~/projects ~/.pocketdev/state ~/.pocketdev/logs
echo "base tools installed"
