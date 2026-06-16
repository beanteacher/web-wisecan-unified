#!/usr/bin/env bash
# wsc 멀티 플랫폼 빌드 스크립트 (Linux/macOS CI 환경)
#
# 사전 요건: Node.js 18+, pkg (npm i -g pkg 또는 로컬 devDependencies)
# 출력 경로: cli/dist/
#
# 사용:
#   chmod +x build.sh
#   ./build.sh            # 전체 3 OS 빌드
#   ./build.sh win        # Windows 만
#   ./build.sh mac        # macOS 만
#   ./build.sh linux      # Linux 만

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PKG="./node_modules/.bin/pkg"
if [ ! -f "$PKG" ]; then
  echo "[wsc-build] pkg 가 없습니다. npm install 을 먼저 실행하세요."
  exit 1
fi

mkdir -p dist

TARGET="${1:-all}"

build_win() {
  echo "[wsc-build] Windows (x64) 빌드 중..."
  "$PKG" . --target node18-win-x64 --output dist/wsc-win.exe
  echo "[wsc-build] -> dist/wsc-win.exe"
}

build_mac() {
  echo "[wsc-build] macOS (x64) 빌드 중..."
  "$PKG" . --target node18-macos-x64 --output dist/wsc-macos
  chmod +x dist/wsc-macos
  echo "[wsc-build] -> dist/wsc-macos"
}

build_linux() {
  echo "[wsc-build] Linux (x64) 빌드 중..."
  "$PKG" . --target node18-linux-x64 --output dist/wsc-linux
  chmod +x dist/wsc-linux
  echo "[wsc-build] -> dist/wsc-linux"
}

case "$TARGET" in
  win)   build_win ;;
  mac)   build_mac ;;
  linux) build_linux ;;
  all)   build_win; build_mac; build_linux ;;
  *)
    echo "알 수 없는 타겟: $TARGET (win|mac|linux|all)"
    exit 1
    ;;
esac

echo "[wsc-build] 빌드 완료."
ls -lh dist/
