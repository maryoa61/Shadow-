#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SINGBOX_VERSION="1.13.19"
ABIS="${SHADOW_NET_ABIS:-arm64-v8a armeabi-v7a x86 x86_64}"
SINGBOX_DIR="$ROOT/app/src/main/jniLibs"
MARKER="$ROOT/app/.vpn-core-versions"
EXPECTED="sing-box=v$SINGBOX_VERSION
abis=$ABIS"

if [[ -f "$MARKER" ]] && [[ "$(cat "$MARKER")" == "$EXPECTED" ]]; then
  missing=false
  for abi in $ABIS; do
    [[ -f "$SINGBOX_DIR/$abi/libsingbox.so" ]] || missing=true
  done
  if [[ "$missing" == false ]]; then
    echo "sing-box v$SINGBOX_VERSION is already present."
    exit 0
  fi
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$SINGBOX_DIR"

fetch() {
  local url="$1" output="$2"
  echo "Downloading $(basename "$output")…"
  curl --fail --location --silent --show-error \
    --retry 6 --retry-delay 2 --retry-all-errors \
    "$url" --output "$output"
}

for abi in $ABIS; do
  case "$abi" in
    arm64-v8a) archive_arch="arm64" ;;
    armeabi-v7a) archive_arch="arm" ;;
    x86) archive_arch="386" ;;
    x86_64) archive_arch="amd64" ;;
    *) echo "Unsupported ABI: $abi" >&2; exit 1 ;;
  esac

  archive="$TMP/sing-box-$archive_arch.tar.gz"
  extract_dir="$TMP/sing-box-$archive_arch"
  fetch \
    "https://github.com/SagerNet/sing-box/releases/download/v$SINGBOX_VERSION/sing-box-$SINGBOX_VERSION-android-$archive_arch.tar.gz" \
    "$archive"
  mkdir -p "$extract_dir"
  tar -xzf "$archive" -C "$extract_dir"
  binary="$(find "$extract_dir" -type f -name sing-box -print -quit)"
  if [[ -z "$binary" ]]; then
    echo "sing-box binary was not found in $archive" >&2
    exit 1
  fi
  mkdir -p "$SINGBOX_DIR/$abi"
  install -m 0755 "$binary" "$SINGBOX_DIR/$abi/libsingbox.so"
done

printf '%s' "$EXPECTED" > "$MARKER"
echo "Installed sing-box v$SINGBOX_VERSION for: $ABIS"
