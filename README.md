# MMRemover

**Minecraft Malware Remover** — программа для автоматической очистки вирусов с плагинов Minecraft.

## Удаляет

- Hostflow
- Артёмка
- bStats.jar
- PluginMetrics.jar
- aph
- ChbkHack
- ruBstatsHack
- **SystemMetrics** (`panel.bstats.co`)

## SystemMetrics — чаморы и `panel.bstats.co`

В JAR вшивают фейковый «bStats»: метод `SystemMetrics` качает payload с **`panel.bstats.co`** (это **не** настоящий bStats) и дергает `onEnableInj`. MMRemover это находит и вырезает.

Но есть нюанс, о котором стоит знать заранее.

Вирус **SystemMetrics** (`panel.bstats.co`) встраивается в главный класс плагина — например, `DecentHologramsPlugin.onEnable()` вызывает `this.SystemMetrics(this)`, метод качает инжект с **`panel.bstats.co`** и дергает `onEnableInj`. **ЧАМОРЫ**, которые пихают именно эту малварь в leak-сборки, **не смогли в ремапинге Paper** — и **уничтожили плагин изначально**, ещё до любой очистки: в тот же JAR, куда воткнули `SystemMetrics`, они положили **165 пустых NMS-классов** (`.class` на 0 байт) под `v1_20_R4`, `v1_21_R*` и `paper_v1_21_*`. Paper 1.21.x при старте падает на ремапе (`ClickableHologramRenderer` → `ArrayIndexOutOfBoundsException: length 0`) — **DecentHolograms с этим вирусом не запустится ни с малварью, ни после вырезки**, пока NMS не подставить из другого целого JAR.

Итого:

| Что | MMRemover |
|-----|-----------|
| Бэкдор `SystemMetrics` / `panel.bstats.co` | вырезает |
| Пустые NMS-классы (порча сборки чаморами) | не чинит — байтов нет |

Домен **`panel.bstats.co`** имеет смысл резать на хостинге/DNS — пока чаморы не разнесли ещё плагины.

## ru/bstats — `api-bstats.online`

Фейковый bStats в пакете `ru/bstats`: `Metrics.load()` в `onEnable`, телеметрия на **`api-bstats.online`**, `InternalService` разносит заразу по другим JAR в `plugins/`.

Типичная ловушка при очистке: малварь вшивает **asm.jar + asm-tree.jar** внутрь плагина → в ZIP **дубли** (`module-info.class` ×3). Старый `ruBstatsHack` падал на 44-й записи и оставлял **пустышку из одного ASM** (~120 КБ вместо ~700 КБ).

Сейчас: дедуп ZIP, вырезка `ru/bstats/*`, мусорного `org/objectweb/asm/`, `Metrics.load` из главного класса — **плагин остаётся целым**.

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
javaw -jar MMRemover-1.20.jar
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
