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
