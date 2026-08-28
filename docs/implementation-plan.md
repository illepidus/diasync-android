# Diasync Android v2 — план реализации

Статус: готов к выполнению  
Основание: `docs/design.md`  

## Как пользоваться планом

- Один slice — одна отдельная задача Codex и один reviewable diff.
- Новый slice начинается только после выполнения acceptance criteria предыдущего.
- Внутри slice допустимы модели, persistence, UI и тесты одновременно: это vertical slice, а не слой.
- Не просить Codex «реализовать весь план» одним запуском.
- После каждого slice запускать указанные проверки и просматривать diff.
- Статус `[x]` ставится только после проверок; частично готовый slice остаётся `[ ]` с короткой заметкой.

## Карта требований

| Требование | Slice |
|---|---|
| FR-1 Settings/status | 1, 5, 11 |
| FR-2 REST bootstrap | 1 |
| FR-3 Continuous long poll | 3 |
| FR-4 Atomic persistence/cursor | 1, 3 |
| FR-5 Phone widget | 2, 4 |
| FR-6 Phone alerts | 5 |
| FR-7 Wear snapshot | 6 |
| FR-8 WFF | 7, 8 |
| FR-9 Watch alerts | 9 |
| FR-10 Recovery | 10 |
| NFR-1 End-to-end latency | 3, 6, 10 |
| NFR-2 No cursor/data loss | 1, 3, 10 |
| NFR-3 No blocking main thread | каждый slice |
| NFR-4 Credential isolation | 1, 6, 11 |
| NFR-5 Deterministic time tests | 1, 2, 5, 6, 9, 10 |

---

## Slice 0 — собираемый фундамент

Статус: `[x]`

### Outcome

Проект имеет согласованную структуру из четырёх модулей, собирается на Java 17 и содержит testable pure-Java основу без изменения пользовательского поведения.

### Scope

- Добавить `:common` как `java-library`.
- Подключить `app -> common` и `wear -> common`.
- Перевести `wear` на Java 17.
- Оставить `watchface` code-free.
- Проверить стартовые dependencies и удалить только действительно неиспользуемые; Kotlin-specific/`*-ktx` dependencies не добавлять.
- Удалить `play-services-wearable` из `watchface`, где `hasCode=false`.
- Добавить test dependencies и базовые package roots.
- Добавить минимальные domain value objects в `common` только если они сразу нужны для теста структуры; не строить все будущие слои заранее.
- Переименовать `ru.krotarnya.diasync.watchface -> ru.krotarnya.diasync2.watchface`

### Предполагаемые файлы

```text
settings.gradle
build.gradle
gradle/libs.versions.toml
common/build.gradle
common/src/main/java/ru/krotarnya/diasync2/common/...
common/src/test/java/ru/krotarnya/diasync2/common/...
app/build.gradle
wear/build.gradle
watchface/build.gradle
```

### Acceptance criteria

- `settings.gradle` включает четыре модуля.
- `common` не зависит от Android SDK.
- Ни в одном source set нет Kotlin.
- `app` и `wear` компилируются с Java 17.
- WFF APK остаётся `android:hasCode="false"`.
- WFF APK не содержит DEX или runtime dependencies.
- Все debug APK и common tests собираются.

### Проверки

```bash
./gradlew :common:test :app:assembleDebug :wear:assembleDebug :watchface:assembleDebug
```

### Не входит

- Room, REST, widget, alert UI, Data Layer implementation.

---

## Slice 1 — конфигурация → REST bootstrap → локальное состояние → status screen

Статус: `[x]`

### Outcome

Пользователь вводит backend URL и userId, нажимает Start/Refresh и видит на экране последнее полученное значение или понятное empty/error состояние. Точки переживают перезапуск приложения.

### Scope

#### Common

- `GlucoseValue`, `GlucoseUnit`, `Calibration`, `SensorPoint`/`DataPoint` presentation models.
- Конвертация mg/dL/mmol/L.
- Calibration function.
- `Clock`-based age calculation.

#### App data

- API DTO, соответствующий backend `DataPoint`.
- Минимальный REST client для `getDataPoints`.
- Room entity/DAO/database для `data_points` и `sync_state`.
- Mapper API DTO → local entity → domain presentation.
- Atomic bootstrap transaction.
- Настройки URL/userId в app-private preferences; userId не логировать.

#### UI

- XML Activity/fragment со следующими состояниями:
  - configuration missing;
  - loading;
  - latest value;
  - no data;
  - connection/HTTP/parse error.
- Показывать unit, value, timestamp/age и кнопку повторить.

### Bootstrap details

- Запрашивать явное bounded окно, не полагаться на backend default hour.
- Сохранять все DataPoint types, даже если UI показывает только sensor glucose.
- Upsert по `(userId, timestamp)`.
- Не использовать server `id` как локальную identity.
- Bootstrap не запускает постоянный service: это пока один user-visible request.

### Acceptance criteria

- После успешного request последнее sensor value видно в Activity.
- Empty response показывает `NO DATA`, а не exception.
- Повтор того же response не создаёт duplicate rows.
- Изменённая server point с тем же `(userId,timestamp)` обновляет локальную row.
- После recreating Activity/process данные читаются из Room до следующей сети.
- userId не появляются в обычных logs; userId не показывается полностью в status UI.
- Network и DB выполняются вне main thread.

### Tests

- Unit conversion/calibration.
- DTO parsing с sensor/manual/carbs/calibration.
- Room insert/update/idempotency.
- Bootstrap success/empty/error.
- UI presenter/view-model-like state transitions как обычный Java-класс без Android dependency.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Manual: выполнить bootstrap против `demo` и сверить отображение.

### Не входит

- Continuous service, widget, alerts, Wear.

---

## Slice 2 — latest-value home-screen widget

Статус: `[x]`

### Outcome

Пользователь добавляет widget и видит последнее локально сохранённое значение, trend placeholder/вычисленный trend и корректное fresh/stale/error состояние без открытия Activity.

### Scope

- `AppWidgetProvider`, manifest receiver и appwidget metadata.
- XML `RemoteViews` layout.
- Presentation builder, который читает Room вне main thread.
- Value/unit/color/trend/age states.
- Click открывает Diasync status/settings Activity.
- Update после bootstrap и resize.
- Minute tick/update orchestration для возраста при интерактивном экране без копирования старого sticky `WidgetUpdateService` буквально.

### Acceptance criteria

- Widget добавляется и переживает launcher restart.
- No data: `----`, скрытая trend arrow, `NO DATA`.
- Fresh: message пустой.
- После двух минут показывается age в формате `Nm ago`.
- После 10 минут значение перечёркнуто.
- Более чем +1 minute future timestamp показывает `DATA FROM FAR FUTURE`.
- Resize не вызывает crash, сохраняет читаемую компоновку и заполняет графиком всю площадь widget,
  включая сверхширокие размеры.
- После Slice 1 bootstrap widget обновляется без ручного удаления/добавления.

### Tests

- Widget presentation state table с injected `Clock`.
- Range color boundaries.
- Intent/update routing.
- Robolectric/instrumented test там, где pure unit test недостаточен.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Manual: add widget, resize, change unit, проверить fresh/2m/10m/future states через debug clock/data fixture.

### Не входит

- Полный bitmap graph.

---

## Slice 3 — непрерывный long poll foreground service

Статус: `[x]`

### Outcome

После включения monitoring телефон непрерывно получает server updates, обновляет Room, Activity и widget, а ongoing notification честно показывает состояние соединения.

### Scope

- `specialUse` foreground service и permissions/property.
- Ongoing notification channel и notification builder.
- Long-poll API call с `timeoutMs=75000`, client timeout > 75s.
- Dedicated single-thread executor/cancellable HTTP call.
- Bootstrap-to-cursor transition с overlap.
- Room transaction batch+cursor.
- Loop: empty success → immediate next poll; error → backoff+jitter.
- Start/stop из видимой Activity.
- `START_STICKY` process recovery.
- Coordinator после commit: Activity state, widget, notification; Wear hook пока no-op.

### Cursor invariants

- После первого server cursor использовать только `updateTimestamp`.
- Максимальный cursor вычислять по всему batch.
- Missing/invalid `updateTimestamp` не продвигает cursor.
- Commit failure оставляет прежний cursor.
- Повтор batch безопасен.

### Acceptance criteria

- Monitoring запускается из UI и быстро переходит в foreground.
- Notification видна всё время работы.
- Пустой response не отображается как ошибка.
- Новая server point появляется в Room/widget в пределах NFR-1 при нормальной сети.
- Airplane mode приводит к connecting/backoff без потери локального state.
- Возврат сети восстанавливает loop без открытия Activity.
- Force-stop исключается из обещаний Android; обычный process kill восстанавливается.
- Stop отменяет активный call и не оставляет executor/thread leak.
- `dataSync` FGS type не используется.

### Tests

- Fake/MockWebServer сценарии: immediate data, timeout empty, HTTP error, disconnect, malformed JSON.
- Crash boundary simulation: точки записаны/cursor нет; cursor не может обогнать points.
- Backoff reset/jitter bounds.
- Service controller state transitions.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Manual: 10+ минут long polling, airplane mode, process kill, notification actions.

### Не входит

- Boot receiver и полный stress/recovery pass — Slice 10.

---

## Slice 4 — widget с графиком и parity со старой версией

Статус: `[x]`

### Outcome

Widget визуально и поведенчески соответствует основной части старого `Libre2Widget`: точки графика, зоны/линии, value, trend и stale/error состояния.

### Scope

- Pure graph layout calculation где возможно.
- Android Canvas bitmap renderer.
- Windows 30m/1h/3h, default 30m.
- Auto Y-scale, включающий low/high и margin.
- Colored points.
- Zones default on, lines default off.
- Настройки unit/threshold/window/zones/lines.
- Перерисовка после data/settings/resize/home-screen orientation/minute age update.

### Acceptance criteria

- График не обрезается на минимальном и большом widget sizes.
- Low/normal/high points и zones соответствуют настройкам.
- Empty/future state очищает bitmap.
- Смена window/unit/threshold немедленно обновляет widget.
- Рендер не выполняется на main thread и не создаёт unbounded bitmap.
- Визуальное сравнение со старым widget не выявляет пропавших обязательных элементов.

### Tests

- Coordinate mapping и degenerate ranges.
- Auto-scale при одинаковых значениях.
- Bounds для точек вне окна.
- Golden/screenshot-like bitmap checks для нескольких фиксированных datasets, если test stack позволяет устойчиво сравнивать результат.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Manual: screenshot matrix размеров × windows × normal/low/high/stale.

---

## Slice 5 — phone alert engine и настройки snooze

Статус: `[x]`

### Outcome

Телефон воспроизводит LOW/HIGH/NO DATA так же, как старая версия, включая ухудшение, priority, минутные повторы и persistent global snooze.

### Scope

#### Common

- `AlertEvaluator` с `Clock`.
- `AlertType`, `AlertDecision`, policy/settings.
- 55-second global throttle.
- LOW/HIGH worsening rules.
- NO DATA после 5 минут.
- Priority LOW > HIGH > NO DATA.

#### App

- Alert settings XML.
- Enabled switches default false.
- Snooze options 5m…24h и Resume.
- Persistent `snoozedUntil`.
- Bundled low/high/no-data sounds.
- Playback с `USAGE_ALARM` и корректным release lifecycle.
- Alert notification/channel с переходом в settings.
- Проверка после data commit и minute schedule.
- Подготовить output hook для Wear event; фактическая отправка в Slice 6.

### Acceptance criteria

- LOW срабатывает только ниже/equal low и при строгом падении.
- HIGH — только выше/equal high и при строгом росте.
- NO DATA — через 5 минут или при полном отсутствии точки.
- За один check воспроизводится только приоритетный alert.
- Повтор раньше 55 секунд подавляется.
- Snooze переживает process restart и подавляет все alerts.
- Resume немедленно снимает snooze.
- Все alerts выключены по умолчанию.
- Media resource освобождается после playback/error.

### Tests

- Полная boundary/state table из старого `Alerter`.
- Repeat/throttle с fake clock.
- Snooze/Resume/persistence.
- Priority и no-data recovery после свежей точки.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Manual: проиграть три звука, проверить snooze и notification channel/DND behavior.

---

## Slice 6 — persistent phone → Wear state path

Статус: `[ ]`

### Outcome

После новых данных телефон отправляет bounded snapshot, часы получают его, сохраняют и показывают диагностическое последнее значение даже после reconnect/restart. Backend credential на часы не попадает.

### Scope

#### Common protocol

- Versioned `WearSnapshot` DTO.
- JSON serialization/deserialization contract tests.
- `protocolVersion`, `generatedAt`, bounded points, display/graph/alert policy, optional alert event.
- Payload validation и size budget.

#### App sender

- Snapshot builder из local domain data.
- Data Item path `/diasync/v1/state`.
- Urgent update после commit/settings change.
- Connection/status diagnostics.
- Не использовать MessageClient для durable state.

#### Wear receiver

- Data Layer listener service.
- Version/shape validation.
- Atomic persistence последнего корректного payload.
- Last-known state repository.
- Простая диагностическая Activity или debug log/state test surface; production launcher UI не обязателен.

### Acceptance criteria

- Snapshot содержит не больше максимального graph window + margin.
- Snapshot не содержит userId, backend URL или credential-derived string.
- Повтор snapshot idempotent.
- Unsupported/corrupt payload не удаляет последнее корректное состояние.
- После disconnect → update phone → reconnect часы получают latest state.
- После kill/restart Wear process last state читается из storage.
- Protocol round trip сохраняет timestamps/thresholds/trend.

### Tests

- Serialization golden JSON.
- Old/new protocol version rejection.
- Credential absence assertion.
- Event expiry/dedupe primitives.
- Persistence replace-on-valid-only.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :wear:testDebugUnitTest :app:assembleDebug :wear:assembleDebug
```

Manual на реальных часах: paired, disconnected/reconnected, Wear process killed.

### Не входит

- Финальный WFF и vibration playback.

---

## Slice 7 — минимальный complication + WFF end-to-end

Статус: `[ ]`

### Outcome

На реальных часах WFF показывает время/дату и диабетическую complication с последним value/trend или `NO DATA`, обновляющуюся после phone long poll.

### Scope

- Wear complication data source service и manifest metadata.
- Request update после нового persisted snapshot.
- Минимальная complication representation.
- WFF slot с собственным provider по умолчанию.
- Time `HH:mm`, date `EE dd.MM`.
- Interactive/ambient variants.
- Preview и watch-face metadata.

### Acceptance criteria

- Оба APK устанавливаются через adb/Android Studio.
- Watchface появляется в picker.
- Provider доступен и выбран/назначен.
- Phone server update отражается на watchface в пределах NFR-1.
- После отсутствия snapshot показывается `NO DATA`, без crash/пустого невидимого slot.
- Ambient не показывает секунды и остаётся читаемым.

### Tests

- Complication state mapping unit tests.
- WFF XML/build validation.
- Provider preview data.

### Проверки

```bash
./gradlew :common:test :wear:testDebugUnitTest :wear:lintDebug :wear:assembleDebug :watchface:lintDebug :watchface:assembleDebug
```

Manual обязательно: Watch 7 complication/WFF/ambient.

---

## Slice 8 — полный watchface visual parity

Статус: `[ ]`

### Outcome

Новый WFF воспроизводит обязательный внешний вид старого watchface: время, дата, батарея, graph, value, trend, stale/errors.

### Scope

- Wear Canvas bitmap renderer для diabetic `PHOTO_IMAGE` complication.
- Graph windows 30m/1h/3h.
- Threshold lines, colored points, current glucose overlay, trend.
- Visual stale после 90s и age minutes.
- System battery complication: percent, charging green, <=15% red, otherwise white.
- WFF layout под круглые 450×450 design coordinates с безопасным scaling.
- Ambient simplification/low-bit/burn-in conscious palette.
- Settings change на телефоне обновляет snapshot и face.

### Acceptance criteria

- Все обязательные элементы старого renderer присутствуют.
- Current value остаётся читаемым поверх graph.
- No data/stale/error видны на interactive и ambient.
- Graph не выходит за intended bounds на Watch 7 и Watch 4.
- Battery state корректно меняет цвет.
- Face не обновляется чаще, чем нужно; новые данные и minute-age достаточно.

### Tests

- Renderer coordinate/bounds tests.
- Fixed datasets: empty, normal, low falling, high rising, stale, future.
- Bitmap snapshots при стабильном test renderer, либо зафиксированная manual screenshot matrix.

### Проверки

```bash
./gradlew :common:test :wear:testDebugUnitTest :wear:lintDebug :wear:assembleDebug :watchface:lintDebug :watchface:assembleDebug
```

Manual: Watch 7 + обновлённый Watch 4, interactive + AOD screenshots.

---

## Slice 9 — Wear LOW/HIGH и локальный NO DATA

Статус: `[ ]`

### Outcome

Часы надёжно вибрируют на fresh LOW/HIGH event от телефона и самостоятельно обнаруживают NO DATA через 5 минут, не дублируя событие после restart/reconnect.

### Scope

- Phone включает alert event/policy/snoozedUntil в snapshot.
- Stable `eventId`, generated/expiry timestamps.
- Wear event dedupe persistence.
- LOW/HIGH vibration patterns.
- Local `Clock`-based NO DATA state machine.
- Minute-level scheduling, учитывающее process lifecycle.
- Fresh point сбрасывает NO DATA.
- Snooze/enabled policy применяется без backend connection.
- Complication invalidation при переходе fresh→stale→no-data.

### Acceptance criteria

- Один event вибрирует не более одного раза, включая restart.
- Просроченный event после reconnect не вибрирует.
- LOW pattern: 800/400/800ms.
- HIGH pattern: 1000ms.
- NO DATA определяется локально спустя 5 минут даже при disconnect телефона.
- Snooze подавляет vibration до срока.
- Новая fresh point прекращает NO DATA repeats и обновляет face.
- Проверки времени не зависят от real sleep в unit tests.

### Tests

- Dedupe/restart/expiry.
- 90s visual stale против 5m alarm state.
- Snooze crossing and clock jumps.
- Fresh recovery.
- Vibration command mapping.

### Проверки

```bash
./gradlew :common:test :app:testDebugUnitTest :wear:testDebugUnitTest :app:assembleDebug :wear:assembleDebug :watchface:assembleDebug
```

Manual обязательно: реальные vibration patterns и disconnect phone scenario.

---

## Slice 10 — reboot, reconnect и failure hardening

Статус: `[ ]`

### Outcome

Система восстанавливается после реальных сбоев без ручной очистки данных и без тихой потери cursor/state.

### Scope

- Phone boot/package-replaced receiver.
- Восстановление enabled monitoring с учётом background FGS restrictions.
- Reconciliation bootstrap при invalid cursor/state.
- Network transition handling без busy loop.
- Database migration baseline и destructive migration запрет.
- Wear direct-boot-aware storage/provider, где это поддерживается и нужно.
- Clock anomaly handling.
- Notification/status diagnostics для blocked/retry states.
- Debug-only сокращение timeout для device scenarios.

### Failure matrix

- kill phone process during HTTP;
- kill после response до Room transaction;
- kill внутри/после transaction;
- reboot phone;
- network off дольше server timeout;
- backend 500/malformed JSON;
- смена userId/URL;
- disconnect/reconnect watch;
- reboot watch;
- unsupported Wear payload;
- local clock jump вперед/назад;
- timestamp from future.

### Acceptance criteria

- Ни один scenario не продвигает cursor без соответствующих local points.
- Monitoring восстанавливается автоматически, когда Android разрешает запуск.
- Если автоматический background start запрещён, пользователь видит actionable state.
- Long disconnect не вызывает unbounded retries/battery loop.
- Watch сохраняет last-known face и показывает stale/no-data.
- Смена credential не смешивает данные разных userId.

### Tests

- Transaction crash boundaries.
- Restart controller.
- Backoff long-run bounds.
- DB migrations.
- Wear persisted protocol upgrade/reject.

### Проверки

```bash
./gradlew test lint assembleDebug
```

Manual: пройти failure matrix на телефоне и Watch 7; критичные пункты повторить на Watch 4.

---

## Slice 11 — UX polish, install flow и первый личный release

Статус: `[ ]`

### Outcome

Владелец может собрать, установить, настроить и обновить phone/Wear/WFF без знания внутренней структуры Gradle.

### Scope

- Финальный settings/status UI и validation.
- App icons/names/previews.
- Release build configuration без premature obfuscation, если она мешает диагностике.
- Документ `INSTALL.md` с точными adb-командами и device selection.
- Опциональный безопасный helper script для сборки/установки трёх APK, если он реально сокращает рутину.
- Версионирование phone/wear согласовано; watchface отдельно.
- Проверка backup/restore поведения.
- Redaction pass logs/notifications/errors.
- Полный test/lint/build и manual acceptance checklist.

### Acceptance criteria

- Fresh checkout собирается wrapper-командой.
- INSTALL.md позволяет установить компоненты на правильные устройства без угадывания APK paths.
- Update поверх предыдущей версии сохраняет settings/Room/Wear snapshot.
- UserId отсутствует в logs, widget, Wear payload и notification.
- Widget и watchface работают после release APK install.
- Все FR/NFR из `design.md` имеют проверку или честно зафиксированное исключение.

### Проверки

```bash
./gradlew clean test lint assembleDebug assembleRelease
```

Manual: clean install, configure, update install, reboot, widget, Watch 7, Watch 4, alert sounds/vibration, ambient.

---

## После первого release

Следующие идеи не должны попадать в ранние slices «заодно»:

- producer/master upload path;
- visual markers для carbs/manual glucose;
- watch Tile;
- alert acknowledgement с часов;
- несколько пользователей;
- direct backend bypass на LTE-часы;
- diagnostics export;
- PIP;
- Play distribution;
- compression/binary Wear protocol;
- framework DI.

Каждая такая возможность начинается с изменения `design.md` и отдельного плана, а не с расширения текущего slice.

## Рекомендуемый prompt для запуска slice

```text
Реализуй Slice N из implementation-plan.md целиком.

Перед изменениями прочитай AGENTS.md, design.md и сам slice, затем осмотри текущий код и незакоммиченные изменения. Не выходи за scope slice. Сохрани существующее пользовательское поведение и добавь проверки для failure states, указанных в acceptance criteria.

Готово означает: все acceptance criteria выполнены, релевантные тесты/lint/build запущены, diff просмотрен. Если device check невозможен, не симулируй его успешность — перечисли точный ручной сценарий, который остаётся выполнить.
```
