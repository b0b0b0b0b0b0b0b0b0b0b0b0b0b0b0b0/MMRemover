# MMRemover

**Minecraft Malware Remover** — программа для автоматической очистки вирусов с плагинов Minecraft.

**Домены малвари для бана на хостинге:** актуальный список — [b0b0b0.dev/vb.json](https://b0b0b0.dev/vb.json) (JSON, можно парсить автоматически). Часть доменов уже мертва, но резать на DNS всё равно имеет смысл.

## Удаляет

- Hostflow
- Артёмка
- bStats.jar
- PluginMetrics.jar
- aph
- ChbkHack
- ruBstatsHack (`ru/bstats`, `me/bstats` + `bstats.xyz`)
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

## ru/bstats — малварь `api-bstats.online` (adod_bstats)

**Не путать с настоящим [bStats](https://bstats.org)** (`org.bstats`, метрики на `bstats.org`). Это отдельная семья бэкдоров, маскирующихся под bStats.

### Названия / следы в коде

| Как встречается | Что это |
|-----------------|---------|
| **adod_bstats** / **bstats.jar** | Отдельный «плагин» `name: bStats`, main `ru.bstats.Bootstrap` |
| **`ru/bstats/`** | Вшито в leak-плагины (`Metrics.load()` в `onEnable`) |
| **`me/bstats/`** | Вариант той же семьи (`MetricsConfig` → `bstats.xyz`, `adod_bstats.jar`) |
| **C2** | `http://api-bstats.online`, зеркала через `/domain.list` |
| **Автор (подписи)** | @adodovsky, NULLUDP, файл `крякерыпрочитайте.alexey` |

User-Agent на C2: `Java/forchhh-core`. Ключ шифрования payload: `Make bstats great again!` + AES-GCM.

### Как устроено (два слоя)

**Слой 1 — видимый JAR** (`bstats.jar`, ~900 КБ):

- `plugin.yml`: `name: bStats`, `main: ru.bstats.Bootstrap`
- `data/1` — зашифрованный ZIP с payload (~850 КБ)
- `ru.bstats.Bootstrap` — читает `data/1`, расшифровывает, грузит классы в память (`InMemoryLoader`)
- Куча мусорных классов с рандомными пакетами — шум для анализа

**Слой 2 — payload в памяти** (`ru.bstats.RemoteBootstrap`, `ru.bstats.a`, …):

- Ищет последний **enabled**-плагин через Bukkit API
- Вешается на него как «хост»
- Глушит `System.out` / `System.err` (консоль сервера «тихая»)
- Копирует `крякерыпрочитайте.alexey` в `getDataFolder()` плагина
- Через вшитый **ASM** патчит **другие `.jar` в `plugins/`** на диске (персистентность)
- Поддержка **Bukkit**, **BungeeCord**, **Velocity**

Настоящего `org/bstats/` в этих JAR **нет**.

### Что делает на сервере (не на Windows)

| Действие | Описание |
|----------|----------|
| Регистрация на C2 | POST JSON: `id`, `owner`, `platform`, `ip`, `port`, `is_pc` |
| Опрос команд | `poll` / `update` — ботнет, не «метрики» |
| `cmd` / `shell` | На Windows: `cmd /c …` (только пока крутится `java.exe`) |
| `mc` | Команды консоли Minecraft |
| `download` | Скачать файл на диск сервера по URL |
| `upload` | Отправить **конкретный** файл на C2 (hex), не автоматически |
| Эксфиль | Список **имён** файлов в папке, куски `logs/latest.log` — **не** заливка всей сборки |

**Не** вирус Windows: нет автозагрузки, реестра, служб. Работает только внутри процесса сервера. Удалил папку сервера — всё умерло.

### Декой в коде (можно игнорировать)

В `static {}` и константах — фейковые URL (`api.metrics.example.com`, `127.0.0.1`, китайские «пугалки» про Интерпол), неиспользуемые `@uuid`-строки. Нужны, чтобы отвлечь grep при реверсе. Реальный C2 — только `api-bstats.online` в `ru.bstats.d.a`.

### Вшитый вариант (`ru/bstats`, `me/bstats` внутри leak-плагина)

Отдельно от `bstats.jar`-дроппера: малварь сидит **внутри** чужого JAR.

- В `onEnable` вызывается `Metrics.load()` или `MetricsBase.configure()`
- C2: **`api-bstats.online`**, **`bstats.xyz`**
- `InternalService` разносит заразу по **другим `.jar` в `plugins/`**
- Вшивает **asm.jar + asm-tree.jar** → в ZIP появляются **дубли** (`module-info.class` ×3)

### Как чистит MMRemover (`ruBstatsHack`)

- Дедуп записей в ZIP (чтобы не осталась пустышка из одного ASM ~120 КБ вместо ~700 КБ)
- Вырезка `ru/bstats/*`, `me/bstats/*`, мусорного `org/objectweb/asm/`
- Удаление вызовов `Metrics.load` / `MetricsBase.configure` из главного класса
- **Плагин остаётся целым**

**`bstats.jar`** с `ru.bstats.Bootstrap` и `data/1` — это **отдельный дроппер**, не leak-плагин. Его **удаляют из `plugins/` целиком**; «починить» байткодом в рабочий плагин нельзя.

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
javaw -jar MMRemover-1.22.jar
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
