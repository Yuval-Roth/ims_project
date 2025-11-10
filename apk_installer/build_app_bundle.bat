@echo off
setlocal enabledelayedexpansion

REM =======================================================
REM  ApkInstaller Portable Builder - self-contained, no JRE required
REM =======================================================

REM ---- CONFIG ----
set APP_NAME=apk_installer
set MAIN_CLASS=MainKt
set JAR_FILE=apk-installer-jar-with-dependencies.jar
set JAR_PATH=target\%JAR_FILE%
set BUILD_DIR=%~dp0build
set APP_DIR=%BUILD_DIR%\app
set RUNTIME_DIR=%BUILD_DIR%\runtime
set DIST_DIR=%~dp0bundle

REM ---- CLEAN OLD ----
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%APP_DIR%"

REM ---- CHECK JAVA ----
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JDK 17+ and set JAVA_HOME.
    pause
    exit /b 1
)

echo.
echo === Building %APP_NAME% Portable Package ===
echo.

REM ---- 1. Build jar ----
echo Running mvn package...
call mvn clean package -DskipTests

REM ---- 2. Create trimmed runtime ----
echo Creating embedded Java runtime...
jlink ^
  --no-header-files ^
  --no-man-pages ^
  --compress=2 ^
  --strip-debug ^
  --add-modules java.base,java.desktop ^
  --output "%RUNTIME_DIR%"

if not exist "%RUNTIME_DIR%\bin\java.exe" (
    echo [ERROR] Failed to create runtime image.
    pause
    exit /b 1
)

REM ---- 3. Copy application jar ----
echo Copying application jar...
copy "%JAR_PATH%" "%APP_DIR%" >nul

REM ---- 4. Package into portable app-image ----
echo Packaging self-contained portable app...
jpackage ^
  --name "%APP_NAME%" ^
  --input "%APP_DIR%" ^
  --main-jar "%JAR_FILE%" ^
  --main-class "%MAIN_CLASS%" ^
  --type app-image ^
  --win-console ^
  --runtime-image "%RUNTIME_DIR%" ^
  --dest "%DIST_DIR%" ^
  --app-version 1.0.0 ^
  --icon "%~dp0apk_installer_icon.ico"

if not exist "%DIST_DIR%\%APP_NAME%\%APP_NAME%.exe" (
    echo [ERROR] Packaging failed.
    pause
    exit /b 1
)

REM ---- 5. Ensure empty apk folder exists ----
mkdir "%DIST_DIR%\%APP_NAME%\apk" >nul 2>nul

REM ---- 6. Rename bundle folder ----
pushd "%DIST_DIR%"
ren "%APP_NAME%" "%APP_NAME%_bundle"
popd

REM ---- 7. Clean up temporary build files ----
echo Cleaning up temporary build files...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"

REM ---- 8. Done ----
echo.
echo Portable build complete!
echo Output: %DIST_DIR%\%APP_NAME%_bundle\
echo.

pause
