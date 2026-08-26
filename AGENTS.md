# AGENTS.md

Этот репозиторий содержит Diasync Android v2: личное Java-приложение для непрерывного мониторинга глюкозы на телефоне и Wear OS.

Перед изменениями прочитай `docs/design.md` и относящийся к задаче slice из `docs/implementation-plan.md`. Не переосмысливай зафиксированное пользовательское поведение без явного запроса.

## Источники истины

При конфликте используй следующий порядок:

1. Текущий явный запрос пользователя.
2. `docs/design.md`.
3. `docs/implementation-plan.md`.
4. Поведение `diasync-old` как референс.
5. Существующий код этого репозитория.

Если нужное решение уже следует из этих источников, не задавай вопрос. Спрашивай только когда выбор существенно меняет пользовательское поведение, безопасность, данные или границы scope.

## Общение и язык

- Отвечай пользователю по-русски, неформально и по делу.
- Код, identifiers, test names, commit-oriented summaries и комментарии в коде пиши по-английски.
- Не добавляй комментарий, если код можно сделать понятным именами и структурой.
- Сначала сообщай результат, затем важные детали и проверки.

## Неподвижные архитектурные решения

- Только Java 17. Не добавляй Kotlin, Kotlin DSL, Compose или `*-ktx` dependency без отдельного явного запроса.
- UI телефона — XML Views; widget — `RemoteViews` и Canvas bitmap.
- Backend доступен только модулю `app`.
- Используется REST bootstrap + long poll. GraphQL/WebSocket не использовать.
- `userId` — credential. Не передавай его в `wear`, не логируй и не показывай в notification/widget.
- `wear` получает bounded snapshot через Wear Data Layer, хранит последний корректный snapshot и предоставляет complication.
- `watchface` — Watch Face Format с `android:hasCode="false"`; исполняемый код туда не добавлять.
- `common` — чистая Java-библиотека без Android SDK.
- Continuous phone sync живёт в `specialUse` foreground service с ongoing notification; не используй `dataSync` FGS для 24/7 loop.
- Sync cursor — server `updateTimestamp`, никогда не measurement timestamp и не локальное время после bootstrap.
- Data batch и cursor сохраняются в одной Room-транзакции.
- Старый проект копируется по поведению и внешнему виду, но не по архитектуре.

## Ожидаемые модули

```text
:common     pure Java models, calculations, protocol, tests
:app        phone UI, Room, REST sync, widget, alerts, Wear sender
:wear       Data Layer receiver, persistence, alerts, complication provider
:watchface  WFF resources only
```

Разрешённые направления зависимостей:

```text
app  -> common
wear -> common
watchface -> none
```

Не создавай зависимости `common -> app/wear`, `wear -> app` или `watchface -> wear` на уровне Gradle/code.

## Стиль архитектуры

- Реализуй один небольшой vertical slice за раз.
- Предпочитай прямой код и явные зависимости слоям ради слоёв.
- Не создавай интерфейс, если существует только одна реализация и тестовая граница не приносит пользы.
- Не добавляй DI framework в первой версии. Используй небольшой composition root и constructor injection.
- Не используй service locator/static mutable singleton для domain state.
- Android components должны быть тонкими: orchestration в service/provider, логика в обычных Java-классах.
- Время передавай через `Clock`; domain logic использует `Instant` и `Duration` в UTC.
- Blocking network, database и bitmap rendering никогда не выполнять на main thread.
- Публичные DTO между телефоном и часами versioned и имеют contract tests.
- Не передавай Room entities и Retrofit DTO напрямую в widget/watch/domain presentation.

## Данные и синхронизация

- Local identity точки: `(userId, timestamp)`.
- Upsert должен быть idempotent.
- Не продвигай cursor при частичном/неуспешном batch.
- Пустой long-poll response — нормальный timeout.
- При ошибке оставляй последнее корректное пользовательское состояние и повторяй с bounded exponential backoff+jitter.
- Активный HTTP call должен отменяться при остановке service.
- Snapshot на Wear содержит только ограниченное окно данных и не содержит backend URL/userId.
- Повреждённый или неподдерживаемый Wear payload не заменяет последнее корректное состояние.

## Поведенческие инварианты

Сохраняй правила из `docs/design.md`, особенно:

- default unit mmol/L; thresholds 70/180 mg/dL;
- trend основан на latest против среднего предыдущих точек за 10 минут;
- widget windows 30m/1h/3h, default 30m;
- visual stale: widget после 1m, strike-through после 10m, watchface после 90s;
- NO DATA alarm после 5m;
- LOW требует `latest <= low` и ухудшение; HIGH требует `latest >= high` и ухудшение;
- alert priority LOW > HIGH > NO DATA;
- global silence interval 55s и persistent snooze;
- Wear alert event дедуплицируется и имеет expiry.

Если старый код содержит очевидную платформенную ошибку, race или утечку lifecycle, сохрани намеренное пользовательское поведение, а механизм исправь. Добавь characterization test, который показывает сохранённое поведение.

## Dependencies

- Управляй версиями через `gradle/libs.versions.toml`.
- Используй Gradle wrapper.
- Перед новой dependency объясни, какую конкретную сложность она убирает.
- Предпочтительные базовые средства: Room, OkHttp/Retrofit, Gson, AndroidX AppWidget/Wear APIs и существующий JUnit 4 test stack. Не добавляй отдельную JUnit 5 integration без конкретной необходимости.
- Не добавляй RxJava/Reactor/coroutines/event bus ради одного loop.
- Не обновляй AGP/Gradle/targetSdk одновременно с feature slice без необходимости.

## Рабочий процесс

Перед изменениями:

1. Прочитай `docs/design.md`, этот файл и нужный slice плана.
2. Осмотри текущий diff/status и не трогай пользовательские несвязанные изменения.
3. Найди существующие тесты и ближайший рабочий pattern.
4. Сформулируй краткий plan и критерий готовности.

Во время работы:

- держи scope в границах одного slice;
- делай минимальный связный diff;
- сначала добавляй/уточняй test для чистой логики;
- проверяй ошибки, empty/stale/restart states, а не только happy path;
- не делай широких rename/refactor «заодно»;
- не редактируй backend или `diasync-old`: они read-only references;
- не коммить, не push и не создавай PR без явного запроса.

После работы:

1. Выполни релевантные проверки.
2. Просмотри собственный diff на секреты, случайный Kotlin, debug code и scope creep.
3. Отметь slice выполненным только если выполнены все acceptance criteria.
4. Если реализация изменила архитектурное решение, обнови `docs/design.md` в том же change.
5. В финальном ответе перечисли: результат, важные файлы, выполненные проверки, что не удалось проверить.

## Проверки

Минимум для общей логики:

```bash
./gradlew :common:test
```

Для phone change:

```bash
./gradlew :common:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Для Wear/WFF change:

```bash
./gradlew :common:test :wear:testDebugUnitTest :wear:lintDebug :wear:assembleDebug :watchface:lintDebug :watchface:assembleDebug
```

Перед milestone/release:

```bash
./gradlew test lint assembleDebug
```

Если команда не существует из-за фактической конфигурации Gradle, сначала посмотри доступные tasks и используй ближайшую эквивалентную проверку. Не заявляй, что device behavior проверено, если был только JVM build.

## Device validation

Для изменений widget, foreground lifecycle, Wear Data Layer, complication, vibration или WFF одних unit tests недостаточно.

- Используй подключённое устройство/emulator только когда оно доступно.
- Не очищай app data и не переустанавливай APK с потерей данных без явного согласия.
- Для time-based behavior можно сокращать интервалы только через debug-only injection/configuration, не меняя production constants.
- Записывай точный manual scenario и фактический результат.

## Definition of done для slice

Slice завершён, когда:

- пользовательский outcome реально достижим;
- acceptance criteria из плана выполнены;
- happy path и главные failure states покрыты тестами или явно выполненным device check;
- affected modules собираются;
- нет нового известного silent data-loss path;
- документация соответствует реализации;
- final report честно отделяет автоматические проверки от ручных.
