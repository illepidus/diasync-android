# Diasync Android v2 — технический дизайн

Статус: базовая версия для начала реализации  
Язык реализации: Java 17  
Целевые устройства: Android-телефон, Galaxy Watch 7, обновлённый Galaxy Watch 4

## 1. Назначение документа

Этот документ фиксирует поведение и архитектурные решения Diasync Android v2. Он является основной
технической спецификацией для Codex и человека, который принимает изменения.

При конфликте источников действует следующий порядок:

1. Явное текущее решение владельца проекта.
2. Этот `design.md`.
3. `implementation-plan.md`.
4. Поведение `diasync-old` как референс пользовательского поведения.
5. Текущая реализация `diasync-android`.
6. Предпочтения конкретного разработчика или Codex.

Старый проект используется как поведенческая спецификация и графический референс. Старые
Android-механизмы и архитектура не копируются.

## 2. Цели

Diasync должен:

- непрерывно получать точки глюкозы с Diasync backend на телефон через REST long polling;
- сохранять полученные точки локально и восстанавливаться после потери сети, process death и reboot;
- показывать текущую глюкозу, тренд, возраст данных и график в Android home-screen widget;
- передавать ограниченное окно данных с телефона на Wear OS через Wear Data Layer;
- показывать на циферблате WFF время, дату, батарею часов, график, последнее значение, тренд и
  ошибки;
- воспроизводить телефонные и часовые алерты с поведением, основанным на `diasync-old`;
- оставаться достаточно простой системой для личного использования и дальнейшей разработки
  небольшими vertical slices.

## 3. Не-цели первой версии

- GraphQL over WebSocket.
- Прямое подключение часов к backend.
- Поддержка Wear OS 3 и legacy `WatchFaceService`.
- Публикация в Google Play и соответствующая продуктовая инфраструктура.
- Несколько пользователей или профилей в одной установке.
- Медицинская сертификация и позиционирование приложения как медицинского устройства.
- Перенос PIP, запуска xDrip и прочих функций старого приложения, которые не входят в заявленный v2.
- Универсальный SDK, «чистая архитектура» ради самой архитектуры или библиотечные абстракции на
  случай гипотетического второго backend.
- Загрузка точек на backend в первом read-path релизе. Возможный producer/master-сценарий
  добавляется отдельным будущим vertical slice, не усложняя текущий consumer.

## 4. Поддерживаемая платформа

### Телефон

- `minSdk 26`.
- `targetSdk 37`.
- Java 17.
- XML Views и `RemoteViews`; Compose и Kotlin не используются.

### Wear OS

- `minSdk 33` для `wear` и `watchface`.
- Java 17 в исполняемом `wear`.
- Watch Face Format v1, который требует Wear OS 4 / API 33.
- Galaxy Watch 4 поддерживается после обновления до Wear OS 4 или новее.

### Распространение

- Личное использование.
- Debug/release APK устанавливаются через Android Studio или `adb`.
- Phone APK и Wear APK имеют один `applicationId`: `ru.krotarnya.diasync2`.
- WFF устанавливается отдельным APK с `applicationId`: `ru.krotarnya.diasync2.watchface`.

## 5. Структура Gradle-проекта

```text
Diasync
├── app        Android application для телефона
├── wear       Android application с Wear-сервисами
├── watchface  code-free Watch Face Format application
└── common     чистая Java-библиотека
```

Зависимости модулей:

```text
app  ───────► common
wear ───────► common
watchface     без code dependency
```

### `common`

Содержит только платформонезависимый Java-код:

- модели glucose/data point;
- конвертацию mg/dL ↔ mmol/L;
- вычисление trend;
- alert evaluation;
- DTO и versioned contract телефон ↔ часы;
- работу со временем через `Instant`/`Duration` и переданный `Clock`;
- чистые unit tests.

`common` не зависит от Android SDK, Room, Retrofit, Wear APIs, `Context`, ресурсов или lifecycle.

### `app`

Содержит:

- настройки и экран статуса;
- REST-клиент;
- Room database;
- foreground sync service;
- bootstrap и long-poll loop;
- phone alert engine;
- AppWidget provider и bitmap renderer;
- подготовку Wear snapshot и отправку через Data Layer;
- boot/restart orchestration.

### `wear`

Это набор сервисов, а не самостоятельный backend-клиент:

- получение Data Item от телефона;
- проверка версии payload;
- сохранение последнего snapshot;
- complication data source;
- локальный `NO DATA` watchdog;
- воспроизведение вибраций;
- запрос обновления complication.

`com.google.android.wearable.standalone` для этого модуля должен быть `false`.

### `watchface`

- `android:hasCode="false"`.
- WFF XML отвечает только за композицию и отображение.
- Время и дата рисуются WFF.
- Батарея берётся из системного watch-battery complication.
- Диабетическая область получает bitmap/text из complication provider модуля `wear`.

## 6. Backend contract

Backend: `illepidus/diasync-backend` (старый URL `diasync-server` перенаправляется на него).

Используемый REST prefix:

```text
/api/v1
```

### Bootstrap

```http
GET /api/v1/getDataPoints?userId={userId}&from={instant}&to={instant}
```

Если `from` не задан, сервер берёт последний час. Клиент всегда задаёт явное окно, достаточное для
максимального графика и безопасного overlap.

### Long poll

```http
GET /api/v1/getDataPointsLongPoll
    ?userId={userId}
    &since={updateTimestamp}
    &timeoutMs=75000
```

Семантика:

- cursor — исключительно серверный `updateTimestamp`, не timestamp измерения;
- сервер возвращает записи с `updateTimestamp > since`;
- все записи одного server batch получают одинаковый `updateTimestamp`;
- записи сортируются по `updateTimestamp`, затем по server `id`;
- пустой список означает штатный timeout, а не ошибку;
- один `(userId, timestamp)` является уникальной логической точкой;
- повторная запись того же ключа обновляет непустые поля существующей точки.

### Data point

```text
DataPoint
├── id: Long?                  server-only identity
├── userId: String             одновременно идентификатор и секрет доступа
├── timestamp: Instant         время измерения/события
├── updateTimestamp: Instant?  sync cursor
├── sensorGlucose?
│   ├── mgdl: Double
│   ├── sensorId: String
│   └── calibration?
│       ├── slope: Double
│       └── intercept: Double
├── manualGlucose?
│   └── mgdl: Double
└── carbs?
    ├── grams: Double
    └── description: String?
```

Первая версия UI строит основную линию по `sensorGlucose`. `manualGlucose` и `carbs` сохраняются
сразу, даже если их визуализация появится позже.

## 7. Локальная модель телефона

Room database содержит как минимум:

### `data_points`

- composite primary key: `(user_id, timestamp)`;
- `server_id`;
- `update_timestamp`;
- nullable sensor/manual/carbs columns;
- calibration columns;
- индекс по `(user_id, timestamp)`;
- индекс по `(user_id, update_timestamp)`.

Хранятся исходные server values. Calibration применяется при построении presentation model, а не
изменяет сохранённые данные.

### `sync_state`

- `user_id` — primary key;
- `cursor_update_timestamp`;
- `last_success_at`;
- диагностическое состояние/последняя ошибка без секретных данных.

Batch long poll применяется в одной Room-транзакции:

1. Проверить payload и `userId`.
2. Upsert всех точек.
3. Вычислить максимальный `updateTimestamp` batch.
4. Сохранить cursor.
5. Commit.

Cursor нельзя продвигать до успешного commit. Повтор batch после падения безопасен благодаря upsert.

## 8. Синхронизация телефона

### Foreground service

Непрерывный long poll живёт в одном foreground service типа `specialUse`, потому что `dataSync` при
target Android 15+ ограничен суммарно шестью часами за 24 часа.

Manifest должен содержать:

- `FOREGROUND_SERVICE`;
- `FOREGROUND_SERVICE_SPECIAL_USE`;
- `foregroundServiceType="specialUse"`;
- `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` с честным описанием непрерывного семейного glucose monitoring.

Google Play review не входит в scope, но тип сервиса должен соответствовать реальному use case.

### Ongoing notification

Notification является частью продукта и показывает:

- что мониторинг активен;
- последнее значение и возраст, если они есть;
- состояние соединения: disabled / connecting / connected; `connected` означает, что bootstrap или
  предыдущий long-poll round trip успешно завершился, при валидированной Android сети активен
  следующий long poll; отсутствие сети, request error, backoff и повторная попытка до первого
  успешного ответа показываются как `connecting`;
- действие для открытия настроек/статуса;
- действие остановки мониторинга только если пользователь явно запросил такую возможность.

### Запуск

- Первый запуск выполняется из видимой Activity после сохранения корректной конфигурации.
- После reboot receiver пытается восстановить ранее включённый мониторинг.
- `START_STICKY` и сохранённый cursor позволяют восстановиться после process death.
- Если платформа запретила background-start, состояние становится видимым в notification/UI и сервис
  запускается при следующем разрешённом пользовательском действии.

### Bootstrap algorithm

1. Взять максимальное нужное окно графика (3 часа) плюс safety margin.
2. Выполнить `getDataPoints` с явными `from` и `to`.
3. Upsert snapshot.
4. Начать long poll с безопасным overlap по `updateTimestamp`.
5. Дубликаты принять через upsert.
6. После первого непустого long-poll batch использовать только максимальный server `updateTimestamp`
   как cursor.

Bootstrap повторяется при смене backend URL/userId и может запускаться как reconciliation после
серьёзной ошибки cursor/payload.

### Long-poll loop

- Server timeout: 75 секунд.
- Client read timeout должен быть больше server timeout, например 90 секунд.
- Пустой успешный response немедленно начинает следующий poll с прежним cursor.
- После network/HTTP/parse error используется exponential backoff с jitter, ограниченный примерно
  минутой.
- После успешного HTTP round trip backoff сбрасывается.
- Остановка service отменяет активный HTTP call и executor; blocking network не работает на main
  thread.
- Локальное время никогда не заменяет server cursor после его получения.

## 9. Распространение обновлений внутри телефона

После commit новых данных один application-level coordinator последовательно инициирует:

1. пересчёт текущего presentation state;
2. проверку телефонных алертов;
3. обновление AppWidget;
4. подготовку/отправку Wear snapshot;
5. обновление ongoing notification.

Не используются широковещательные глобальные singleton/event-bus конструкции. Компоненты получают
зависимости через небольшой composition root в `Application` и явно тестируемые интерфейсы там, где
граница действительно нужна.

## 10. Глюкоза, calibration и trend

### Единицы

- Внутренние значения и пороги хранятся в mg/dL.
- UI поддерживает mg/dL и mmol/L.
- Default: mmol/L.
- mmol/L форматируется с одной цифрой после запятой; mg/dL — целым числом.

### Пороги

- Default low: `70 mg/dL` (`3.9 mmol/L`).
- Default high: `180 mg/dL` (`10.0 mmol/L`).

### Calibration

По умолчанию calibration включена. Если у sensor point присутствуют slope/intercept:

```text
displayMgdl = rawMgdl * slope + intercept
```

При выключенной calibration или отсутствии параметров используется raw mg/dL.

### Trend

Поведение повторяет `diasync-old`:

1. Найти последнюю sensor point.
2. Взять предыдущие sensor points не старше 10 минут относительно неё.
3. Вычислить их среднее.
4. `delta = latest - average(previous)`.
5. Преобразовать delta в символ:

|  Delta, mg/dL | Trend |
|--------------:|:-----:|
|    `<= -13.5` |  `⇊`  |
| `(-13.5, -7]` |  `↓`  |
|    `(-7, -3]` |  `↘`  |
|     `(-3, 3]` |  `→`  |
|      `(3, 7]` |  `↗`  |
|   `(7, 13.5]` |  `↑`  |
|      `> 13.5` |  `⇈`  |

При недостатке данных trend пустой.

## 11. Phone widget

Реализация: обычный `AppWidgetProvider`, XML `RemoteViews`, график как bitmap из Android Canvas.

### Содержимое

- последнее sensor glucose без отдельного label единицы измерения;
- опциональная trend arrow, нарисованная как bitmap без зависимости от системного шрифта и
  окрашенная так же, как последнее значение;
- возраст данных или error message;
- график;
- low/normal/high zones либо threshold lines;
- цвет значения и точек по диапазону.

### График

- Варианты окна: 30 минут, 1 час, 3 часа.
- Default: 30 минут.
- X соответствует времени.
- Y автоматически включает минимум/максимум данных и оба threshold с небольшим margin.
- Данные рисуются цветными точками, как в старом приложении.
- Радиус точек зависит от ширины widget и выбранного окна: чем длиннее окно, тем
  меньше точки; сверху радиус ограничен высотой widget.
- Bitmap строится под фактический размер widget. На Android 12+ для переданных launcher размеров
  готовится bounded exact-size `RemoteViews` mapping, чтобы home screen сам выбирал корректный
  portrait/landscape вариант независимо от orientation открытого приложения.
- Графический bitmap заполняет всю доступную площадь widget, включая сверхширокие размеры. При
  достижении pixel limit обе стороны bitmap уменьшаются пропорционально, поэтому точки остаются
  круглыми.
- Zones default: on; lines default: off; trend arrow default: on.
- Значение и trend по возможности выравниваются к левому краю.
- Размер значения и trend плавно растёт с доступной площадью widget; размер 1x1 остаётся
  нижней границей масштаба.
- Цвет значения: белый в normal range, `#FFBB33` в high range и `#C30909` в low range.

### Fresh/stale/error

- Нет точек: `NO DATA`, glucose `----`, trend скрыт, график пустой.
- Возраст меньше двух минут: сообщение пустое.
- Возраст от двух минут: жирный `Nm ago`, по центру у нижнего края отдельно от строки значения и
  trend.
- Возраст больше 10 минут: glucose дополнительно перечёркнута.
- Timestamp более чем на минуту в будущем: `DATA FROM FAR FUTURE`, glucose и график скрыты.

### Обновления

- после commit новых данных;
- после изменения размера;
- после изменения набора размеров/orientation домашнего экрана; orientation устройства остаётся
  fallback для launcher без exact-size options;
- раз в минуту при интерактивном экране для обновления возраста;
- системный `updatePeriodMillis` остаётся редким fallback, а не основным механизмом.

## 12. Phone alerts

Поведение основано на старом `Alerter`.

### Настройки

- LOW, HIGH и NO DATA включаются независимо;
- все три выключены по умолчанию;
- один общий persistent snooze;
- варианты snooze: 5, 10, 15, 20, 30 минут; 1, 2, 4, 6, 8, 10, 12, 24 часа;
- есть ручной Resume.

### Evaluation

Проверка выполняется:

- после новых данных;
- раз в минуту для повторов и NO DATA.

Глобальный minimum silence interval — 55 секунд.

Условия:

- `LOW`: latest `<= low` и либо предыдущей точки нет, либо latest строго ниже previous;
- `HIGH`: latest `>= high` и либо предыдущей точки нет, либо latest строго выше previous;
- `NO DATA`: sensor point отсутствует или последняя sensor point не обновлялась 5 минут.

Приоритет одного check:

```text
LOW > HIGH > NO DATA
```

Пока условие сохраняется, алерт может повторяться примерно раз в минуту. Snooze подавляет все типы
до указанного времени и переживает перезапуск процесса.

### Воспроизведение

- отдельные bundled sounds для low/high/no-data;
- audio usage — `USAGE_ALARM`;
- отдельный notification channel для alert visibility/actions;
- системные ограничения DND не обходятся скрытыми или привилегированными способами.

## 13. Phone → Wear protocol

Телефон является единственным владельцем backend credentials. `userId` не передаётся на часы, потому
что часы не обращаются к backend.

Основной path:

```text
/diasync/v1/state
```

Передаётся versioned snapshot, содержащий:

- `protocolVersion`;
- `generatedAt`;
- sensor points за максимальное нужное окно;
- display unit;
- low/high thresholds;
- выбранное graph window;
- calibration/display parameters;
- рассчитанный trend;
- alert configuration и `snoozedUntil`, нужные часовому watchdog;
- optional fresh low/high alert event с устойчивым `eventId` и сроком годности.

Snapshot передаётся через `DataClient` как urgent Data Item:

- это состояние, а не одноразовое сообщение;
- новый snapshot целиком заменяет предыдущий;
- после reconnect часы получают последнее состояние;
- размер payload контролируется; передаётся только bounded window;
- формат сериализации — компактный JSON с contract tests; compression добавляется только при
  доказанной необходимости.

Alert event содержит measurement timestamp/type в `eventId`. Часы хранят последний обработанный
event и не вибрируют повторно после process restart. Просроченное событие после долгого reconnect
игнорируется.

## 14. Wear persistence и complication

### Persistence

- Последний корректный snapshot хранится в app-private/device-protected storage.
- Отдельная Room database на часах в первой версии не нужна.
- Неподдерживаемая версия или повреждённый payload не заменяет последнее корректное состояние.
- После reboot/process death complication может немедленно отрисовать последнее состояние как stale.

### Complication provider

Исполняемый `wear` предоставляет complication data source и запрашивает обновление после нового
snapshot или изменения stale/no-data state.

Для воспроизведения старого сложного дизайна provider может генерировать цветной bitmap (
`PHOTO_IMAGE`) с:

- графиком;
- последним значением;
- trend;
- stale/error text.

WFF размещает этот image complication вместе со своими time/date и системной батареей.

### Watchface presentation

- время `HH:mm`;
- дата `EE dd.MM`;
- battery percent;
- charging — зелёный;
- critical battery `<= 15%` — красный;
- normal battery — белый;
- black background;
- visual stale после 90 секунд: показывать возраст в минутах;
- payload отсутствует: `NO DATA`;
- interactive и ambient layouts не должны расходиться по смыслу; ambient упрощает цвет/детали ради
  энергопотребления.

## 15. Watch alerts

### LOW/HIGH

- Решение LOW/HIGH вычисляет телефон по тем же правилам, что phone alert engine.
- Телефон включает fresh alert event в Wear snapshot.
- Настройки enabled/snooze передаются как часть policy, чтобы оба устройства имели предсказуемое
  поведение.
- Wear дедуплицирует event и проверяет его свежесть.

Vibration patterns из старого приложения:

- LOW: `800ms on, 400ms off, 800ms on` с высокой amplitude;
- HIGH: `1000ms on` с высокой amplitude.

### NO DATA

- Вычисляется локально на часах по timestamp последней sensor point.
- Порог alarm: 5 минут.
- Проверка не зависит от связи с телефоном.
- Enabled/snooze policy берётся из последнего snapshot.
- Повтор ограничивается примерно одним разом в минуту.
- Появление свежих данных немедленно сбрасывает NO DATA state.

## 16. Settings и status UI

Activity приложения является configuration/status UI, а не основным ежедневным экраном.

Минимальные настройки:

- backend base URL;
- `userId`/token;
- monitoring enabled;
- unit;
- low/high;
- use calibration;
- widget graph period;
- widget zones/lines/trend arrow;
- watch graph period;
- low/high/no-data alerts;
- snooze/resume.

Monitoring включается и останавливается одной context-aware кнопкой, которая меняет действие и текст
между `Start monitoring` и `Stop monitoring`.

Status показывает:

- disabled/connecting/connected;
- last successful response;
- last data timestamp/age;
- количество локальных точек в рабочем окне;
- Wear connection/snapshot status без раскрытия credential.

## 17. Ошибки и восстановление

| Событие                   | Поведение                                                              |
|---------------------------|------------------------------------------------------------------------|
| Long poll timeout         | Немедленно следующий poll, не показывать как ошибку                    |
| Нет сети                  | Сохранить данные, показать connecting, backoff+jitter                  |
| HTTP/JSON error           | Не менять cursor; retry; оставить последнее корректное состояние       |
| Duplicate batch           | Безопасный Room upsert                                                 |
| Process death             | `START_STICKY`, восстановить cursor и loop                             |
| Reboot телефона           | boot receiver восстанавливает enabled monitoring                       |
| Смена URL/userId          | Остановить loop, очистить/переключить active state, bootstrap заново   |
| Watch disconnected        | Телефон продолжает работу; последний Data Item догоняет часы           |
| Reboot часов              | Загрузить persisted snapshot, показать stale, ждать Data Item          |
| Повреждённый Wear payload | Отклонить; сохранить последнее корректное состояние                    |
| Timestamp из будущего     | Явная ошибка presentation; не использовать как свежую нормальную точку |

## 18. Privacy и security

- `userId` рассматривается как bearer secret.
- Secret не попадает в логи, notification, widget, Wear payload, exception text или analytics.
- Настройки и БД app-private.
- Backup разрешён осознанно для личного приложения, включая настройки и данные.
- По умолчанию разрешён только HTTPS; cleartext не включается глобально.
- В проекте нет analytics/telemetry.
- Логи содержат состояние и counts, но не значения медицинских данных и не credentials, если это не
  требуется для локальной отладки и явно не включено developer build.

## 19. Наблюдаемость

- Короткие стабильные log tags по подсистемам: Sync, Db, Widget, Alert, WearSync, Complication.
- Sync state доступен в UI и foreground notification.
- Ошибка хранится как тип/короткое безопасное описание, а не полный потенциально секретный response.
- Debug build может иметь действие «export diagnostics», но оно не входит в первые slices.

## 20. Тестирование

### `common`

- unit conversion;
- calibration;
- trend boundary table;
- LOW/HIGH worsening rules;
- priority;
- 55-second throttle;
- 5-minute NO DATA;
- snooze persistence model;
- Wear DTO round trip/version rejection.

### `app`

- Room upsert и cursor transaction;
- bootstrap overlap;
- empty long-poll timeout;
- network error/backoff/cancellation;
- widget presentation states и bitmap bounds;
- alarm scheduling/repetition;
- service restart state.

### `wear`

- snapshot validation/persistence;
- alert event dedupe/expiry;
- local NO DATA clock tests;
- complication state mapping;
- bitmap renderer bounds.

### Device smoke tests

- phone install/config/bootstrap;
- widget add/resize/fresh/stale;
- process kill и reboot;
- network off/on;
- Watch 7 Data Layer/complication/WFF/vibration;
- updated Watch 4 install and display;
- watch disconnect/reconnect и watch reboot;
- ambient/AOD.

## 21. Проверяемые требования

| ID    | Требование                                                                                        |
|-------|---------------------------------------------------------------------------------------------------|
| FR-1  | Пользователь может настроить URL, userId, единицы, пороги, graph windows и alerts                 |
| FR-2  | Телефон выполняет bootstrap REST window и сохраняет точки локально                                |
| FR-3  | Телефон непрерывно получает long-poll updates в foreground service                                |
| FR-4  | Data batch и server cursor сохраняются атомарно и idempotently                                    |
| FR-5  | Widget воспроизводит старое glucose/trend/graph/stale/error поведение                             |
| FR-6  | Phone alerts воспроизводят старые правила, sounds, repeats и snooze                               |
| FR-7  | Телефон передаёт bounded persistent snapshot на Wear без credential                               |
| FR-8  | WFF показывает time/date/battery/glucose/trend/graph/errors                                       |
| FR-9  | Wear воспроизводит LOW/HIGH и локально вычисляет NO DATA                                          |
| FR-10 | Система восстанавливается после network loss, process death, reboot и Wear reconnect              |
| NFR-1 | При нормальной сети новая server point отражается на телефоне и часах не позднее 60 секунд        |
| NFR-2 | Повтор batch или crash между получением и commit не теряет данные и не продвигает cursor ошибочно |
| NFR-3 | Все blocking/network/render операции выполняются вне main thread                                  |
| NFR-4 | Credentials не покидают телефон и не попадают в пользовательские surfaces/logs                    |
| NFR-5 | Поведение времени, trend, alerts и serialization покрыто deterministic tests с injected `Clock`   |

## 22. Блокирующие открытые вопросы

На момент этой версии блокирующих продуктовых вопросов нет. Новые решения, которые меняют
пользовательское поведение или границы модулей, сначала вносятся в этот документ, затем реализуются
отдельным vertical slice.

## 23. Референсы

- Новый Android skeleton: <https://github.com/illepidus/diasync-android>
- Backend: <https://github.com/illepidus/diasync-backend>
- Старый widget: <https://github.com/illepidus/diasync-old/blob/master/mobile/src/main/java/ru/krotarnya/diasync/Libre2Widget.java>
- Старый graph renderer: <https://github.com/illepidus/diasync-old/blob/master/mobile/src/main/java/ru/krotarnya/diasync/Libre2GraphBuilder.java>
- Старый alert engine: <https://github.com/illepidus/diasync-old/blob/master/mobile/src/main/java/ru/krotarnya/diasync/Alerter.java>
- Старый watch renderer: <https://github.com/illepidus/diasync-old/blob/master/wear/src/main/java/ru/krotarnya/diasync/WatchFaceRenderer.java>
- Старый watch service: <https://github.com/illepidus/diasync-old/blob/master/wear/src/main/java/ru/krotarnya/diasync/service/MainWatchFaceService.java>
- Android FGS timeouts: <https://developer.android.com/develop/background-work/services/fgs/timeout>
- Android FGS types: <https://developer.android.com/develop/background-work/services/fgs/service-types>
- Watch Face Format: <https://developer.android.com/training/wearables/wff>
