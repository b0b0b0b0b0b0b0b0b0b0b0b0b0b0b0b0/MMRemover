@echo off
cd /d "%~dp0"

set "JAVAW="
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-25*") do if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe" & goto java_ok
for /d %%d in ("C:\Program Files\Java\jdk-25*") do if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe" & goto java_ok
for /d %%d in ("C:\Program Files\Microsoft\jdk-25*") do if exist "%%d\bin\javaw.exe" set "JAVAW=%%d\bin\javaw.exe" & goto java_ok

echo.
echo [ERROR] Java 25 not found. MMRemover needs Java 25.
echo Install: https://adoptium.net/temurin/releases/?version=25
echo Enable: Set JAVA_HOME + Add to PATH, then reboot.
echo.
pause
exit /b 1

:java_ok
set "JAR="
for /f "delims=" %%f in ('dir /b /o-d MMRemover*.jar 2^>nul') do set "JAR=%%f" & goto run

echo.
echo [ERROR] MMRemover-*.jar not found in this folder.
echo Put MMRemover jar next to start.bat (same folder).
echo.
pause
exit /b 1

:run
echo Starting %JAR% ...
start "" "%JAVAW%" -jar "%JAR%"
pause
