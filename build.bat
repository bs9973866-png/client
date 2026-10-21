@echo off
setlocal
where gradle >nul 2>nul
if %errorlevel% neq 0 (
  echo Gradle is not installed. Use GitHub Actions instead: Actions ^> Build NIGHTFALL.
  exit /b 1
)
gradle build --no-daemon
if %errorlevel% neq 0 exit /b %errorlevel%
echo Build complete. JAR is in build\libs
