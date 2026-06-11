@echo off
REM Run the ATM system (runs build.bat first if not built yet).
cd /d "%~dp0"

if not exist out (
  echo Not built yet -- running build.bat first...
  call build.bat
  if errorlevel 1 (
    echo.
    pause
    exit /b 1
  )
)

java -cp "out;lib\postgresql-42.7.4.jar" ATMSystem

echo.
echo === ATM exited. Press any key to close this window. ===
pause >nul
