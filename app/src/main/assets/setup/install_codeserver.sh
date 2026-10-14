# code-server from the Termux User Repository (TUR). Verify this is still the supported route.
export DEBIAN_FRONTEND=noninteractive
pkg install -y tur-repo
pkg update -y
pkg install -y code-server
