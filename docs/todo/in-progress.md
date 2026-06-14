---
- id: T3
  title: Update CPU ranking list with 2025/2026 chips
  type: feature
  priority: high
  difficulty: easy
  why: User's device rank is wrong because newest CPUs (post-Snapdragon 8 Gen 3) are missing from the static CPU DB
  really_needed: Yes, no workaround — rank accuracy depends on it
  impact: Result page UI (rank display), and the underlying static CPU data file it reads
  followups: null
  images: null
  evidence: data collection required (pain to gather authoritative ranking source)
  github_ref: GH-4
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
