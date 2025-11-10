@echo off
setlocal enabledelayedexpansion

REM =======================================================
REM  ApkInstaller Portable Builder - self-contained, no JRE required
REM =======================================================

REM ---- CONFIG ----
set APP_NAME=apk_installer_bundle
set MAIN_CLASS=MainKt
set JAR_FILE=apk-installer-jar-with-dependencies.jar
set JAR_PATH=target\%JAR_FILE%
set BUILD_DIR=%~dp0build
set APP_DIR=%BUILD_DIR%\app
set RUNTIME_DIR=%BUILD_DIR%\runtime
set DIST_DIR=%~dp0bundle

REM ---- CLEANUP OLD ----
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

REM ---- 1. (Optional) Build JAR ----
if exist pom.xml (
    echo Running mvn package...
    call mvn clean package -DskipTests
)

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

REM ---- 3. Copy application files ----
echo Copying app files...
copy "%JAR_PATH%" "%APP_DIR%" >nul
if exist "tools" xcopy "tools" "%APP_DIR%\tools" /E /I /Y >nul
if exist "apk"   xcopy "apk"   "%APP_DIR%\apk"   /E /I /Y >nul

REM ---- 4. Create CMD launcher ----
echo Creating portable CMD launcher...
echo @echo off > "%APP_DIR%\%APP_NAME%.cmd"
echo setlocal >> "%APP_DIR%\%APP_NAME%.cmd"
echo set JAVA_HOME=%%~dp0runtime >> "%APP_DIR%\%APP_NAME%.cmd"
echo set PATH=%%JAVA_HOME%%\bin;%%PATH%% >> "%APP_DIR%\%APP_NAME%.cmd"
echo java -cp "%%~dp0%JAR_FILE%" %MAIN_CLASS% %%* >> "%APP_DIR%\%APP_NAME%.cmd"
echo endlocal >> "%APP_DIR%\%APP_NAME%.cmd"

REM ---- 5. Package into portable app-image ----
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
  --app-version 1.0.0

if not exist "%DIST_DIR%\%APP_NAME%\%APP_NAME%.exe" (
    echo [ERROR] Packaging failed.
    pause
    exit /b 1
)

REM ---- 6. CLEAN UP TEMP FILES ----
echo Cleaning up temporary build files...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"

REM ---- 7. DONE ----
echo.
echo Portable build complete!
echo Output: %DIST_DIR%\%APP_NAME%\
echo.
echo You can zip that folder and run it anywhere - Java not required.
echo.

pause
