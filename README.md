# GCash Agent Tracker

A polished, fully-offline Android app for GCash remittance agents in the
Philippines to record daily transactions per GCash number and export
daily / date-range reports to Excel (`.xlsx`).

> **Agent business rule (the app's core logic):**
> When the **agent sends** money it is counted as **CASH IN**; when the
> **agent receives** money it is **CASH OUT**. This is the agent's
> perspective, not standard accounting.

## Features (MVP)

- **GCash number management** — enroll, edit and delete numbers, each with a
  label/alias (e.g. "Main", "Store 2"). PH number validation + uniqueness.
- **Transaction entry** — record date/time, send/receive, amount, from/to
  number, reference number, and an **optional** screenshot. Each transaction is
  auto-assigned to the selected GCash number. Live "Cash In / Cash Out" preview.
- **Dashboard** — per-number cards showing today's Cash In, Cash Out and Net.
- **Reports** — per-number **and** a combined "All Numbers" summary, with
  quick ranges (Today / This Week / This Month / Custom). Shows total Cash In,
  total Cash Out, **Net (In − Out)** and transaction count, plus the full list.
- **Excel export** — generates a real `.xlsx` via a tiny built-in OOXML writer
  (no Apache POI, no heavy dependencies) and shares it through the system share
  sheet. Works entirely offline.

All money is Philippine Peso, formatted `₱1,234.50`. Dates use PH locale and the
Asia/Manila time zone (e.g. `Jun 12, 2026 · 4:32 PM`).

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM + Repository** pattern, manual DI (`AppContainer`)
- **Room** for local storage (money stored as integer centavos)
- **Navigation Compose**
- **Coil** for screenshot thumbnails
- Min SDK **26** (Android 8.0), target/compile SDK 35

## Project structure

```
app/src/main/java/com/gcashagent/tracker/
  core/
    data/local/        Room entities, DAOs, database, converters
    data/repository/   GCashRepository (interface) + impl + mappers
    domain/model/      TransactionType (cashFlow rule), Transaction, ReportSummary, ...
    util/              PesoFormatter, PhDateTime, DateRange, ImageStore, ExcelWriter, ReportExporter
  di/                  AppContainer (manual DI)
  ui/
    theme/             Material 3 theme (GCash blue)
    navigation/        Routes + NavGraph
    components/        CashFlowChip, SummaryCard, EmptyState
    feature/numbers/        dashboard + add/edit sheet
    feature/transactions/   list + add/edit entry
    feature/report/         filters, summary, Excel export
```

## Build & run

```bash
./gradlew assembleDebug      # build the APK
./gradlew installDebug       # install on a connected device/emulator
./gradlew testDebugUnitTest  # run JVM unit tests
```

Requires the Android SDK (`local.properties` with `sdk.dir`, or `ANDROID_HOME`).

## Tests

JVM unit tests cover the logic that matters most and pass green:

- `CashFlowRuleTest` — the send→Cash In / receive→Cash Out rule
- `PesoFormatterTest` — `₱` formatting, comma grouping, parsing to centavos
- `ReportSummaryTest` — Cash In / Cash Out / Net / count aggregation
- `ExcelWriterTest` — the `.xlsx` writer produces a valid zip of well-formed XML

## Roadmap (not in this MVP)

Cloud backup, multi-device sync, subscription model, screenshot OCR/auto-extract,
running float/balance ledger. The architecture (repository interface, centavo
money, feature-based modules) is kept clean so these can be added later.
