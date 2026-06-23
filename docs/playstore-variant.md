# Plan — Finalbenchmark 2 Pro (Play Store variant)

Branch: `playstore` (at `bc2eee1`, off `main`)
Target: v1.1.0-pro · versionCode `11001`
Package: `com.zenithblue.fb2Pro`
App name: `Finalbenchmark 2 Pro`

## Decisions (confirmed)

| Q | Decision |
|---|---|
| Pro features | Same as F-Droid, branding only |
| Keystore | New `fb2pro.jks` on disk (Play App Signing upload key) |
| CI/CD | Manual AAB upload only |
| F-Droid config | Drop on `playstore` branch (kept on `main`/`v1.1.x`) |

---

## Phase 1 — Repo scaffolding (~146 Kotlin files + gradle)

1. `app/build.gradle.kts`:
   - `namespace = "com.zenithblue.fb2Pro"`
   - `applicationId = "com.zenithblue.fb2Pro"`
   - `versionName = "1.1.0-pro"`, `versionCode = 11001`
   - Add `signingConfigs.create("playstore")` → `/home/abhay/repos/keys/keystore/fb2pro.jks`
   - `release.signingConfig = signingConfigs.getByName("playstore")`
   - Remove F-Droid-specific: `dependenciesInfo` block, `apply(from = "fix-baseline-profiles.gradle")`, `isReproducibleFileOrder`/`isPreserveFileTimestamps` task config, `ignoreAssetsPattern` PNG-crunch disable
2. Move all `app/src/main/java/com/ivarna/finalbenchmark2/**` → `app/src/main/java/com/zenithblue/fb2Pro/**` (146 files)
3. Update `package` declarations in all moved files: `com.ivarna.finalbenchmark2` → `com.zenithblue.fb2Pro`
4. `strings.xml`: `app_name` → "Finalbenchmark 2 Pro"
5. `themes.xml`: rename `Theme.FinalBenchmark2` → `Theme.FB2Pro`; update `AndroidManifest.xml` refs
6. `FinalBenchmark2Application.kt` → `FB2ProApplication.kt` (file + class rename)
7. `MainActivity.kt` package decl, manifest `.MainActivity` (relative — still works)

## Phase 2 — Pro branding

8. New launcher icon set under `app/src/main/res/mipmap-*/` with Pro variant (or reuse main + add "Pro" badge later)
9. `app_name` localized strings — add en default + de/others (optional, defer to v1.2)

## Phase 3 — Play Store metadata (fastlane)

10. `fastlane/metadata/android/en-US/title.txt` → "Finalbenchmark 2 Pro"
11. `fastlane/metadata/android/en-US/short_description.txt` → Pro-focused copy
12. `fastlane/metadata/android/en-US/full_description.txt` → Pro variant copy
13. New `fastlane/metadata/android/en-US/changelogs/11001.txt` — initial Pro release notes
14. Reuse Pro-tier screenshots/feature graphic (copy from main, retitle as Pro)

## Phase 4 — Signing + release artifacts

15. Generate `fb2pro.jks` keystore at `/home/abhay/repos/keys/keystore/fb2pro.jks`
    - `keytool -genkey -v -keystore fb2pro.jks -keyalg RSA -keysize 2048 -validity 25000 -alias fb2pro`
16. Build AAB: `./gradlew :app:bundlePlaystoreRelease`
    - Or: `./gradlew :app:bundleRelease` (whichever matches buildType name)
17. Verify: `app/build/outputs/bundle/playstoreRelease/app-playstore-release.aab` exists and signs cleanly

## Phase 5 — Docs

18. `CHANGELOG.md` — add `v1.1.0-pro` entry
19. `README.md` — add Play Store badge/link alongside existing F-Droid one

---

## Pre-flight checks (run before Phase 1)

- [ ] `git status` clean on `playstore` ✓ (already confirmed)
- [ ] `playstore` is at `main` HEAD ✓
- [ ] `app/src/main/java/com/ivarna/finalbenchmark2/` has 146 `.kt` files
- [ ] `/home/abhay/repos/keys/keystore/` exists and is writable
- [ ] `keytool` available in `$PATH` (NDK env has JDK)

## Risks / open items

- Package refactor across 146 files is mechanical but error-prone — use IDE refactor or scripted `find … -exec sed` with verification
- AAB signing requires the same minSdk/targetSdk as APK — no drift
- Play App Signing will re-sign with Google's key, so the local `fb2pro.jks` is the *upload* key (not the install key)
- Pro-exclusive feature flag (`BuildConfig.IS_PRO`) is NOT needed since Pro == F-Droid feature-wise — can be added later

## Out of scope (defer)

- Play Console CI upload workflow
- In-app purchase / license verification
- Pro-only feature gates
- Localized store listing (de, es, etc.)
- AAB signing via Gradle Play Publisher plugin
