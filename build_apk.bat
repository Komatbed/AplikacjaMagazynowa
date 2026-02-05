@echo off
echo ===================================================
echo   Building Warehouse App APK (Debug)
echo ===================================================

REM Set JAVA_HOME explicitly if needed (uncomment and adjust if auto-detection fails)
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

echo Using JAVA_HOME: %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo Cleaning project...
call gradlew.bat clean -Dorg.gradle.java.home="%JAVA_HOME%"

echo.
echo Building Debug APK...
call gradlew.bat :app:assembleDebug -Dorg.gradle.java.home="%JAVA_HOME%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build Failed!
    echo Please check the error messages above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo   Build Successful!
echo ===================================================
echo APK location: app\build\outputs\apk\debug\app-debug.apk
echo.
pause