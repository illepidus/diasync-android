# Создание релиза

Релизы создаются вручную через GitHub Actions запуском workflow `Create release`.

## Версионирование

Версия релиза имеет формат `major.minor`.

* Чтобы начать новую мажорную ветку релизов, вручную измени `diasyncReleaseMajor` в
  `gradle.properties`.
* Workflow находит самый большой опубликованный тег вида `v<major>.<minor>` и берёт следующий номер
  `minor`.
* Если для этого `major` ещё нет подходящих тегов релизов, `minor` начинается с `0`.

Для версии `0.0` workflow создаёт ветку `diasync-release-0.0`, запускает `./gradlew test`, собирает
три подписанных APK и публикует GitHub Release `v0.0`. Если запуск завершается с ошибкой,
ветка релиза удаляется, чтобы ту же версию можно было попробовать выпустить ещё раз.

## Секреты репозитория

Лежат [тут](https://github.com/illepidus/diasync-android/settings/secrets/actions):

* `ANDROID_KEYSTORE_BASE64`: release-keystore, закодированный в Base64 одной строкой;
* `ANDROID_KEYSTORE_PASSWORD`: пароль от keystore;
* `ANDROID_KEY_ALIAS`: alias ключа для подписи;
* `ANDROID_KEY_PASSWORD`: пароль от ключа для подписи.
