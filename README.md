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
javaw -jar MMRemover-1.15.jar
```

Или батник рядом с jar:

```bat
@echo off
set JAR_NAME=MMRemover-1.15

for %%f in (%JAR_NAME%.jar) do (
    echo Start %%f...
    javaw -jar "%%f"
    echo.
)
pause
```

Сборка: `mvn package` → jar в `target/`.
