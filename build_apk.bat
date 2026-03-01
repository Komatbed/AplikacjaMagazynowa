@echo off
cd /d "%~dp0"
echo ===================================================
echo   Building Ferplast-magazyn APK (v1.3)
echo ===================================================

REM Set JAVA_HOME explicitly (Using Android Studio JBR 21)
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

echo Using JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo Killing all Java processes to release file locks...
taskkill /F /IM java.exe >nul 2>&1

echo.
echo Force deleting build directory...
if exist "app\build" (
    rmdir /s /q "app\build"
    if exist "app\build" (
        echo [WARN] Failed to fully delete app\build. Retrying...
        timeout /t 2 >nul
        rmdir /s /q "app\build"
    )
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
echo Renaming debug APK to Ferplast-magazyn-debug.apk...
set "DEBUG_DIR=app\build\outputs\apk\debug"
if exist "%DEBUG_DIR%\Ferplast-magazyn-debug.apk" del "%DEBUG_DIR%\Ferplast-magazyn-debug.apk"
if exist "%DEBUG_DIR%\app-debug.apk" (
    ren "%DEBUG_DIR%\app-debug.apk" "Ferplast-magazyn-debug.apk"
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
echo   3. Copying APK to target folder
echo ===================================================
set "TARGET_DIR=G:\Mój dysk\Development"

if not exist "%TARGET_DIR%" (
    echo [INFO] Creating target directory: "%TARGET_DIR%"
    mkdir "%TARGET_DIR%"
)

if not exist "%TARGET_DIR%" (
    echo [ERROR] Target directory could not be created: "%TARGET_DIR%"
) else (
    if exist "app\build\outputs\apk\debug\Ferplast-magazyn-debug.apk" (
        copy /Y "app\build\outputs\apk\debug\Ferplast-magazyn-debug.apk" "%TARGET_DIR%\Ferplast-magazyn-debug.apk" >nul
        echo Copied debug APK to: "%TARGET_DIR%\Ferplast-magazyn-debug.apk"
    ) else (
        echo [WARN] Debug APK not found: app\build\outputs\apk\debug\Ferplast-magazyn-debug.apk
    )

    if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
        copy /Y "app\build\outputs\apk\release\app-release-unsigned.apk" "%TARGET_DIR%\Ferplast-magazyn-release-unsigned.apk" >nul
        echo Copied release APK to: "%TARGET_DIR%\Ferplast-magazyn-release-unsigned.apk"
    ) else (
        echo [WARN] Release APK not found: app\build\outputs\apk\release\app-release-unsigned.apk
    )
)

echo.
echo ===================================================
echo   Build Finished
echo ===================================================
if exist "app\build\outputs\apk\debug\Ferplast-magazyn-debug.apk" (
    echo Debug APK:   app\build\outputs\apk\debug\Ferplast-magazyn-debug.apk
) else (
    echo Debug APK:   [NOT FOUND] app\build\outputs\apk\debug\Ferplast-magazyn-debug.apk"
)
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
) else (
    echo Release APK: [NOT FOUND] app\build\outputs\apk\release\app-release-unsigned.apk
)
echo.
pause
