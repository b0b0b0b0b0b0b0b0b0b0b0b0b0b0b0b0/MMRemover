@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"

set "JAVAW="
for /d %%d in (
    "C:\Program Files\Eclipse Adoptium\jdk-25*"
    "C:\Program Files\Java\jdk-25*"
    "C:\Program Files\Microsoft\jdk-25*"
) do (
    if exist "%%d\bin\javaw.exe" (
        set "JAVAW=%%d\bin\javaw.exe"
        goto :java_ok
    )
)

echo.
echo [ОШИБКА] Java 25 не найдена. MMRemover 2.0 без неё не запустится.
echo   https://adoptium.net/temurin/releases/?version=25
echo.
pause
exit /b 1

:java_ok
set "JAR="
for /f "delims=" %%f in ('dir /b /o-d "MMRemover*.jar" 2^>nul') do (
    set "JAR=%%f"
    goto :run
)

echo.
echo [ОШИБКА] MMRemover-*.jar не найден в этой папке.
echo Положите jar рядом со start.bat.
echo.
pause
exit /b 1

:run
echo Start %JAR%...
start "" "%JAVAW%" -jar "%JAR%"
pause
