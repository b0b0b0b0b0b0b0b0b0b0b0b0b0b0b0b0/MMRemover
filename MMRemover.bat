@echo off
cd /d "%~dp0"

set "JAR="
for /f "delims=" %%f in ('dir /b /o-d "MMRemover*.jar" 2^>nul') do (
    set "JAR=%%f"
    goto :run
)

echo MMRemover jar not found.
pause
exit /b 1

:run
echo Start %JAR%...
javaw -jar "%JAR%"
echo.
pause
