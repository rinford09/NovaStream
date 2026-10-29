# Changelog

All notable changes to the CloudStream customized project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [4.8.0-jam-1.2.0] - 2026-10-28

### Milestone: Configurable Batch Resolve Delay & IP Anti-Blocking Protection
* **Git Tag**: `v4.8.0-jam-1.2.0`
* **Release APK**: [CloudStream-v4.8.0-jam-1.2.0.apk](https://github.com/rinford09/cloudstream-jam/releases/tag/v4.8.0-jam-1.2.0)

### Added
- **Configurable Batch Episode Resolve Delay Setting**:
  - Added new `Batch Episode Resolve Delay` slider (0–10 seconds, default: 1s) under **Settings → Downloads** for both Mobile/Compose (`SettingsGeneralScreen`) and Android TV / XML (`settings_general.xml`).
  - Full Arabic and English localization strings.
  - Allows users to fine-tune the delay between resolving consecutive episodes (0s = instant / concurrent, 1s–10s = gentle rate-limiting) to prevent IP rate-limiting, temporary bans, and HTTP 403 Forbidden / Access Denied errors across all shows and providers.
- **Sequential Rate-Limited Batch Resolver Engine**:
  - When `intervalSec > 0`, `batchDownloadViaAdm` resolves episodes strictly sequentially with the configured delay between requests and between retries.
  - Dynamic progress dialog informs the user of the ongoing interval spacing.
  - When `intervalSec == 0`, resumes maximum-speed concurrent extraction respecting the `parallelDownloads` preference.

---

## [4.8.0-jam-1.1.1] - 2026-10-28

### Milestone: AB Download Manager Header & Referer Preservation (Access Denied Fix)
* **Git Tag**: `v4.8.0-jam-1.1.1`
* **Release APK**: [CloudStream-v4.8.0-jam-1.1.1.apk](https://github.com/rinford09/cloudstream-jam/releases/tag/v4.8.0-jam-1.1.1)

### Fixed
- **AB Download Manager Access Denied (HTTP 403) Resolution**:
  - Formatted links passed via `ACTION_SEND` into curl commands recognized by AB Download Manager's `DownloadCredentialsFromCurl` extractor (`curl "<url>" -H "User-Agent: ..." -H "Referer: ..."`).
  - Attached per-episode `Referer` and browser `User-Agent` headers in batch and single downloads, eliminating intermittent `HTTP 403 Forbidden` / `Access Denied` errors on strict file hosts (such as Pixeldrain).
  - Maintained raw link export to clipboard as clean backup URLs.

---

## [4.8.0-jam-1.1.0] - 2026-10-28

### Milestone: AB Download Manager Full Integration & Support
* **Git Tag**: `v4.8.0-jam-1.1.0`
* **Release APK**: [CloudStream-v4.8.0-jam-1.1.0.apk](https://github.com/rinford09/cloudstream-jam/releases/tag/v4.8.0-jam-1.1.0)

### Added
- **AB Download Manager Integration (`com.abdownloadmanager`)**:
  - Full support for open-source **AB Download Manager** as an external download provider.
  - **Single Episode Downloads**: Direct link intent routing to AB Download Manager with stream URL, headers, and pre-populated metadata.
  - **Batch Episode Downloads**: Multi-episode queueing and selection support sent cleanly via `Intent.ACTION_SEND` multi-URL batch format.
  - **Settings Selection**: Added AB Download Manager as a selectable default download manager option in both Phone/Compose settings (`SettingsGeneralScreen`) and Android TV settings (`SettingsGeneral`).
  - **Always Ask / Action Sheets**: Shows "Download via AB Download Manager" option in episode download menus and mirror link pickers.
  - **Direct Link Dialog**: Added direct launch button to trigger AB Download Manager from the direct link inspection dialog.
  - **Arabic Localization**: Full Arabic translation strings (`تحميل عبر AB Download Manager`, `لم يتم تثبيت AB Download Manager`, etc.).

---

## [4.8.0-jam-1.0.2] - 2026-10-28

### Milestone: Compose Extensions Direct Repository Loading Fix
* **Git Tag**: `v4.8.0-jam-1.0.2`
* **Release APK**: [CloudStream-v4.8.0-jam-1.0.2.apk](https://github.com/rinford09/cloudstream-jam/releases/tag/v4.8.0-jam-1.0.2)

### Fixed
- Fixed `AndroidRepositoryManager.getPlugins` to use universal `parseRepository(url)`, allowing Compose Extensions screen to load direct `plugins.json` URLs as well as standard `repo.json` URLs.

---

## [4.8.0-jam-1.0.1] - 2026-10-28

### Milestone: Universal Repository Parser Support
* **Git Tag**: `v4.8.0-jam-1.0.1`

### Added
- **Universal Repository URL Parser in `RepositoryManager`**:
  - Automatically accepts direct `plugins.json` URLs (wrapping them into a Repository entry).
  - Automatically falls back to GitHub repository branch lookups (`builds/repo.json`, `main/repo.json`, `builds/plugins.json`) when a repository home link is entered.
  - Prevents "nothing found" errors when adding raw extension links.

---

## [4.8.0-jam-1.0.0] - 2026-10-28

### Milestone: Interactive Anime Filters & ADM Batch Download Support
* **Git Tag**: `v4.8.0-jam-1.0.0`
* **Release Name**: `CloudStream Pre-release (with Anime Filters & ADM Fix)`
* **Published APK**: [cloudstream-prerelease.apk](https://github.com/rinford09/cloudstream-jam/releases/tag/pre-release)

### Added
- **Visual Anime Filter Dialog (`تصنيف بواسطة ؟` / Filter by?)**:
  - Full bottom-sheet dialog accessible directly from the search bar via the filter icon (`@drawable/filter_alt_24px`).
  - Toggleable multi-select chips across 7 filter categories:
    - **Status (`الحالة`)**: `مستمر` (Ongoing), `مكتمل` (Completed).
    - **Type (`النوع`)**: `مسلسل` (TV Series), `فيلم` (Movie), `أونا` (ONA), `أوفا` (OVA), `حلقة خاصة` (Special).
    - **Season (`الموسم`)**: `خريف` (Fall), `شتاء` (Winter), `ربيع` (Spring), `صيف` (Summer).
    - **Age Rating (`الفئة العمرية`)**: `+13`, `+17`, `+18`, `جميع الأعمار`.
    - **Year (`السنة`)**: Expandable list from `2026` down to `1970`.
    - **Studio (`الاستديو`)**: Expandable list featuring major studios (`MAPPA`, `Toei Animation`, `Madhouse`, `ufotable`, `Bones`, `A-1 Pictures`, `Wit Studio`, `CloverWorks`, etc.).
    - **Genres (`التصنيفات`)**: Expandable grid with 40+ genres (`اكشن`, `مغامرات`, `كوميدي`, `دراما`, `غموض`, `سحر`, `خارق للطبيعة`, `شياطين`, `رعب`, etc.).
- **Dynamic Badge Counter**:
  - Live count badge on the search filter icon indicating the number of currently active chips.
  - Automatically updates when filters are added, removed, or typed directly.
- **Bidirectional Token Sync**:
  - Two-way binding between typed search tokens (`tag:`, `genre:`, `studio:`, `year:`, `state:`, `season:`, `age:`) and the dialog chips.
  - Auto-expands collapsed accordion sections if any of their contained chips are active.
  - Preserves freeform anime titles (e.g. `One Piece state:مستمر`).
  - One-click `Reset` action clears all active chips and search tokens.
- **CI/CD Automated Build Workflow**:
  - Added `.github/workflows/prerelease.yml` supporting pushes to `main` and manual triggers (`workflow_dispatch`).
  - Automatically builds signed release APK using GitHub Actions and publishes to GitHub Releases.

### Fixed
- **ADM Direct Batch Download**:
  - Resolved episode batch download failures where links could not be loaded sequentially in bulk.
  - Added direct extraction passing to ADM (Advanced Download Manager) to ensure all selected episodes download smoothly.
- **Build & Signing Fallback**:
  - Added debug keystore fallback configuration in `app/build.gradle.kts` so release builds are always signed and installable out of the box on any device without missing upstream private keys.

---

### How to Roll Back to this Version:
To restore or inspect the repository exactly at this milestone:
```bash
git checkout v4.8.0-jam-1.0.0
```
To return to the latest development branch:
```bash
git checkout main
```
