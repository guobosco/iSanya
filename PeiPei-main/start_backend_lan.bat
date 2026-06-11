@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0start_backend_lan.ps1"
if errorlevel 1 (
  echo.
  echo Startup failed. Press any key to close...
  pause >nul
)
endlocal
