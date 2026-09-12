# Master mode — план реализации

Статус: approved design
Дата исследования: 2026-09-10; Libre raw path уточнён после device validation 2026-09-12

## Утверждённые продуктовые решения

- Diasync и xDrip fork подписываются одним release certificate. Exported receiver защищается
  custom permission с `protectionLevel="signature"`.
- Mode, backend URL и `userId` разрешено менять только при остановленном monitoring и отсутствии
  недоставленных master events. При наличии `PENDING`, `IN_FLIGHT` или `BLOCKED` rows пользователь
  сначала доставляет их либо выполняет отдельный destructive discard с явным подтверждением.
- Sensor event для Libre содержит каждую успешно сохранённую минутную `Libre2RawValue` и snapshot
  текущей calibration `slope`/`intercept`, взятый в том же processing step. Пятиминутный
  `BgReading`, `calculated_value`, smoothing и график xDrip не являются частью контракта. Если
  calibration отсутствует, xDrip явно отправляет identity calibration `slope=1`, `intercept=0`.
- Calibration относится только к sensor glucose. Она бессмысленна и запрещена для
  `MANUAL_GLUCOSE` и `CARBS`.
- Protocol v1 передаёт только create/insert events. Edits и deletes остаются отдельным решением
  Slice 7.

## Цель

Добавить телефону два принципиально взаимоисключающих режима работы:

- `SLAVE` — текущее поведение: bootstrap и long poll из Diasync backend;
- `MASTER` — принимать локальные события из fork-а xDrip и гарантированно доставлять уже принятые
  события в Diasync backend.

Путь данных в master mode:

```text
Libre CGM ─> xDrip LibreReceiver ─> explicit Intent ─> Diasync phone DB
                                                        │
                                                        ├─> local UI/widget/alerts/Wear
                                                        └─> durable outbox ─> Diasync backend
```

Master mode не выполняет bootstrap или long poll. Slave mode не принимает xDrip events и не
отправляет их в backend.

## Что исследовано

### Diasync Android

- `MonitoringService` и `SyncRunner` сейчас реализуют один непрерывный slave loop.
- `data_points` имеет identity `(user_id, timestamp)`, а `sync_state` хранит server cursor.
- После успешной Room-транзакции `PhoneUpdateCoordinator` обновляет presentation, alerts, widget,
  Wear snapshot и foreground notification.
- Текущий Room `@Upsert` целиком заменяет запись. Для двух master events с одинаковым timestamp это
  может стереть ранее сохранённые nullable-поля; для master ingest нужен merge non-null fields.
- Manifest уже содержит package query для `com.eveningoutpost.dexdrip`, но receiver-а и master
  protocol пока нет.

### Diasync backend

Текущий `POST /api/v1/addDataPoints` подходит для MVP доставки:

- ответ возвращается после сохранения batch;
- server отбрасывает присланные `id` и `updateTimestamp`;
- повтор одного `(userId, timestamp)` idempotent;
- новые non-null sensor/manual/carbs поля объединяются с существующей точкой;
- повтор запроса после потери HTTP response безопасен.

Следовательно, MVP может дать at-least-once delivery без изменения backend. Exactly-once transport
не нужен: его эффект достигается повторной отправкой и server upsert.

### xDrip upstream и существующий fork

- На дату исследования и повторной проверки 2026-09-10 upstream
  `NightscoutFoundation/xDrip` master: commit
  `e1407ec9a8ab20bc3f56315daf8cfc8913e12735` от 2026-09-09. Это исходный SHA нового fork-а; он
  фиксируется в PR/release notes.
- Существующий `illepidus/xDrip` заканчивается commit
  `e715a3a6bd2eb003e0955ee1ec5ecf82be145c73` от 2024-09-29 и существенно отстал.
- Старый patch меняет `LibreReceiver`, `BgReading` и `Libre2RawValue`, отправляет action
  `com.eveningoutpost.dexdrip.diasync.libre2_bg` в старый package `ru.krotarnya.diasync`.
- Старый patch сначала берёт raw Libre event, затем отдельно ищет последний `BgReading`. Значения,
  calibration и timestamps могут относиться к разным readings. Этот контракт не переносим.
- В актуальном xDrip каждый принятый Libre result сохраняется как `Libre2RawValue`, а отдельный
  `BgReading` создаётся из сглаженного окна примерно раз в пять минут. Для Diasync sensor path
  источником является `Libre2RawValue`; ручной замер представлен `BloodTest`, carbs — `Treatments`.
- В xDrip уже есть общие broadcast-механизмы, но они настраиваемые, glucose-oriented и не дают
  надёжного отдельного события для каждого нового `BloodTest`/`Treatments`. Для требуемого контракта
  нужен небольшой dedicated patch fork-а.

Полезные upstream paths:

- `app/src/main/java/com/eveningoutpost/dexdrip/LibreReceiver.java`;
- `app/src/main/java/com/eveningoutpost/dexdrip/models/BgReading.java`;
- `app/src/main/java/com/eveningoutpost/dexdrip/models/BloodTest.java`;
- `app/src/main/java/com/eveningoutpost/dexdrip/models/Treatments.java`.

## Предлагаемая архитектура

### 1. Mode как часть сохранённой конфигурации

Добавить `AppMode { SLAVE, MASTER }` в phone `app` и сохранять его в `AppPreferences`.

- Default для существующих установок — `SLAVE`, поэтому migration не меняет текущее поведение.
- Выбор mode доступен в connectivity settings.
- Mode разрешено менять только при остановленном monitoring.
- Один `MonitoringService` остаётся владельцем ongoing notification и orchestration, но создаёт
  ровно один strategy: `SlaveMonitoringRunner` или `MasterMonitoringRunner`.
- Никаких двух параллельных loops и никакого boolean-набора `isMaster/isSlave`.
- `userId` и backend URL нужны в обоих modes, но только Diasync знает эти credentials. xDrip их не
  получает.

`SLAVE` использует нынешний `SyncRunner` без изменения его алгоритма. `MASTER` держит foreground
service активным, слушает сигнал о новых outbox rows, следит за сетью и запускает их отправку.

### 2. xDrip → Diasync protocol

Использовать explicit broadcast:

```text
action:  ru.krotarnya.diasync2.action.XDRIP_EVENT
package: ru.krotarnya.diasync2
extra:   payload = UTF-8 JSON string
```

Не передавать граф объектов через `Serializable`/`Parcelable`: JSON проще versioning-овать,
валидировать и покрывать одинаковыми fixtures в двух независимых repositories.

Минимальная envelope:

```json
{
  "protocolVersion": 1,
  "eventId": "SENSOR:xdrip-stable-uuid",
  "eventType": "SENSOR|MANUAL_GLUCOSE|CARBS",
  "occurredAtEpochMillis": 1789027200000,
  "sensor": {
    "rawValue": 123.0,
    "sensorId": "libre-sensor-id",
    "calibration": {
      "slope": 1.1,
      "intercept": -2.0
    }
  },
  "manualGlucose": { "mgdl": 121.0 },
  "carbs": { "grams": 20.0, "description": "optional note" }
}
```

Правила контракта:

- ровно один payload subtype соответствует `eventType`;
- `eventId` — глобально уникальный `<eventType>:<stable source id>`, не случайный UUID на каждую
  попытку broadcast. Для Libre sensor source id — SHA-256 от `sensorId`, separator byte `0` и
  decimal timestamp; повтор того же минутного raw event получает тот же id. Для `BloodTest` и
  `Treatments` используется stable xDrip UUID. Namespace по type обязателен, потому что одна операция
  xDrip может использовать общий UUID для `BloodTest` и `Treatments`;
- timestamp берётся из сохранённой xDrip entity, не из времени отправки intent;
- `sensor.rawValue` — исходное значение sensor reading до обработки xDrip;
- sensor calibration всегда присутствует полной парой. При отсутствии calibration source отправляет
  identity transform `slope=1`, `intercept=0`;
- `manualGlucose.mgdl` — уже измеренная glucose в mg/dL и не имеет calibration;
- carbs не имеет calibration;
- invalid/unknown version отклоняется до записи в БД;
- payload не содержит backend URL, `userId`, sync key и других credentials;
- размер одного event мал и не приближается к Binder transaction limit;
- fork отправляет только insert/create events в первом protocol version;
- Diasync принимает event только когда сохранённые mode=`MASTER` и monitoring enabled.

Diasync хранит raw value и calibration отдельно и применяет существующую настройку `use calibration`:

```text
displayMgdl = rawValue * slope + intercept
```

При выключенной calibration отображается `rawValue`. Этот контракт сознательно не пытается
воспроизвести `BgReading.calculated_value`, age adjustment, smoothing или график xDrip. Raw value
берётся из сохранённой `Libre2RawValue`, а calibration snapshot — непосредственно в том же
`LibreReceiver` processing step; брать значения из отдельного latest `BgReading` нельзя.

### Protocol v1 schema и limits

- JSON кодируется UTF-8; maximum payload size — 8192 bytes.
- Неизвестные поля отклоняются, чтобы опечатки и непредусмотренные данные не влияли на durable hash.
- `eventId` — printable ASCII без пробелов по краям, максимум 128 characters; prefix обязан точно
  совпадать с `eventType` и после `:` должен быть непустой stable xDrip id.
- `sensorId` — printable ASCII без пробелов по краям, максимум 128 characters.
- `carbs.description` optional, без control characters и пробелов по краям, максимум 256 Unicode
  code points.
- `occurredAtEpochMillis` — целое число миллисекунд Unix epoch, не меньше нуля.
- Все numeric values конечны. `sensor.rawValue` находится в `(0, 1000000]`, manual glucose —
  `(0, 1000]` mg/dL, carbs — `(0, 1000]` grams, calibration slope — `(0, 1000]`, абсолютное
  значение intercept не больше `1000000`.
- Event содержит ровно один subtype, совпадающий с `eventType`. Sensor требует `rawValue`,
  `sensorId` и полную calibration. Manual требует только `mgdl`. Carbs требует `grams` и optional
  `description`.

Canonical fixtures находятся в `common/src/test/resources/xdrip-event-v1`. Encoder xDrip fork-а
должен точно воспроизводить UTF-8 JSON content valid fixtures без завершающего файлового перевода
строки; Diasync parser должен принимать valid и отклонять invalid fixtures.

### 3. Защита exported receiver

Receiver должен быть manifest-declared, explicit-targeted и `exported=true`. В `onReceive` он:

1. вызывает `goAsync()`;
2. передаёт короткую parse/validate/persist работу на bounded executor;
3. завершает `PendingResult` в `finally`;
4. не выполняет HTTP и тяжёлый presentation на broadcast thread.

Используется custom permission с `protectionLevel="signature"`. xDrip fork и Diasync release
подписываются одним ключом; xDrip объявляет `uses-permission`, а Diasync receiver требует permission.
Это надёжнее, чем shared secret в extras, который можно извлечь из APK/runtime.

### 4. Transactional inbox/outbox

Добавить Room entity `master_events`:

```text
master_events
├── event_id: String primary key
├── payload_hash: String
├── event_type: String
├── occurred_at: Instant
├── destination_fingerprint: String
├── payload_json: String
├── state: PENDING | IN_FLIGHT | DELIVERED | BLOCKED
├── attempt_count: Int
├── next_attempt_at: Instant?
├── lease_until: Instant?
├── received_at: Instant
├── delivered_at: Instant?
└── last_error_code: String?
```

`destination_fingerprint` связывает event с конфигурацией, активной в момент приёма. Secret нельзя
помещать в fingerprint или diagnostics. Сам `userId` уже хранится app-private в `data_points` и
может быть частью app-private upload payload.

После parse/validation event сериализуется в canonical JSON с фиксированным порядком полей.
`payload_hash` — SHA-256 именно этих canonical UTF-8 bytes, а `payload_json` хранит тот же canonical
JSON. Поэтому различия только в whitespace или порядке входных JSON fields не создают ложный
eventId conflict.

Одна Room-транзакция должна:

1. проверить, не был ли `eventId` уже принят;
2. при duplicate с тем же hash ничего не менять;
3. при duplicate с другим hash отклонить event и записать безопасную diagnostic error;
4. merge-нуть non-null поля event в `data_points` по `(userId, timestamp)`;
5. вставить immutable `PENDING` row в `master_events`;
6. commit;
7. только после commit вызвать `PhoneUpdateCoordinator` и разбудить uploader.

Граница обещания «Diasync получил данные» — успешный commit этой транзакции. После него event нельзя
потерять из-за отсутствия сети, process death или reboot.

Delivered rows стоит хранить ограниченное время, например 30 дней, для dedupe. Pending/blocked rows
никогда не удаляются retention-задачей. Это также требует запретить Android auto-backup только для
эфемерных lease fields либо безопасно сбрасывать `IN_FLIGHT` в `PENDING` после restore/startup.

### 5. Guaranteed backend delivery

Один `MasterOutboxDrainer` читает bounded FIFO batch и использует текущий
`POST /api/v1/addDataPoints`.

Алгоритм:

1. В Room-транзакции взять eligible rows и установить короткий `IN_FLIGHT` lease.
2. Преобразовать каждый event в partial backend `DataPoint` с настроенным `userId`.
3. Отправить batch не больше server limit.
4. На HTTP 200 проверить response count и identity/content каждой принятой точки.
5. В Room-транзакции обновить local `server_id`/`updateTimestamp` из response и пометить events
   `DELIVERED`, не стирая соседние local fields.
6. Если request мог дойти, а response потерян, вернуть rows в retry: server upsert делает повтор
   безопасным.
7. Network/timeout/5xx повторять с bounded exponential backoff + jitter.
8. 413 разбивать на меньшие batches.
9. 4xx или несовместимый response не удалять: пометить `BLOCKED`, показать причину без payload и
   дать ручной retry после исправления конфигурации/версии.
10. При stop отменить active HTTP call; lease позднее превращается обратно в eligible retry.

Prompt delivery выполняет master foreground runner. Дополнительную durable wakeup-гарантию после
process death даёт platform `JobScheduler` job с `requiredNetworkType=ANY` и `setPersisted(true)`;
он вызывает тот же drainer. Это не добавляет dependency. Конкуренция FGS и job исключается lease-ом
и process-local mutex. Boot/package-replaced receiver восстанавливает monitoring и повторно
планирует job, если outbox не пуст.

Обычный `Stop monitoring` запрещает принимать новые xDrip events и останавливает prompt foreground
upload, но не отменяет обязательство по уже committed rows: persisted job продолжает редкие retries.
Полностью прекратить их можно только отдельным destructive discard. Mode при этом остаётся `MASTER`,
так что slave long poll одновременно не запускается.

Гарантия формулируется как at-least-once при следующих условиях:

- app data не очищены и приложение не удалено;
- пользователь не force-stop-нул приложение навсегда;
- backend URL/credential снова становятся корректными и backend принимает payload;
- Android когда-либо даёт приложению execution time и сеть;
- pending data не были явно отброшены пользователем.

Более сильную гарантию Android и fire-and-forget broadcast дать не могут.

### 6. Локальное поведение master mode

После durable ingest sensor event сразу проходит существующий `PhoneUpdateCoordinator`; интернет для
widget, phone alerts и Wear не нужен. Manual glucose и carbs сохраняются сразу, хотя их отображение
может оставаться за scope текущего UI.

Mode-aware status/notification показывают:

- `SLAVE`: нынешние disabled/connecting/connected/retrying и last response;
- `MASTER`: waiting for xDrip / uploading / retrying / blocked;
- last xDrip event time;
- last successful upload time;
- pending/blocked counts;
- наличие установленного package `com.eveningoutpost.dexdrip`;
- protocol/signature mismatch без вывода payload, glucose values или credentials.

## Изменения в xDrip fork

### Стратегия fork-а

Не продолжать старый fork history как рабочую базу. Создать новый integration commit поверх свежего
`NightscoutFoundation/xDrip/master`, сохранив upstream remote. Старый commit использовать только как
справку.

Чтобы снижать вероятность merge conflicts:

- вся сериализация и intent construction живут в одном новом небольшом package, например
  `com.eveningoutpost.dexdrip.diasync`;
- в существующих xDrip classes остаются только узкие вызовы exporter-а после успешного save;
- не добавлять Diasync settings в xDrip UI, не менять xDrip DB и не трогать его upload queues;
- не переносить `toDiasyncIntent()` в xDrip model;
- держать integration одним логическим commit, который легко пересоздать/cherry-pick;
- регулярно merge-ить upstream master и прогонять xDrip build до накопления большого расхождения.

### Точки событий

1. **Libre sensor glucose** — отправлять каждую минутную raw-точку сразу после успешного
   `Libre2RawValue.save()` в `LibreReceiver`, вне пятиминутного smoothing/dedup gate для
   `BgReading`. Exporter получает конкретную `Libre2RawValue`, sensor identity и текущий calibration
   snapshot из того же processing path. Повтор raw broadcast имеет тот же deterministic event id.
2. **Manual glucose** — событие после успешного `BloodTest.create...`, только для действительно
   ручного source (`Manual Entry`). До patch-а проверить все актуальные manual-entry flows и добавить
   characterization tests, чтобы не экспортировать calibrations и Bluetooth meter records как
   manual.
3. **Carbs** — событие после успешного `Treatments.create...`, только если `carbs > 0`. Insulin не
   входит в Diasync payload. `uuid`, timestamp, grams и optional notes берутся из сохранённой entity.

`sendBroadcast()` не даёт acknowledgment — это сознательная граница. Fork не хранит свою Diasync
queue, чтобы не создавать две расходящиеся очереди и сложный cross-app ack protocol.

## Нарезка на vertical slices

Каждый slice должен быть отдельно собираемым, тестируемым и пригодным для ручной проверки.

### Slice 0 — контракт и продуктовые решения

**Scope**

- Зафиксировать этот план как approved design и обновить `docs/design.md`.
- Утвердить signing model, mode-switch policy и raw-plus-calibration glucose semantic.
- Зафиксировать JSON schema v1 и limits, реализовать platform-independent codec/validator в
  Diasync `common`, добавить golden fixtures для трёх event types.
- Зафиксировать upstream xDrip SHA, от которого начинается fork.

**Готово, когда**

- canonical valid/invalid fixtures проходят parser tests в Diasync; encoder tests нового xDrip fork-а
  в Slice 4 обязаны точно воспроизвести UTF-8 JSON content тех же valid fixtures;
- нет неоднозначности, что означает «event принят» и когда доставка считается подтверждённой.

### Slice 1 — взаимоисключающие modes без master ingest

**Scope**

- `AppMode`, preferences migration/default `SLAVE`;
- mode selector и mode-aware validation/status;
- разделение orchestration на slave/master runners;
- master runner пока показывает `Waiting for xDrip` и не вызывает network;
- stop/restart/reboot paths выбирают только сохранённый mode.

**Проверки**

- существующие slave tests остаются зелёными;
- mode нельзя менять при активном monitoring;
- master никогда не создаёт long-poll call, slave никогда не создаёт upload call;
- upgrade существующей установки остаётся в slave mode.

### Slice 2 — durable master ingest на стороне Diasync

**Scope**

- manifest receiver + signature permission;
- интеграция готового `common` parser/validator в receiver;
- Room migration с `master_events`;
- atomic merge local point + inbox/outbox insert;
- duplicate/hash-conflict handling;
- local coordinator update после commit;
- test-only sender/ADB fixture для device smoke test.

**Проверки**

- sensor/manual/carbs events сохраняются корректно;
- duplicate не создаёт вторую pending delivery;
- разные event types на одном timestamp не стирают друг друга;
- malformed/wrong-version/wrong-mode/unauthorized broadcasts ничего не меняют;
- simulated process interruption до commit не оставляет point без outbox row и наоборот.

### Slice 3 — durable upload в существующий backend

**Scope**

- HTTP data source для `addDataPoints`;
- `MasterOutboxDrainer`, batching, lease, cancellation и backoff;
- network-aware foreground runner;
- persisted `JobScheduler` recovery;
- upload diagnostics/status;
- reconciliation local server metadata из ack.

**Проверки**

- offline receive → app/process restart → network online → upload;
- server commit + lost response → retry без duplicate backend row;
- empty/multiple batches, 413 split, 4xx blocked, 5xx retry;
- concurrent FGS/job invocations не отправляют один lease одновременно;
- reboot с pending queue восстанавливает отправку;
- stop отменяет call, но не удаляет event.
- после stop persisted job всё равно доставляет уже принятый event, но receiver не принимает новый.

### Slice 4 — новый xDrip fork: Libre sensor path

**Scope**

- свежая upstream base;
- новый standalone Diasync encoder/exporter;
- узкий hook после `Libre2RawValue.save()`, вне пятиминутного `BgReading` smoothing gate;
- explicit package/action и signature permission;
- никаких backend credentials/settings в xDrip.

**Проверки**

- xDrip unit test golden fixture;
- последовательные минутные raw events создают отдельные Diasync events;
- duplicate raw event с тем же sensor id и timestamp получает тот же event id и не создаёт вторую
  logical delivery в Diasync;
- LibreReceiver остаётся функциональным без установленного Diasync;
- xDrip build/lint и device Libre smoke test;
- on-device end-to-end: Libre event появляется локально и затем на backend.

### Slice 5 — xDrip manual glucose и carbs

**Scope**

- hooks после успешного создания `BloodTest` и `Treatments`;
- фильтрация manual source и `carbs > 0`;
- description mapping из xDrip notes с length limit;
- общие encoder fixtures без копирования transport logic.

**Проверки**

- manual glucose и carbs проходят end-to-end;
- calibration/Bluetooth meter не маскируются как manual entry;
- treatment с insulin-only не отправляется;
- combined carbs+insulin отправляет только carbs;
- одинаковый timestamp с sensor/manual/carbs корректно объединяется backend-ом.

### Slice 6 — hardening и release readiness

**Scope**

- mode-aware diagnostics и notification polish;
- outbox retention и manual retry blocked rows;
- signing/build/release инструкции для пары APK;
- fork upstream-sync procedure;
- battery/Doze/process-death/reboot/device validation;
- threat review: intent spoofing, logs, backups, credential leaks;
- финальное обновление `docs/design.md` и release checklist.

**Проверки**

- `./gradlew test lint assembleDebug` в Diasync;
- релевантные xDrip Gradle checks;
- device matrix: network off/on, kill, reboot, xDrip absent/present, wrong signature, mode switches,
  phone + Wear delivery;
- ручная сверка нескольких часов Libre readings между xDrip, local Diasync и backend.

### Slice 7 — updates/deletes, если они входят в требуемую семантику

Текущий backend и protocol v1 хорошо покрывают create/insert, но не могут честно выразить удаление или
изменение timestamp. До заявления полной синхронизации manual/carbs нужно решить, должны ли
xDrip edits/deletes отражаться в Diasync.

Если да, отдельный slice включает:

- protocol operations `UPSERT`/`DELETE` и revision;
- стабильный `sourceEventId` в backend;
- tombstone/idempotency semantics;
- mapping xDrip treatment/blood-test update/delete paths;
- conflict policy и long-poll распространение изменения на slaves.

Это изменение backend-контракта, его нельзя незаметно имитировать nullable fields: текущий backend
специально сохраняет старое non-null поле при входном null.

## Архитектурные проблемы, которые нужно решить заранее

### Fire-and-forget не гарантирует первый hop

Если Diasync force-stopped, удалён, обновляется, имеет неверную подпись или receiver падает до Room
commit, xDrip не узнает о потере intent. Гарантия начинается только после durable commit в Diasync.
Для гарантии начиная с xDrip пришлось бы добавлять durable queue + acknowledgment/replay в xDrip,
что противоречит заданному fire-and-forget и заметно расширяет fork.

### Pending outbox конфликтует со строгой взаимоисключаемостью modes

Если разрешить перейти из master в slave при offline pending events, есть только три варианта:

1. продолжить upload в slave — modes уже не взаимоисключающие;
2. удалить pending — нарушить гарантию;
3. заморозить pending до следующего master start — доставка не произойдёт при появлении интернета.

Принятое решение: блокировать mode/backend/userId change, пока есть недоставленные outbox rows;
разрешать только retry или явный destructive discard с отдельным подтверждением. Доставленные rows,
оставленные на retention для dedupe, переключению не мешают.

### Подпись fork-а влияет на установку и безопасность

Signature permission требует один signing certificate. Fork xDrip, подписанный нашим ключом, нельзя
установить поверх APK xDrip с другой подписью без uninstall/restore. Нужны documented backup/migration
и единая release-signing схема. Отказ от общей подписи ослабляет защиту медицинского input channel.

### Processed и raw Libre glucose — разные данные

xDrip `BgReading.calculated_value` может включать smoothing, calibration и clamps; минутная
`Libre2RawValue.glucose` — нет. Принятое решение для v1 — передавать каждую сохранённую raw-точку и
snapshot текущей valid calibration, захваченный в том же processing step, не обращаясь к latest
`BgReading`. Diasync применяет `rawValue * slope + intercept`; при отсутствии calibration xDrip
передаёт `(1, 0)`. Processed xDrip value и его график не передаются.

### Current backend identity ограничивает event model

`(userId, timestamp)` позволяет удобно merge-ить sensor/manual/carbs, но не представляет два carbs
events в одну миллисекунду, перенос timestamp и deletion. Для create-only MVP риск мал; для полной
редактируемой истории нужен `sourceEventId`/revision/tombstone.

### Android delivery — практическая, не абсолютная гарантия

Foreground service, persisted Room outbox, JobScheduler, reboot recovery и idempotent backend дают
сильную практическую at-least-once гарантию. Очистку app data, uninstall, вечный force-stop, потерю
credential или backend, который навсегда отклоняет payload, программно преодолеть нельзя. UI обязан
показывать pending/blocked, а не молча считать такие events доставленными.

## Definition of done всей инициативы

- На одной установке в каждый момент активен только slave или master data path.
- Slave behavior и его tests не изменились.
- Принятый и committed xDrip event переживает offline, process death и reboot до backend ack.
- Retries не создают duplicate logical points в backend.
- Sensor event немедленно обновляет phone/widget/alerts/Wear до появления интернета.
- Manual glucose и carbs попадают в local DB и backend с исходным event timestamp.
- Ни xDrip intent, ни Wear payload, ни diagnostics/logs не содержат `userId` или backend credential.
- Неверно подписанный sender и malformed/unsupported payload не меняют последнее корректное state.
- Pending/blocked delivery видна пользователю и никогда не удаляется автоматически.
- Fork основан на актуальном upstream, его patch минимален и документирована процедура регулярного
  upstream merge.
- Ограничение create-only либо явно принято, либо Slice 7 завершён до заявления полной
  синхронизации edits/deletes.

## Рекомендуемый порядок

Начать со Slice 0, затем выполнить 1 → 2 → 3. После этого Diasync можно полностью проверить
с synthetic intents, не ожидая fork. Далее 4 даёт первый реальный sensor vertical slice, 5 добавляет
manual/carbs, 6 доводит систему до эксплуатации. Slice 7 выполняется, если подтверждена необходимость
синхронизировать редактирование и удаление.

Такой порядок сначала доказывает самую рискованную часть — durable acceptance/delivery — и держит
xDrip fork маленьким: xDrip только публикует versioned события, вся надёжность и backend credential
остаются в Diasync.

## Ссылки

- [xDrip upstream](https://github.com/NightscoutFoundation/xDrip)
- [существующий fork](https://github.com/illepidus/xDrip)
- [сравнение старого fork-а с upstream](https://github.com/NightscoutFoundation/xDrip/compare/master...illepidus:xDrip:master)
- [Diasync backend](https://github.com/illepidus/diasync-backend)
