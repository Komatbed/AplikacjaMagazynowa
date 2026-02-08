@echo off
echo ===================================================
echo   Building Warehouse App APK (v1.3)
echo ===================================================

REM Set JAVA_HOME explicitly (Using Android Studio JBR 17)
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

echo Using JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo Cleaning project...
call gradlew.bat clean -Dorg.gradle.java.home="%JAVA_HOME%"

echo.
echo ===================================================
echo   1. Building DEBUG APK...
echo ===================================================
call gradlew.bat :app:assembleDebug -Dorg.gradle.java.home="%JAVA_HOME%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Debug Build Failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo   2. Building RELEASE APK (Unsigned)
echo ===================================================
call gradlew.bat :app:assembleRelease -Dorg.gradle.java.home="%JAVA_HOME%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Release Build Failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo   Build Successful!
echo ===================================================
echo Debug APK:   app\build\outputs\apk\debug\app-debug.apk
echo Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
echo.
echo Note: Release APK is unsigned. You must sign it before installation
echo       or use the Debug APK for testing.
echo.
pause
