# План улучшения авто-скролла новелл

Дата: 2026-06-23
Проект: `ranobe-aniyomi`
Фокус: авто-скролл в reader flow для новелл, включая native scroll, page reader и WebView renderer.

## 1. Метод работы

План составлен по дисциплине fable-mode:

1. Сначала зафиксирована карта этапов и проверяемые артефакты.
2. Прочитаны релевантные участки кода, а не только названия настроек.
3. Отдельно сверены UX-паттерны для infinite scroll, seamless chapter transition и доступности.
4. Сформирован план по конкретным файлам.
5. Выполнен skeptical self-review плана.
6. План обновлен после внешнего review `NOVEL_AUTO_SCROLL_REVIEW.md`. Критичные замечания review приняты: handoff должен жить в `NovelReaderScreenModel`, WebView нельзя опрашивать через JS на каждом кадре, переход в конец главы должен блокироваться до готовности контента и первого layout pass, пользовательские настройки нужно упростить.

Внешние источники, использованные для UX-ориентиров:

- Nielsen Norman Group, `Infinite Scrolling: When to Use It, When to Avoid It`, ключевые выводы: infinite scroll снижает стоимость взаимодействия, но создает проблемы с ориентацией, возвратом к месту, доступностью и ощущением конца списка. URL: https://www.nngroup.com/articles/infinite-scrolling-tips/
- Smashing Magazine, `Infinite Scroll UX Done Right`, ключевые выводы: нужно давать ориентиры, разделять старый и новый контент, не ломать footer/end-state, думать о bookmark/location, доступности и производительности. URL: https://www.smashingmagazine.com/2022/03/designing-better-infinite-scroll/
- Readest issue #2777, `Seamless Infinite Scroll between chapters`, ключевой пользовательский pain: hard jump/reload между главами ломает immersion, следующий chapter лучше добавлять под текущий или хотя бы сглаживать переход. URL: https://github.com/readest/readest/issues/2777
- Android Compose accessibility defaults, ключевой вывод: интерактивные элементы должны иметь минимум 48dp touch target, Material компоненты дают часть semantics, но кастомные компоненты нужно проверять отдельно. URL: https://developer.android.com/develop/ui/compose/accessibility/api-defaults

## 2. Текущий flow авто-скролла новелл

### 2.1 Основные файлы

- `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderScreen.kt`
  - хранит локальные состояния `autoScrollEnabled`, `autoScrollSpeed`, `autoScrollExpanded`, `autoScrollWasUsed`, `touchCooldownUntilNanos`, `speedFactor`;
  - содержит основной `LaunchedEffect` авто-скролла;
  - переключает режимы native scroll, page reader и WebView;
  - открывает следующую главу через `openNextChapterFromReader()`;
  - показывает `AutoScrollActionFab`.
- `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderLayout.kt`
  - содержит pure helpers для скорости, page delay, frame step, remainder accumulation и initial state;
  - `resolveInitialAutoScrollEnabled()` сейчас всегда возвращает `false`.
- `app/src/main/java/eu/kanade/presentation/reader/novel/GeneralTab.kt`
  - содержит настройки скорости авто-скролла и `autoScrollOffset`.
- `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsNovelReaderScreen.kt`
  - дублирует глобальные настройки скорости и offset.
- `app/src/main/java/eu/kanade/tachiyomi/ui/reader/novel/setting/NovelReaderPreferences.kt`
  - хранит `autoScroll`, `autoScrollInterval`, `autoScrollOffset`, `showAutoScrollFloatingButton`, `prefetchNextChapter`, `swipeToNextChapter`.
- `app/src/main/java/eu/kanade/presentation/reader/components/AutoScrollActionFab.kt`
  - кастомная кнопка play/pause.
- `app/src/test/java/eu/kanade/presentation/reader/novel/NovelReaderUiVisibilityTest.kt`
  - покрывает speed mapping, frame step, remainder, UI toggle и cooldown constants.

### 2.2 Как сейчас работает авто-скролл

1. При входе в главу `autoScrollEnabled` инициализируется через `resolveInitialAutoScrollEnabled(savedPreferenceEnabled = state.readerSettings.autoScroll)`, но функция всегда возвращает `false`.
2. Пользователь включает авто-скролл из верхней панели или FAB.
3. При включении верхняя и нижняя панели скрываются через `resolveAutoScrollUiStateOnToggle()`.
4. В `LaunchedEffect` запускается цикл:
   - если открыт UI, цикл ждет;
   - если активен touch cooldown, скорость плавно уменьшается через `speedFactor`;
   - если cooldown закончился, скорость плавно возвращается к 1;
   - в WebView и native scroll режимах используется `withFrameNanos`, `autoScrollFrameStepPx()` и `scrollBy()`;
   - в page reader режиме используется `delay(autoScrollPageDelayMs(autoScrollSpeed))` и переход на следующую страницу;
   - в конце главы, если `swipeToNextChapter == true` и есть `nextChapterId`, вызывается `openNextChapterFromReader()`;
   - перед открытием следующей главы авто-скролл выключается.
5. На смене главы локальные состояния пересоздаются по `state.chapter.id`, поэтому авто-скролл на новой главе не продолжается.

## 3. Подтвержденные слабые места

### 3.1 Авто-переход на следующую главу есть, но flow неполный

В коде уже есть переход к следующей главе из авто-скролла, но он завязан на настройку `swipeToNextChapter`. Это слабая модель: пользователь может хотеть авто-продолжение для auto-scroll, но не хотеть swipe gestures между главами. Кроме того, перед переходом `autoScrollEnabled = false`, а на новой главе state сбрасывается, поэтому continuous auto-scroll across chapters фактически отсутствует.

### 3.2 Настройка `autoScrollOffset` фактически не участвует в чтении

`autoScrollOffset` есть в `NovelReaderPreferences`, `GeneralTab` и `SettingsNovelReaderScreen`, но в `NovelReaderScreen.kt` не используется для native scroll, WebView или page reader. Это создает ложную настройку: пользователь меняет параметр, но flow чтения не меняется.

### 3.3 Слишком резкое окончание главы

Текущая логика открывает следующую главу сразу при достижении конца. В native scroll условие `consumed == 0f || !textListState.canScrollForward`, в WebView условие `!webView.canScrollVertically(1)`. Нет dwell-паузы, нет chapter end card, нет countdown, нет отмены перехода. Это может ощущаться как преждевременный прыжок на последних строках, особенно на медленных устройствах, при WebView reflow или при больших нижних отступах.

### 3.4 Prefetch не связан напрямую с авто-скроллом

В настройках есть `prefetchNextChapter`, а `NovelReaderScreenModel` умеет prefetch, но auto-scroll не усиливает preload, когда пользователь гарантированно движется к концу. При переходе следующая глава может грузиться с задержкой, что ломает immersion.

### 3.5 Скорость задана как пиксели на кадр, а не как читательская скорость

`autoScrollScrollStepPx()` задает 0.5..5 px/frame. Это зависит от density, шрифта, lineHeight, размера экрана и режима рендера. В page reader скорость задана фиксированным delay 2..10 секунд на страницу, не учитывая количество текста на странице. На практике одна и та же скорость будет по-разному ощущаться на разных настройках текста.

### 3.6 WebView end detection и native end detection хрупкие

`canScrollVertically(1)` в WebView может быть нестабилен во время загрузки, reflow, применения CSS/JS или восстановления scroll position. В native `scrollBy()` может вернуть 0 при временной layout-ситуации. Сейчас нет state machine с подтверждением конца на нескольких кадрах. После review добавлено критичное ограничение: end detection должен быть заблокирован, пока контент не готов и пока контейнер не прошел минимум один layout pass. Иначе при открытии новой главы пустой список или еще не построенный DOM могут быть ошибочно приняты за конец главы и запустить бесконечный цикл переходов.

### 3.6.1 WebView нельзя опрашивать через JS на каждом кадре

Изначальная идея считать `distanceToBottom` через `evaluateJavascript` внутри frame-loop неприемлема для производительности. Такой подход создает высокочастотный мост Kotlin-WebView, повышает нагрузку на CPU, расход батареи и риск jank. WebView end detection должен быть событийным: `WebView.onScrollChanged`, throttled JS listener или `@JavascriptInterface`, который сообщает Kotlin только о пересечении порога конца, а не на каждом кадре.

### 3.6.2 Handoff нельзя хранить только в локальном Compose state

Локальные `remember` состояния в `NovelReaderScreen.kt` пересоздаются при смене главы. Поэтому `pendingAutoScrollHandoff`, режим непрерывного чтения и target следующей главы должны храниться на уровне `NovelReaderScreenModel` или в небольшом lifecycle-aware holder, управляемом ScreenModel. В `NovelReaderScreen.kt` должны остаться только визуальные и frame-local состояния: remainder, текущий speed factor, локальный флаг layout-ready.

### 3.7 UI и accessibility недоделаны

`AutoScrollActionFab` имеет `contentDescription = null`, long-click для раскрытия настроек не очевиден. В `NovelReaderScreen.kt` часть иконок auto-scroll panel имеет `contentDescription = null`. Android Compose рекомендации требуют минимум 48dp touch target и внимательной настройки semantics для кастомных компонентов. Размер FAB 48dp есть, но semantics недостаточны.

### 3.8 Сохранение настройки `autoScroll` противоречиво

Есть preference `autoScroll`, но старт всегда disabled. При этом `LaunchedEffect(state.chapter.id)` снова сохраняет true, если setting true. Это выглядит как наследие старой логики и может приводить к путанице: preference есть, но не значит автозапуск, а UI может сохранять состояние, которое не применяется.

### 3.9 Взаимодействие с TTS, выделением текста и пользовательскими жестами не оформлено как политика

Код уже учитывает TTS и selection в разных местах, но auto-scroll не имеет явной policy-таблицы: что делать при TTS playback, выделении текста, ручном скролле, открытии меню, смене renderer, WebView загрузке, ошибке главы. Это повышает риск регрессий.

## 4. Целевое поведение

### 4.1 Минимальный целевой UX

1. У авто-скролла должны быть отдельные настройки:
   - `Auto-scroll to next chapter`, включено или выключено;
   - `Continue auto-scroll after chapter change`, включено или выключено;
   - `Pause at chapter end`, например 0.5..10 секунд;
   - `End offset`, использовать существующий `autoScrollOffset` или переименовать в понятный параметр.
2. При приближении к концу текущей главы auto-scroll должен заранее инициировать prefetch следующей главы.
3. В конце главы должен появляться контролируемый переход:
   - короткая пауза;
   - overlay/card `Следующая глава через N сек`;
   - кнопки `Перейти сейчас`, `Остановить`, `Остаться`.
4. Если включено continue across chapters, auto-scroll должен восстановиться после загрузки новой главы без ручного нажатия play.
5. Если следующей главы нет или загрузка не удалась, auto-scroll должен остановиться и показать понятный end-state.

### 4.2 Более качественный UX, второй этап

1. Seamless mode: следующая глава добавляется ниже текущей в одном scroll контейнере, хотя бы для native scroll renderer.
2. Chapter boundary как явный landmark: заголовок следующей главы, дата, source, progress.
3. Adaptive speed: первый шаг должен быть дешевым, через px/sec с учетом lineHeight и через заранее посчитанную длину page reader страницы в символах. WPM не делать в P0/P1 без отдельной оценки стоимости.
4. WebView fallback: если seamless невозможен, использовать transition screen и restore state без резкого blank/reload.

## 4.3 Корректировки после `NOVEL_AUTO_SCROLL_REVIEW.md`

Review принят как обязательное ограничение для реализации:

1. `handoff` хранить в `NovelReaderScreenModel`, потому что локальный Compose state не переживает смену главы.
2. Не использовать `evaluateJavascript` в frame-loop. WebView near-end должен быть event-based или throttled.
3. Заблокировать `EndDwell` до готовности контента, первого layout pass и появления renderable items.
4. Свести пользовательские настройки к одному режиму поведения в конце главы. Prefetch threshold оставить внутренней константой.
5. Для E-Ink не делать секундный анимированный countdown.
6. Adaptive delay для page reader считать дешево, по заранее известной длине страницы в символах, а не через динамический WPM-подсчет.

## 5. Детальный план по файлам

### Phase 1. Выделить policy и state machine авто-скролла

Review correction: policy может быть pure, но lifecycle-состояние непрерывного перехода между главами не должно жить только в Compose. Phase 1 теперь делится на pure policy и ScreenModel-owned handoff state.

#### Создать `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderAutoScrollPolicy.kt`

Добавить pure-модели:

```kotlin
enum class NovelAutoScrollMode { Off, Running, Cooldown, EndDwell, Handoff, Paused }

enum class NovelAutoScrollChapterEndBehavior {
    StopAtEnd,
    AdvanceAndStop,
    ContinuousReading,
}

data class NovelAutoScrollConfig(
    val enabled: Boolean,
    val speed: Int,
    val chapterEndBehavior: NovelAutoScrollChapterEndBehavior,
    val endPauseMs: Long,
    val endOffsetPx: Int,
)

private const val AUTO_SCROLL_PREFETCH_THRESHOLD_PERCENT = 85

data class NovelAutoScrollEndState(
    val isAtEnd: Boolean,
    val stableEndFrameCount: Int,
    val shouldEnterDwell: Boolean,
    val shouldAdvanceNow: Boolean,
)
```

Добавить pure functions:

- `resolveNovelAutoScrollEndState(...)`
  - принимает `canScrollForward`, `scrollConsumedPx`, `isContentReady`, `hasCompletedInitialLayout`, `hasRenderableItems`, `previousStableEndFrameCount`, `requiredStableFrames`;
  - требует подтверждения конца на 2-3 кадрах, чтобы не прыгать при transient layout;
  - всегда возвращает `shouldEnterDwell = false`, если контент еще не готов, не было layout pass или список/DOM еще пустой.
- `resolveNovelAutoScrollHandoff(...)`
  - отделяет поведение авто-скролла в конце главы от `swipeToNextChapter`;
  - использует `NovelAutoScrollChapterEndBehavior`, а не пару switch-флагов.
- `resolveAutoScrollSpeedFactor(...)`
  - переносит cooldown ramp из `NovelReaderScreen.kt` в тестируемую pure-функцию.
- `resolveAutoScrollPrefetchNeeded(...)`
  - вычисляет, когда prefetch нужен по percent или distance-to-end.
- `resolveInitialAutoScrollEnabled(...)`
  - заменить текущую функцию или добавить новую с явной семантикой:
    - default: не стартовать автоматически при открытии reader;
    - если есть pending handoff от auto-scroll, стартовать на новой главе.

Проверка:

- Unit tests должны покрыть stable end detection, readiness guard, dwell, handoff, cooldown ramp, continue across chapter.

#### Обновить `app/src/main/java/eu/kanade/tachiyomi/ui/reader/novel/NovelReaderScreenModel.kt`

Добавить ScreenModel-owned состояние handoff, потому что локальный Compose state уничтожается при смене главы. Минимальная модель:

```kotlin
data class NovelAutoScrollHandoffState(
    val active: Boolean = false,
    val fromChapterId: Long? = null,
    val targetChapterId: Long? = null,
    val speed: Int = 50,
    val requestedAtMs: Long = 0L,
)
```

Добавить intents:

- `prepareAutoScrollHandoff(targetChapterId: Long, speed: Int)`;
- `consumeAutoScrollHandoffIfMatches(currentChapterId: Long): NovelAutoScrollHandoffState?`;
- `cancelAutoScrollHandoff(reason: String)`.

Правила:

- handoff создается только из активной auto-scroll сессии, не из stale preference;
- handoff имеет TTL, например 30 секунд, чтобы не запускать авто-скролл после долгой навигации назад;
- manual stop, смена главы вручную, ошибка загрузки или закрытие reader сбрасывают handoff.

Проверка:

- unit tests на create, consume, mismatch, TTL expiry и cancel.

#### Обновить `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderLayout.kt`

Сделать файл только для layout/speed helpers или оставить backward-compatible wrappers.

Изменения:

- оставить `intervalToAutoScrollSpeed`, `autoScrollSpeedToInterval`, `autoScrollFrameStepPx`;
- перенести или продублировать policy helpers в новый `NovelReaderAutoScrollPolicy.kt`;
- изменить `resolveInitialAutoScrollEnabled()` так, чтобы она не игнорировала handoff context, полученный из ScreenModel, но по-прежнему не включала автозапуск только из старого `autoScroll` preference.

Проверка:

- существующие тесты `NovelReaderUiVisibilityTest` должны продолжать проходить;
- новые тесты не должны требовать Compose runtime.

### Phase 2. Упростить preferences и устранить путаницу `swipeToNextChapter`

Review correction: не добавлять 4 пользовательские настройки. Это settings bloat. Для пользователя нужен один понятный режим поведения в конце главы, а prefetch threshold должен быть внутренней константой.

#### Обновить `app/src/main/java/eu/kanade/tachiyomi/ui/reader/novel/setting/NovelReaderPreferences.kt`

Добавить одну preference поведения в конце главы:

```kotlin
fun autoScrollChapterEndBehavior() =
    preferenceStore.getEnum(
        "novel_reader_auto_scroll_chapter_end_behavior",
        NovelAutoScrollChapterEndBehavior.StopAtEnd,
    )

fun autoScrollEndPauseMs() =
    preferenceStore.getLong("novel_reader_auto_scroll_end_pause_ms", 1500L)
```

В data classes настроек добавить поля:

- `autoScrollChapterEndBehavior: NovelAutoScrollChapterEndBehavior`
- `autoScrollEndPauseMs: Long`

Не добавлять в пользовательские настройки `autoScrollPrefetchThresholdPercent`. Использовать внутреннюю константу `AUTO_SCROLL_PREFETCH_THRESHOLD_PERCENT = 85` в policy или ScreenModel. Если позже понадобится тонкая настройка, включать ее только в advanced/debug settings.

Важно:

- не переиспользовать `swipeToNextChapter` для auto-scroll;
- миграцию сделать мягкой: default `StopAtEnd`;
- `autoScrollOffset` либо начать реально использовать, либо пометить как legacy и заменить на `endOffsetPx`;
- если старые поля `autoScrollAdvanceToNextChapter` или `autoScrollContinueAcrossChapters` уже появятся в ветке, не тащить их в UI, а мигрировать в enum.

Проверка:

- `rg "autoScrollChapterEndBehavior"` должен показать подключение в prefs, settings state и UI;
- `rg "autoScrollPrefetchThresholdPercent"` не должен находить пользовательскую preference.

#### Обновить `app/src/main/java/eu/kanade/presentation/reader/novel/GeneralTab.kt`

Добавить в раздел авто-скролла:

- SingleChoice/List preference: `Поведение в конце главы`;
  - `Останавливаться в конце`;
  - `Переходить к следующей и останавливаться`;
  - `Непрерывное чтение`;
- Slider/List preference: `Пауза в конце главы`, значения 0.5, 1, 2, 3, 5, 10 сек;
- Переименовать `autoScrollOffset` в понятный `Буфер конца главы` или убрать из UI до реализации.

Не добавлять slider для prefetch threshold в основной UI. Порог 85% должен быть internal default.

Проверка:

- при изменении каждой настройки обновляется source override и global preference по той же схеме, что текущие настройки.

#### Обновить `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsNovelReaderScreen.kt`

Синхронизировать глобальный экран настроек с `GeneralTab.kt`.

Проверка:

- не должно быть ситуации, где настройка есть в quick dialog, но отсутствует в global settings, или наоборот.

#### Обновить i18n ресурсы

Вероятные файлы:

- `i18n-aniyomi/src/commonMain/moko-resources/base/strings.xml`
- `i18n-aniyomi/src/commonMain/moko-resources/ru/strings.xml`
- при необходимости соответствующие `i18n/src/commonMain/moko-resources/...`

Добавить строки:

- `novel_reader_auto_scroll_chapter_end_behavior`
- `novel_reader_auto_scroll_chapter_end_stop`
- `novel_reader_auto_scroll_chapter_end_advance_stop`
- `novel_reader_auto_scroll_chapter_end_continuous`
- `novel_reader_auto_scroll_end_pause`
- `novel_reader_auto_scroll_next_countdown`
- `novel_reader_auto_scroll_next_static_eink`
- `novel_reader_auto_scroll_stop_here`
- `novel_reader_auto_scroll_go_now`

Проверка:

- генерация ресурсов проходит через Gradle;
- нет hardcoded English strings в auto-scroll UI.

### Phase 3. Переписать runtime effect в `NovelReaderScreen.kt`

#### Обновить `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderScreen.kt`

Цель: уменьшить текущий большой `LaunchedEffect`, сделать handoff и end dwell явными.

Шаги:

1. Добавить локальное состояние только для текущего render session:
   - `autoScrollMode: NovelAutoScrollMode`;
   - `autoScrollEndStableFrames: Int`;
   - `autoScrollEndDwellStartedAtMs: Long?`;
   - `hasCompletedInitialLayout: Boolean`;
   - `hasRenderableItems: Boolean`.
2. Не хранить `pendingAutoScrollHandoff` как локальный `remember`. Для непрерывности между главами использовать intents ScreenModel из Phase 1.
3. Заменить прямую проверку `state.readerSettings.swipeToNextChapter` в auto-scroll loop на `state.readerSettings.autoScrollChapterEndBehavior`.
4. Перед вызовом `openNextChapterFromReader()`:
   - если выбран `ContinuousReading`, вызвать `viewModel.prepareAutoScrollHandoff(targetChapterId, speed)`;
   - если выбран `AdvanceAndStop`, не создавать handoff;
   - не просто `autoScrollEnabled = false`, а перевести локальный mode в `Handoff`;
   - показать overlay или хотя бы не показывать play/pause как stopped.
5. На новой главе:
   - запросить `consumeAutoScrollHandoffIfMatches(state.chapter.id)` из ScreenModel;
   - включить auto-scroll только после `isContentReady`, первого layout pass и появления renderable items;
   - восстановить speed и hide UI;
   - если handoff mismatch, expired или отменен, не стартовать.
6. Для native scroll:
   - заменить `consumed == 0f || !textListState.canScrollForward` на stable end detection;
   - передавать в policy `isContentReady`, `hasCompletedInitialLayout`, `hasRenderableItems`;
   - учитывать `autoScrollOffset` или новый `endOffsetPx` через bottom spacer/end threshold.
7. Для WebView:
   - проверять content ready и stable end несколько кадров;
   - при WebView loading/reflow паузить, а не считать конец;
   - при `webViewInstance == null` сохранять mode running, но не терять ScreenModel handoff;
   - не вызывать `evaluateJavascript` из frame-loop.
8. Для page reader:
   - использовать end dwell перед переходом;
   - если текущая page имеет мало/много текста, в Phase 6 добавить adaptive delay.
9. При открытии reader UI:
   - сохранить текущую паузу как `PausedByUi`, чтобы возврат был предсказуемым;
   - не сбрасывать remainder и mode неконтролируемо.

Проверка:

- ручной сценарий: native scroll, включить auto-scroll, дойти до конца, увидеть dwell/countdown, перейти на следующую главу, auto-scroll продолжается только в режиме `ContinuousReading`;
- ручной сценарий: выбрать `AdvanceAndStop`, убедиться, что следующая глава открывается, но авто-скролл не запускается;
- ручной сценарий: выбрать `StopAtEnd`, убедиться, что в конце авто-скролл останавливается и не использует swipe setting;
- сценарий защиты от race condition: новая глава открыта, но список/DOM еще пустой, auto-scroll не должен переходить дальше.

### Phase 4. Prefetch и chapter handoff без рывков

#### Обновить `app/src/main/java/eu/kanade/tachiyomi/ui/reader/novel/NovelReaderScreenModel.kt`

Найти существующую prefetch-логику вокруг `prefetchNextChapter` и добавить публичный intent, например:

```kotlin
fun requestAutoScrollNextChapterPrefetch()
```

Логика:

- если next chapter уже загружена или грузится, ничего не делать;
- если auto-scroll active и progress >= `AUTO_SCROLL_PREFETCH_THRESHOLD_PERCENT`, prefetch even if general `prefetchNextChapter` disabled, но только если выбран `AdvanceAndStop` или `ContinuousReading`;
- не запускать параллельные prefetch запросы;
- логировать причину prefetch.

Проверка:

- unit или integration-style test на idempotency prefetch;
- ручной тест на слабой сети: при достижении конца переход не показывает долгий blank.

#### Обновить `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderProgressPolicy.kt`

Если возможно, добавить helper для progress percent, который одинаково работает для:

- native scroll;
- page reader;
- WebView progress;
- rich native blocks.

Проверка:

- tests на 0%, 85%, 100%, empty content.

### Phase 5. UI конца главы и управление переходом

#### Создать `app/src/main/java/eu/kanade/presentation/reader/novel/NovelReaderAutoScrollEndOverlay.kt`

Компонент:

- показывается в `EndDwell` mode;
- текст: `Следующая глава через 2 сек`;
- кнопки: `Сейчас`, `Остановить`, `Остаться`;
- показывает title следующей главы, если доступен;
- не перекрывает важные последние строки, лучше bottom sheet/card с прозрачным фоном;
- поддерживает e-ink theme без тяжелых анимаций;
- на E-Ink не обновляет текст каждую секунду. Вместо `Следующая глава через N сек` использовать статичный текст вроде `Переход к следующей главе...` или один финальный refresh.

Проверка:

- Compose preview или screenshot test, если проект поддерживает;
- минимум 48dp для кнопок;
- content descriptions и localized strings.

#### Обновить `app/src/main/java/eu/kanade/presentation/reader/components/AutoScrollActionFab.kt`

Изменения:

- добавить параметры `contentDescription`, `longClickLabel`;
- добавить `Modifier.semantics { ... }`, если `combinedClickable` сам не дает достаточный label;
- не использовать `contentDescription = null` для play/pause;
- добавить optional progress/countdown badge в будущем.

Проверка:

- unit/UI проверка semantics, если доступна;
- ручная проверка TalkBack.

#### Обновить `NovelReaderScreen.kt` для overlay

Добавить `NovelReaderAutoScrollEndOverlay` поверх reader content, но ниже critical dialogs.

Проверка:

- overlay не появляется при ручном swipe next;
- overlay скрывается при stop, menu open, manual scroll вверх, text selection.

### Phase 6. Скорость и адаптация под текст

#### Обновить `NovelReaderLayout.kt` или новый `NovelReaderAutoScrollSpeedPolicy.kt`

Review correction: не начинать с тяжелого динамического WPM-подсчета на лету. Для первого улучшения использовать дешевые вычисления, подготовленные при построении страниц.

Минимально реалистичный первый шаг:

- оставить текущий slider 1..100;
- внутри пересчитывать target px/sec с учетом density и lineHeight;
- для page reader считать delay по длине текста страницы в символах, вычисленной один раз при создании `pageReaderPages` или rich page slices;
- не считать слова заново в frame-loop или при каждом recomposition;
- границы delay: минимум 2 сек, максимум 60 сек.

Второй шаг, только если понадобится:

```kotlin
enum class NovelAutoScrollSpeedMode { Pixel, LinesPerMinute, CharactersPerMinute }
```

Проверка:

- tests для small page, large page, empty page;
- один и тот же speed не должен давать экстремально быстрый scroll на маленьком font size;
- character-count delay не должен пересчитываться в scroll loop.

### Phase 7. WebView надежность

#### Обновить `NovelReaderWebViewBridge.kt` и `NovelReaderScreen.kt`

Запрещено: вызывать `evaluateJavascript` на каждом кадре auto-scroll loop ради `distanceToBottom`. Это performance bottleneck.

Разрешенные варианты:

1. Kotlin-first вариант:
   - использовать `WebView.onScrollChanged` или существующий callback scroll progress;
   - считать end threshold на Kotlin side из доступных scroll metrics, если они надежны;
   - применять stable frame confirmation и readiness guard.
2. JS event вариант:
   - один раз установить JS listener после готовности DOM;
   - listener слушает `scroll` с throttle, например 100-250 ms;
   - JS вызывает `@JavascriptInterface` только при входе или выходе из threshold zone;
   - Kotlin хранит последний reported `distanceToBottom` и не опрашивает DOM в frame-loop.

Пример JS должен быть event-based, а не polling-based:

```javascript
(function() {
  if (window.__ranobeAutoScrollEndWatcherInstalled) return;
  window.__ranobeAutoScrollEndWatcherInstalled = true;
  let lastSentNearEnd = null;
  let lastRun = 0;
  const thresholdPx = 120;
  window.addEventListener('scroll', function() {
    const now = Date.now();
    if (now - lastRun < 150) return;
    lastRun = now;
    const scrollTop = window.scrollY || document.documentElement.scrollTop;
    const viewport = window.innerHeight;
    const height = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
    const nearEnd = height > viewport && (height - (scrollTop + viewport)) <= thresholdPx;
    if (nearEnd !== lastSentNearEnd) {
      lastSentNearEnd = nearEnd;
      window.RanobeAutoScrollBridge && window.RanobeAutoScrollBridge.onNearEndChanged(nearEnd);
    }
  }, { passive: true });
})();
```

Kotlin side:

- учитывать `autoScrollOffset/endOffsetPx`;
- не считать конец, пока WebView не сообщил content height > viewport и загрузка завершена;
- при custom CSS/JS reflow временно сбрасывать stable end frames;
- при disabled JS fallback на `canScrollVertically(1)` плюс stable frame confirmation и задержку после load.

Проверка:

- ручной тест с WebView renderer, большим font size, custom CSS;
- performance check: в frame-loop нет `evaluateJavascript`;
- test для JS installer string, если уже есть инфраструктура.

### Phase 8. Тесты

#### Обновить `app/src/test/java/eu/kanade/presentation/reader/novel/NovelReaderUiVisibilityTest.kt`

Добавить tests:

- `auto-scroll next chapter does not depend on swipeToNextChapter`;
- `auto-scroll handoff resumes only for ContinuousReading`;
- `AdvanceAndStop opens next chapter without resuming auto-scroll`;
- `stable end detection requires multiple frames`;
- `end detection is blocked before content ready and first layout`;
- `autoScrollOffset affects end threshold`;
- `end dwell waits before advance`;
- `manual stop cancels pending ScreenModel handoff`;
- `prefetch threshold triggers once with internal 85 percent threshold`;
- `initial auto-scroll starts only from explicit ScreenModel handoff, not stale preference`;
- `WebView policy forbids per-frame javascript polling`.

#### Создать `app/src/test/java/eu/kanade/presentation/reader/novel/NovelReaderAutoScrollPolicyTest.kt`

Все pure policy tests перенести сюда, чтобы `NovelReaderUiVisibilityTest` не разрастался.

Проверка:

```bash
./gradlew :app:testDebugUnitTest --tests "eu.kanade.presentation.reader.novel.NovelReaderAutoScrollPolicyTest"
./gradlew :app:testDebugUnitTest --tests "eu.kanade.presentation.reader.novel.NovelReaderUiVisibilityTest"
```

### Phase 9. Документация и cleanup

#### Обновить или создать docs

- `docs/superpowers/plans/2026-06-23-novel-autoscroll-v2-implementation-plan.md`, если нужен task-by-task implementation plan.
- В корне уже создан этот стратегический файл: `NOVEL_AUTO_SCROLL_IMPROVEMENT_PLAN.md`.

#### Cleanup

- Удалить dead preference или dead UI для `autoScrollOffset`, если он не будет реализован.
- Убрать hardcoded English content descriptions вроде `Collapse auto-scroll`.
- Проверить `rg "swipeToNextChapter"` после изменений: auto-scroll loop не должен зависеть от этой настройки.
- Проверить `rg "contentDescription = null"` в auto-scroll компонентах.

## 6. Приоритеты реализации

### P0, исправить первым

1. Отвязать auto-scroll next chapter от `swipeToNextChapter`.
2. Реализовать continuous reading через ScreenModel-owned handoff state, не через локальный Compose state.
3. Добавить content readiness и first-layout guard перед любым EndDwell или handoff.
4. Начать использовать `autoScrollOffset` или убрать его из UI.
5. Добавить stable end detection и end dwell.
6. Добавить tests для handoff, readiness guard и end detection.

### P1, сильно улучшит UX

1. Prefetch next chapter specifically for auto-scroll с внутренним threshold 85%.
2. Overlay/countdown в конце главы, но статичный или почти статичный вариант для E-Ink.
3. Accessibility labels для FAB и панели.
4. WebView near-end detection через event-based bridge или `onScrollChanged`, без per-frame JS polling.

### P2, более крупный редизайн

1. Seamless appended next chapter для native scroll.
2. Adaptive speed по characters-per-page или lines-per-minute, WPM только после отдельной оценки стоимости.
3. Полная policy матрица для TTS, selection, manual scroll, WebView load, errors.

## 7. Acceptance criteria

Фича считается готовой для первого релиза, если:

1. Есть настройка `autoScrollChapterEndBehavior` с режимами `StopAtEnd`, `AdvanceAndStop`, `ContinuousReading`.
2. Авто-скролл может перейти на следующую главу без включенного `swipeToNextChapter`.
3. При `ContinuousReading` авто-скролл продолжается на новой главе только через ScreenModel-owned handoff и только после готовности контента.
4. При `AdvanceAndStop` следующая глава открывается, но авто-скролл не продолжается.
5. `autoScrollOffset` либо реально влияет на threshold/end spacer, либо удален/скрыт из UI.
6. End detection требует стабильного подтверждения, минимум 2 кадра для scroll modes.
7. End detection заблокирован до `isContentReady`, first layout pass и наличия renderable items.
8. Есть пауза или overlay перед переходом, пользователь может отменить. На E-Ink overlay не обновляется каждую секунду.
9. Следующая глава prefetchится до handoff с internal threshold 85%, без отдельной основной пользовательской настройки.
10. FAB и overlay имеют accessibility labels и 48dp touch targets.
11. WebView auto-scroll loop не содержит per-frame `evaluateJavascript`.
12. Unit tests покрывают policy, speed, ScreenModel handoff, readiness guard, stable end и offset.

## 8. Self-review плана

### Проверка с позиции скептического reviewer

1. Риск: план слишком большой для одного PR.
   - Исправление: разделить на PR1 policy + ScreenModel handoff + prefs + tests, PR2 runtime loop + readiness guard, PR3 overlay + accessibility, PR4 WebView event bridge, PR5 adaptive speed.

2. Риск: seamless appended chapter может конфликтовать с текущей архитектурой `NovelReaderScreenModel`, сохранением прогресса, переводами и TTS.
   - Решение: не делать seamless в P0. Сначала реализовать controlled handoff с dwell и prefetch. Seamless оставить P2 после отдельного design doc.

3. Риск: auto-scroll autostart может раздражать пользователей.
   - Решение: не включать autostart от старого `autoScroll` preference. Продолжать авто-скролл только через explicit handoff, который был создан текущей активной сессией.

4. Риск: `autoScrollOffset` неясен семантически.
   - Решение: перед кодом выбрать одну семантику и зафиксировать в UI text. Рекомендуемая семантика: `буфер конца главы в px`, при котором появляется dwell/end overlay, плюс optional bottom spacer. Если продуктово это не нужно, удалить настройку из UI.

5. Риск: WebView JS end detection может ломаться при disabled JS.
   - Решение: использовать JS helper только если JS доступен, иначе fallback на `canScrollVertically` плюс stable frame confirmation. После review добавлено ограничение: JS helper должен быть event-based или throttled, не per-frame polling через `evaluateJavascript`.

5.1. Риск: хранение handoff в Compose state не переживет смену главы.
   - Решение: handoff, target chapter id, TTL и speed для продолжения хранить в `NovelReaderScreenModel`; локальный Compose state использовать только для текущего кадра/render session.

5.2. Риск: новая глава с еще пустым layout будет ошибочно определена как конец.
   - Решение: добавить readiness guard: `isContentReady`, first layout pass и наличие renderable items обязательны до входа в `EndDwell`.

6. Риск: prefetch next chapter может расходовать трафик.
   - Решение: делать prefetch только когда `autoScrollChapterEndBehavior` равен `AdvanceAndStop` или `ContinuousReading`, и только после внутреннего threshold 85%. Не добавлять отдельную основную настройку prefetch threshold. Уважать existing cache/download policies.

7. Риск: overlay в конце главы может перекрыть последние строки.
   - Решение: вход в EndDwell должен происходить только после реального конца или после bottom spacer, не поверх непрочитанного текста. Для native scroll лучше добавить end spacer, чем показывать overlay поверх текста.

8. Риск: tests станут слишком привязаны к Compose UI.
   - Решение: максимум логики вынести в pure policy functions и тестировать без Compose. UI тестировать минимально через semantics или ручные сценарии.

### Итог self-review

План после проверки и внешнего review выглядит реализуемым, если не пытаться сразу сделать seamless infinite chapter append. Главная корректировка: P0 должен быть не про идеальный seamless reader, а про надежный ScreenModel-owned handoff state machine, readiness guard, упрощенную настройку поведения в конце главы, prefetch и end dwell. WebView-часть должна быть событийной, без покадрового JS-опроса. Seamless append лучше проектировать отдельно после стабилизации handoff.

## 9. Рекомендуемый порядок PR

1. PR1: `NovelReaderAutoScrollPolicy.kt`, ScreenModel-owned handoff, enum preference, strings, tests.
2. PR2: переписать auto-scroll loop в `NovelReaderScreen.kt`, добавить stable end detection и readiness guard.
3. PR3: prefetch integration в `NovelReaderScreenModel.kt` с internal threshold 85%.
4. PR4: end overlay, E-Ink static behavior, FAB semantics, accessibility cleanup.
5. PR5: WebView event-based near-end detection без per-frame JS polling.
6. PR6: cheap adaptive speed по character count для page reader.
7. PR7: отдельный design/implementation для seamless appended chapters.
