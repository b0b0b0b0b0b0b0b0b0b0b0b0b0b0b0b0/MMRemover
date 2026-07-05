@echo off
chcp 65001 >nul
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ОШИБКА] Java не найдена.
    echo.
    echo Установите JDK 25 с сайта Adoptium:
    echo   https://adoptium.net/temurin/releases/?version=25
    echo.
    echo При установке поставьте галочку "Set JAVA_HOME" / "Add to PATH".
    echo После установки закройте это окно, откройте заново и запустите start.bat снова.
    echo.
    pause
    exit /b 1
)

set "JAR="
for /f "delims=" %%f in ('dir /b /o-d "MMRemover*.jar" 2^>nul') do (
    set "JAR=%%f"
    goto :run
)
for /f "delims=" %%f in ('dir /b /o-d "target\MMRemover*.jar" 2^>nul') do (
    set "JAR=target\%%f"
    goto :run
)

echo.
echo [ОШИБКА] Файл MMRemover-*.jar не найден в этой папке.
echo Положите jar в ту же папку, где лежит start.bat
echo (обычно MMRemover на рабочем столе — рядом с start.bat).
echo.
pause
exit /b 1

:run
echo Запуск %JAR% ...
start "" javaw -jar "%JAR%"
exit /b 0
