# Coolventory

Coolventory is a native Android **offline food inventory and expiry tracker**. You manually record
what you keep in your **Fridge, Freezer, and Pantry**, and the app organizes it as an open shelf
dashboard with simple date-based statuses. It keeps a clear manual record of what is stored, what is
running low, and what should be reviewed soon.

## Main features

- Manual product records with name, storage location, category, shelf/zone, quantity, unit, added
  date, optional expiry date, optional opened date, optional consume-within-days, minimum quantity,
  Buy Again flag, and notes.
- Three storage areas — Fridge, Freezer, Pantry — each rendered as a distinct open shelf layout.
- Date-based statuses: Fresh, Review Soon, Expiry Date Passed, No Expiry Date (plus Opened-review,
  Used, Discarded).
- Quantity tracking with per-unit step buttons, a running-low indicator, and a "quantity reached
  zero" prompt.
- A local Buy Again checklist shown as paper-style slips.
- Used- and discarded-product history with a neutral, reverse-chronological activity log.
- A dedicated Expiry Review screen and in-app reminders (no push notifications, no background work).
- Local search with filters and sorting, plus simple neutral statistics.
- Everything is stored on-device with DataStore Preferences and works fully offline.

## Manual tracking disclaimer

> Coolventory is a manual food inventory organizer. Product names, quantities, dates, locations,
> statuses, and notes are entered by the user. The app does not inspect food, detect spoilage,
> guarantee food safety, or provide dietary, nutritional, or medical advice.

## Expiry disclaimer

> Date-based statuses are organizational reminders only. Always follow package instructions, storage
> guidance, and your own judgment. When in doubt about food condition or safety, discard it or
> consult an appropriate professional source.

## What Coolventory does not do

Coolventory does **not** guarantee food safety, and it does not determine whether food is safe to eat.
It provides **no dietary advice, no nutrition advice, and no medical advice**. It does **not track
calories** or macros, and it provides **no allergy guidance**. It does not classify products as
healthy or unhealthy. Date-based statuses use neutral wording ("Expiry date has passed", "Review this
product") and the app never advises you to taste or smell questionable food.

## Architecture and offline-only design

The app is a small **MVVM** project:

- **Offline-only.** There is no account, no backend, no cloud sync, no Firebase, no remote API, no
  ads, no analytics, no payments, and no internet access. The Android manifest declares **no
  permissions at all** — no INTERNET, camera, notifications, location, storage, Bluetooth, NFC, or
  alarm permissions.
- **Local storage.** All data lives in **DataStore Preferences** as serialized JSON strings, one key
  per collection: `food_products_json`, `storage_shelves_json`, `buy_again_json`,
  `product_history_json`, and `settings_json`.
- **One repository** (`CoolventoryRepository`) exposes data as Kotlin `Flow`s, performs safe
  deserialization (with item-level recovery for partially malformed JSON), merges missing settings
  with defaults, initializes default shelves exactly once, and owns every write operation.
- **One app-scoped ViewModel** (`MainViewModel`) combines the repository flows into an immutable
  `CoolventoryUiState` exposed as a `StateFlow`, and provides action methods run with coroutines.
- **Focused utility functions** compute status, quantity, shelf grouping, reminders, search, and
  statistics. They use `java.time.LocalDate`, never throw into the UI, and return nullable values
  when data is insufficient.

### Product model

`FoodProduct` holds `id`, `name`, `storageLocation`, `category`, `customCategoryName`, `shelfId`,
`quantity`, `quantityUnit`, `customQuantityUnit`, `addedDate`, `expiryDate`, `openedDate`,
`consumeWithinDaysAfterOpening`, `minimumQuantity`, `lifecycleState`, `buyAgain`, `note`, `createdAt`,
and `updatedAt`. Companion models include `StorageShelf`, `BuyAgainItem`, `ProductHistoryEvent`,
`ReminderSettings`, and `AppSettings`. Every field has a default value so older stored JSON that is
missing newer fields still deserializes.

### Fridge / Freezer / Pantry storage

- **Fridge** — an open refrigerator with default shelves (Top, Middle, Bottom, Produce Drawer, Door
  Shelf) using teal accents.
- **Freezer** — stacked drawer-style compartments (Upper, Middle, Lower, Door) with cooler blue
  accents.
- **Pantry** — vertical cabinet shelves (Upper, Eye-Level, Lower, Basket, Cabinet Door) with warm
  sand accents.

You can add custom shelves, rename them, reorder them, hide custom shelves, move products between
shelves and locations, and restore the default shelf configuration. Default shelves cannot be hidden
or permanently deleted; "Restore defaults" re-adds them while keeping your custom shelves.

### Shelf dashboard

The dashboard is the "Open Shelf Inventory" concept: a compact Fridge/Freezer/Pantry switcher, a
search action, a status rail (Fresh / Review Soon / Expiry Date Passed / Running Low counts), a large
open shelf frame with product tiles positioned on visible shelves, a Review Soon drawer, and a Buy
Again tray, with bottom navigation for Storage, Search, History, Buy Again, and Settings. Empty
shelves remain visible with an inline "Add Product" action.

## Status calculation

Statuses are computed locally from the values you enter, in this order:

1. If lifecycle is **Used** → Used.
2. If lifecycle is **Discarded** → Discarded.
3. If an effective opened-product expiry date exists and is before today → **Expired**.
4. If the opened-product expiry date is within the Soon threshold → **OpenedSoon**.
5. If there is no expiry date → **No Expiry Date**.
6. If the expiry date is before today → **Expired**.
7. If the expiry date is today or within the Soon threshold → **Soon** (Review Soon).
8. Otherwise → **Fresh**.

- **Fresh** — the expiry date is comfortably in the future.
- **Soon (Review Soon)** — the expiry date is today or within the Soon threshold (default **3 days**;
  configurable to 1, 3, 5, 7, or 14 days).
- **Expired (Expiry Date Passed)** — the entered expiry date is in the past.
- **No Expiry Date** — no date was entered; these are never counted as expired.

### Opened-product logic

If you enter an opened date and a consume-within-days value, the effective opened expiry date is
`openedDate + consumeWithinDaysAfterOpening`. When both a package expiry and an opened-product date
exist, the **earlier** date governs the reminder status. If the opened date is invalid it is ignored
(the app shows a validation warning and never crashes). Opened-product timing is based only on the
values you enter — always follow package guidance.

### Quantity tracking and Running Low

Quantities are stored as `Double` and formatted only in the UI. Increase/decrease steps depend on the
unit (e.g. Pieces = 1, Grams = 50, Kilograms = 0.1, Milliliters = 100, Liters = 0.1; Custom is manual
only). Quantity never goes negative. When quantity reaches zero, a sheet offers **Mark Used / Add to
Buy Again / Keep Active / Cancel** — nothing is marked used automatically. If you set an optional
minimum quantity, the item shows **Running Low** when the active quantity is at or below it. Running
Low is separate from expiry status and uses no purchase-urgency language.

### Mark Used / Mark Discarded / Restore workflows

- **Mark Used** — confirm a used date, optionally set a final quantity and history note, and choose
  whether to keep the Buy Again flag. The product leaves active storage and a neutral "Recorded as
  used" event is added to history. It does not imply the whole product was consumed.
- **Mark Discarded** — confirm a date, optionally pick a reason (Expiry Date Passed, Quality Concern,
  Not Needed, Storage Cleanup, Other) and a note. The event is recorded as "Discarded manually"; the
  app never determines the reason automatically and never calls an item unsafe.
- **Restore** — return a Used or Discarded item to active storage by choosing a location, shelf, and
  quantity. History is preserved and a Restored event is created.

### Buy Again

Buy Again is a local checklist of explicit `BuyAgainItem` records. You can flag active or used
products, add custom items, edit quantity, mark items purchased or unmark them, delete items, and
clear checked items after confirmation. It contains **no store links, prices, recommendations, or
delivery services** — it is styled as paper slips, not a storefront.

### Search and filters

Local search matches product name, category, shelf name, and note text. Filters include the three
locations, Fresh / Soon / Expired / No Expiry Date / Running Low / Buy Again / Used / Discarded, and
category. Sorting options include nearest expiry, most overdue, recently added, recently updated,
name, quantity low-to-high, storage location, and shelf. No query ever leaves the device.

### Product history

History is a reverse-chronological list of explicit events (Created, Updated, QuantityChanged, Moved,
MarkedUsed, MarkedDiscarded, Restored, AddedToBuyAgain, RemovedFromBuyAgain). It is grouped by date
with Used / Discarded / All Activity tabs and uses neutral statistics only — no food-waste judgments.

### Expiry Review screen

A dedicated screen with Expired, Due Today, Review Soon, Opened-Product Review, and No Expiry Date
sections. It offers "Review Product" (open) actions and never recommends consumption.

### In-app reminders

Reminders are **in-app only** and are evaluated when the app opens, when the Storage screen becomes
active, when the Expiry Review opens, and when products change. They surface as a banner ("N products
need review.") with **Review** and **Not Now** actions. There are **no push notifications, no
POST_NOTIFICATIONS permission, no WorkManager, no AlarmManager, no exact alarms, no background
services, and no background polling.**

## Privacy

> Coolventory stores product names, storage locations, shelves, quantities, dates, statuses, notes,
> Buy Again items, history, and settings locally on this device. The app has no account, no cloud
> sync, no internet access, no ads, no analytics, no payments, no camera access, no barcode scanner,
> no nutrition service, and no background monitoring.

There is **no barcode scanner, no camera, no receipt scanning, and no online product search** — every
value is entered manually.

## Visual concept and layout uniqueness

The visual style is "Cool Shelf Cabinet": calm, organized, household-focused. Instead of the common
mascot → title → stats card → button stack layout, Coolventory uses a **shelf-based spatial layout**
made of shelf rows, product containers, expiry labels, quantity tags, drawer sections, status chips,
Buy Again slips, and history rows. The refrigerator, freezer, and pantry are drawn entirely with
Compose layouts, shapes, borders, and simple vector art — there is **no fridge photograph and no
external illustration assets**.

### App icon

A custom adaptive icon: a frost/teal background with a simplified open refrigerator, two visible
shelves, three abstract food containers, and one small amber "expiry" dot. No brand packaging, no
barcode, no camera, no shopping cart, no calorie symbol, and no text. The icon is defined as an
`adaptive-icon` (API 26+) with a vector fallback for API 24–25.

### Splash screen

A stable static splash (via `androidx.core:core-splashscreen`): a pale/frost background, a centered
open-refrigerator vector, subtle teal shelf lines, and one small amber date marker. No photography,
no scanner visual, and no heavy animation.

## Technology stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Android ViewModel, Kotlin Coroutines, Kotlin
Flow, DataStore Preferences, Kotlinx Serialization, and Gradle Kotlin DSL. No networking, Firebase,
camera, barcode, OCR, ML, chart, image-loading, or dependency-injection libraries are used.

## Building

### Open in Android Studio

1. Open the `Coolventory` folder in Android Studio (a recent stable version).
2. Let Gradle sync. **JDK 17** is required (the project targets Java 17 and uses core-library
   desugaring for `java.time`).
3. Select the `app` configuration and run on a device or emulator.

> **Gradle wrapper jar.** This repository ships `gradlew`, `gradlew.bat`, and
> `gradle/wrapper/gradle-wrapper.properties`, but not the binary `gradle/wrapper/gradle-wrapper.jar`.
> Android Studio regenerates it automatically on first sync. From the command line, run
> `gradle wrapper --gradle-version 8.9` once (requires a local Gradle) to materialize it. The CI
> workflow generates it automatically before building.

### Android configuration

- `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`.
- Android Gradle Plugin 8.6.x, Kotlin 2.0.x, Compose compiler via the Kotlin Compose plugin.
- Portrait-locked, edge-to-edge with visible system bars, correct Android Back handling.
- **16 KB page-size compatibility:** the app ships no native third-party binaries (pure
  Kotlin/Compose/DataStore), so 16 KB memory-page devices are supported. Still verify the final
  release bundle.

### Debug build

```
./gradlew :app:assembleDebug
```

### Unit tests

```
./gradlew testReleaseUnitTest
```

Tests cover status calculation, opened-product logic, quantity math, shelf grouping, filtering and
sorting, reminder evaluation, default-shelf initialization (and no duplication), JSON round-trips,
corrupted-JSON item-level recovery, and repository operations (add/mark used/mark discarded/restore/
Buy Again/reset).

## Release build (staged R8)

Follow the staged rollout:

1. **First, validate a non-minified release.** In `app/build.gradle.kts`, temporarily set both
   `isMinifyEnabled = false` and `isShrinkResources = false` in `buildTypes.release`. Build, install,
   launch, and test on a device.
2. **Then enable R8 and resource shrinking** (the shipped configuration): `isMinifyEnabled = true`
   and `isShrinkResources = true`, with
   `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
   Rebuild, reinstall, and re-test Kotlinx Serialization, DataStore, navigation, shelf rendering,
   status calculation, and history. The provided `proguard-rules.pro` keeps kotlinx.serialization
   serializers and the Coolventory model classes.

### PKCS12 keystore generation

```
keytool -genkeypair -v -storetype PKCS12 -keystore coolventory-release-key.p12 -alias coolventory_key -keyalg RSA -keysize 2048 -validity 10000
```

### Local signing setup

Release builds are **never** signed with the debug key. Signing values are read from environment
variables first (used by CI) and then from a git-ignored `keystore.properties` at the project root:

```
storeFile=/absolute/path/to/coolventory-release-key.p12
storePassword=your_store_password
keyAlias=coolventory_key
keyPassword=your_key_password
```

If no complete set of credentials is found, the release signing config is not created and the build
does not silently fall back to the debug key. Never commit the PKCS12 file, passwords, decoded
keystore, or `keystore.properties` (all are git-ignored).

### Build signed artifacts locally

```
./gradlew :app:assembleRelease   # signed release APK  -> app/build/outputs/apk/release/
./gradlew :app:bundleRelease     # signed release AAB  -> app/build/outputs/bundle/release/
```

### Local release verification

```
apksigner verify --print-certs app-release.apk
adb install -r app-release.apk
adb logcat
```

The signing certificate must **not** contain `CN=Android Debug`. Use the **APK** for local
installation and verification; upload **only the `.aab`** to Google Play.

## GitHub Actions

`.github/workflows/android-build.yml` runs on push to `main` and via manual dispatch. It checks out
the repo, sets up JDK 17, installs Android SDK Platform 35 and Build Tools 35.0.0, restores the Gradle
cache, decodes `ANDROID_KEYSTORE_BASE64` into a temporary PKCS12 file, exposes signing values only as
environment variables, builds the signed release APK and AAB, locates the APK, runs
`apksigner verify --print-certs`, fails if verification fails or if the certificate contains
`CN=Android Debug`, and uploads the signed APK (test artifact) and signed AAB (Play artifact). It
never prints passwords or Base64 values. CI proves compilation, signing, certificate verification,
and artifact generation — it is **not** proof that the app launches.

### Required GitHub secrets

- `ANDROID_KEYSTORE_BASE64` — base64 of the PKCS12 keystore.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD` (use the same value as the store password unless separate values are
  configured reliably).

## Permissions

Coolventory requests **no runtime permissions** and its manifest declares none. There are no
permission dialogs, no scanner actions, and no nutrition or calorie fields anywhere in the app.

## Data reset behavior

Settings provides granular deletion (used history, discarded history, all active products) and a full
**Reset all local data**, which permanently removes every product, quantity, date, shelf, note, Buy
Again item, history record, and setting stored on the device, then re-seeds the default shelves. All
destructive actions require explicit confirmation.

## Local functional test checklist

Verify (at minimum): first launch with empty storage; onboarding and skip; default shelves; switching
Fridge/Freezer/Pantry; adding products to each location; custom shelf add/rename/reorder/hide; moving
products between shelves and locations; product without expiry date; Fresh / Soon / due-today /
expired historical products; changing the Soon threshold; opened date + consume-within-days and the
effective review date; quantities in pieces/grams/liters/custom; increase/decrease; blocked negative
quantity; minimum quantity and Running Low; reducing to zero and keeping active; mark used; mark
discarded; restore used and discarded; Buy Again add/custom/check/clear; search by name and category;
location and status filters; sort by nearest expiry and most overdue; Expiry Review; used, discarded,
and all-activity history; triggering and dismissing in-app reminders; disabling reminders; deleting
products and history; restore default shelves; reset all local data; relaunch; airplane mode (all
features remain available); and confirm no INTERNET/camera permission, no scanner action, no runtime
permission dialogs, and no nutrition/calorie fields. Inspect `adb logcat` for serialization,
DataStore, navigation, `LocalDate`, missing-product/shelf, quantity-parsing, R8, or signing issues,
and confirm no duplicate default shelves after relaunch.

## Manual-entry limitations

Coolventory only knows what you tell it. It cannot read products or expiry dates automatically, does
not detect spoilage or contamination, does not connect to any refrigerator or sensor, and its
date-based statuses are organizational reminders — not food safety assessments.
