@echo off
REM wsc Windows 빌드 스크립트
REM 사전 요건: Node.js 18+, npm install 완료
REM 출력: cli\dist\wsc-win.exe

cd /d "%~dp0"

if not exist node_modules (
  echo [wsc-build] npm install 실행 중...
  npm install
)

if not exist dist mkdir dist

echo [wsc-build] Windows ^(x64^) 빌드 중...
node_modules\.bin\pkg . --target node18-win-x64 --output dist\wsc-win.exe

if %errorlevel% neq 0 (
  echo [wsc-build] 빌드 실패.
  exit /b 1
)

echo [wsc-build] -^> dist\wsc-win.exe
echo [wsc-build] 완료.
dir dist\wsc-win.exe
