---
- id: T1
  title: Bright splash screen in dark environment
  type: bug
  priority: high
  difficulty: easy
  frequency: always
  expected: Splash honours system dark theme (no bright flash on dark devices)
  actual: Bright splash flashes before main UI on cold launch in dark theme
  reproduction: |
    1. Set system theme to dark
    2. Cold-launch the app
    3. Observe bright splash flash before main UI
  impact: AndroidManifest launch theme / windowBackground / night-qualifier in res/values-night (not sure — to confirm in dev-cycle)
  images: null
  evidence: white flash with finalbench logo
  github_ref: GH-7
plan: |
  Goal: Eliminate bright flash on cold-launch when system is in dark mode.

  Root cause:
  - values/themes.xml: Theme.FinalBenchmark2 parents android:Theme.Material.Light.NoActionBar, no windowBackground override
  - values-night/ has only colors.xml, no themes.xml — so the LIGHT theme is used in night mode too
  - Default Theme.Material.Light windowBackground = white → bright flash

  Files:
  - MODIFY app/src/main/res/values/themes.xml
    - parent unchanged: android:Theme.Material.Light.NoActionBar
    - add windowBackground/statusBarColor/navigationBarColor = @color/splash_bg
    - add windowLightStatusBar=true
  - MODIFY app/src/main/res/values/colors.xml
    - add <color name="splash_bg">#FFFBFE</color> (matches Compose light bg)
  - MODIFY app/src/main/res/values-night/colors.xml
    - add <color name="splash_bg">#FF1C1B1F</color> (matches Compose dark bg)
  - NEW app/src/main/res/values-night/themes.xml
    - parent: android:Theme.Material.NoActionBar (dark)
    - same windowBackground/statusBar/navBar refs (resolve to dark via night-colors)
    - windowLightStatusBar=false

  Approach:
  1. Use night-qualifier to swap the splash_bg color (light vs dark).
  2. Provide a night variant of the theme with a dark parent so system bars match.
  3. No Compose changes — the splash window is rendered before MainActivity onCreate.

  Edge cases:
  - minSdk 24+ — all APIs support these attrs.
  - API 30+ auto light/dark — already handled via night-qualifier.
  - No logo on splash (out of scope; pure background fix).

  Test plan:
  - Build: ./gradlew :app:assembleRelease
  - Manual: dark system + cold-launch → no bright flash; status bar icons light.
  - Manual: light system + cold-launch → no dark flash; status bar icons dark.

  Open questions:
  - Splash logo? Out of scope.
  - Theme.Material3 migration? Out of scope.

---
  test_status: merged — verified on CPH2691 (Adreno 750) + 2311DRK48I (Dimensity 9200+) in dark+light mode via screen-record; merged to v1.1.x via PR #8
- id: T5
  title: Layout broken on 4:3 / square aspect ratio devices
  type: bug
  priority: high
  difficulty: easy
  frequency: devices with high dpi / squarish aspect ratios
  expected: UI is usable on 4:3 landscape and other non-standard aspect ratios (e.g. retro handhelds)
  actual: Layout broken on 4:3 screens (Ayaneo Pocket Air Mini); UI not implemented to handle squarish aspects
  reproduction: |
    1. Install on Ayaneo Pocket Air Mini (4:3 landscape)
    2. Navigate main UI / Calibrate Power
    3. Layout clipped, overflow, or content cut off
  impact: Layout XML — wrap main + Calibrate Power screens in ScrollView; add responsive constraints (ConstraintLayout / sw resources) to handle high-DPI and squarish aspects
  followups: null
  images: null
  evidence: screenshots in GH-2 (Galaxy S24U, A50, S9, Ayaneo Pocket Air Mini)
  github_ref: GH-2
plan: |
  Goal: Make Calibrate Power UI usable on 4:3 / squarish aspect ratios AND fix Settings theme labels.

  Part A — Calibrate Power (T5):
  Root cause: PowerCalibrationScreen.kt:82-86 outer Column uses verticalArrangement = Arrangement.SpaceBetween and Modifier.fillMaxSize() with no verticalScroll. On short screens the bottom content overflows and is clipped — no way to reach it.
  Fix:
  - Add import androidx.compose.foundation.rememberScrollState
  - Add import androidx.compose.foundation.verticalScroll
  - Outer Column: add .verticalScroll(rememberScrollState())
  - Outer Column: change Modifier.fillMaxSize() → Modifier.fillMaxWidth()
  - Outer Column: change verticalArrangement = Arrangement.SpaceBetween → Arrangement.Top
  - Remove the redundant Spacer(Modifier.height(32.dp)) at line 87 (padding handles it)

  Part B — Settings theme labels (user follow-up, in this PR):
  Root cause: SettingsScreen.kt:62-63 displays "Light Monet" and "Dark Monet" — these are old Android 12+ Monet dynamic-theming labels. The current code uses ThemeMode.LIGHT / ThemeMode.DARK with Material 3 dynamic colors, so the "Monet" suffix is misleading/stale.
  Fix: rename "Light Monet" → "Light" and "Dark Monet" → "Dark" in the themes list. Indices in getThemeIndex() remain unchanged (LIGHT=0, DARK=1, ...).

  Files:
  - MODIFY app/src/main/java/com/ivarna/finalbenchmark2/ui/screens/PowerCalibrationScreen.kt
  - MODIFY app/src/main/java/com/ivarna/finalbenchmark2/ui/screens/SettingsScreen.kt

  Approach:
  1. PowerCalibrationScreen: convert outer Column to scrollable. Keep fillMaxSize on the outer Box (background brush). Only the Column needs to release height constraint.
  2. SettingsScreen: rename two labels.

  Edge cases:
  - Tall aspect (20:9 phones): scroll never engages, no visual change.
  - Squarish aspect (MediaTek set to 1080x1500 ~3:4): scroll engages, content reachable.
  - Window insets: unchanged.

  Test plan (MediaTek only — user set to 1080x1500 squarish):
  - Build: ./gradlew :app:assembleRelease
  - Install: adb install -r ... on MediaTek
  - Visual: open Settings, verify first two theme entries are "Light" / "Dark" (no "Monet").
  - Visual: open Calibrate Power, verify all content reachable (scroll to bottom).
  - Re-run with default size: verify no regressions.

  Open questions:
  - Could 4:3 benefit from a BoxWithConstraints compact-mode? Out of scope; verticalScroll is sufficient.

---
  test_status: merged — verified on MediaTek (Dimensity 9200+, 1080x1500 squarish); merged to v1.1.x via PR #9

