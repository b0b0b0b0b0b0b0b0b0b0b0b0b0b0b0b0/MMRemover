# MMRemover

**Minecraft Malware Remover** — программа для автоматической очистки вирусов с плагинов Minecraft.

## Удаляет

- Hostflow
- Артёмка
- bStats.jar
- PluginMetrics.jar
- aph
- ChbkHack

## Требования

Java **17+**

## Использование

1. Запустите программу — при первом запуске выберите язык.
2. Закиньте заражённые плагины в окно или в папку `input`.
3. Нажмите **НАЧАТЬ ОЧИСТКУ**.
4. Заберите очищенные плагины из папки `out` или из правого окна.

Консоль показывает ход очистки. Кнопки под ней открывают/очищают `input`, `out` и консоль.

## Запуск

```bat
javaw -jar MMRemover-1.18.jar
```

Или батник рядом с jar:

```bat
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
```

Сборка: `mvn package` → jar в `target/`.
