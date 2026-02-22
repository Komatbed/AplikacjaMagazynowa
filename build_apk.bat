@echo off
cd /d "%~dp0"
echo ===================================================
echo   Building Warehouse App APK (v1.3)
echo ===================================================

REM Set JAVA_HOME explicitly (Using Android Studio JBR 17)
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

echo Using JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo Checking for conflicting Gradle Java processes for this project...
where powershell >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    powershell -NoLogo -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*ApliakcjaAndroidowa*gradle*' } | ForEach-Object { Write-Host ('Stopping PID {0}' -f $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }"
) else (
    echo [WARN] PowerShell not found, skipping auto-kill of Gradle processes.
)

echo.
echo Cleaning project...
call gradlew.bat clean --no-daemon -Dorg.gradle.java.home="%JAVA_HOME%"

echo.
echo ===================================================
echo   1. Building DEBUG APK...
echo ===================================================
call gradlew.bat :app:assembleDebug --no-daemon -Dorg.gradle.java.home="%JAVA_HOME%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Debug Build Failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Renaming debug APK to WarehouseApp-debug.apk...
set "DEBUG_DIR=app\build\outputs\apk\debug"
if exist "%DEBUG_DIR%\WarehouseApp-debug.apk" del "%DEBUG_DIR%\WarehouseApp-debug.apk"
if exist "%DEBUG_DIR%\app-debug.apk" (
    ren "%DEBUG_DIR%\app-debug.apk" "WarehouseApp-debug.apk"
) else (
    echo [WARN] Expected debug APK not found: "%DEBUG_DIR%\app-debug.apk"
)

echo.
echo ===================================================
echo   2. Building RELEASE APK (Unsigned)
echo ===================================================
call gradlew.bat :app:assembleRelease --no-daemon -Dorg.gradle.java.home="%JAVA_HOME%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Release Build Failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo   Build Finished
echo ===================================================
if exist "app\build\outputs\apk\debug\WarehouseApp-debug.apk" (
    echo Debug APK:   app\build\outputs\apk\debug\WarehouseApp-debug.apk
) else (
    echo Debug APK:   [NOT FOUND] app\build\outputs\apk\debug\WarehouseApp-debug.apk
)
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
) else (
    echo Release APK: [NOT FOUND] app\build\outputs\apk\release\app-release-unsigned.apk
)
echo.
echo Note: Release APK is unsigned. You must sign it before installation
echo       or use the Debug APK for testing.
echo.
pause
