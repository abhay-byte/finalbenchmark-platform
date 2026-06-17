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
- id: T6
  title: Support Monochrome/Themed App Icon (Android 13+)
  type: feature
  priority: nice-to-have
  difficulty: easy
  why: Android 13+ themed icons (Material You); user wants the monochrome theme-icon feature
  really_needed: Yes, no workaround from app side
  impact: Drawables + design (new monochrome icon assets)
  followups: null
  images: null
  evidence: mockups (not yet attached)
  github_ref: GH-1
plan: |
  Goal: Enable Android 13+ Material You Themed Icons. The OS tints the
  app icon with the user's wallpaper-derived color when the user enables
  Themed Icons in launcher settings (Android 13+).

  Approach: minimal — point <monochrome> at the existing
  ic_launcher_foreground.webp. The OS applies a tint to non-transparent
  pixels: the white F2 becomes the theme color, the dark background
  pixels become the system's contrasting color. Result is a tinted
  version of the existing icon.

  Note: a proper monochrome should be a clean white silhouette on
  transparent (no background). That requires creating a vector
  approximation of the F2 logo. Tracked as a follow-up if user wants
  pixel-perfect monochrome.

  Files:
  - MODIFY app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
    - add <monochrome android:drawable="@drawable/ic_launcher_foreground"/>
  - MODIFY app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
    - same

  Edge cases:
  - Pre-Android 13: <monochrome> ignored. No change.
  - Themed Icons off: ignored. No change.
  - Themed Icons on: monochrome shown, tinted by OS.

  Test plan (MediaTek, manual by human):
  - Build: ./gradlew :app:assembleRelease
  - Install: adb install -r on MediaTek
  - User: Settings → Home screen → Themed Icons (or wallpaper style) → enable
  - Visual: home screen shows tinted app icon

  Open questions:
  - Is a follow-up vector for pixel-perfect monochrome desired? (T6b)

---
  test_status: merged — user-verified on MediaTek (Dimensity 9200+, 1080x1500); merged to v1.1.x via PR #10
- id: T7
  title: GPU benchmark suite — many inefficient benches, needs review and fixes
  type: bug
  priority: critical
  difficulty: hard
  frequency: always
  expected: GPU benches stress the GPU efficiently and accurately (not CPU-bound, not bottlenecked, consistent across vendors)
  actual: Many GPU benches are inefficient / CPU-bound / bottlenecked; inconsistent across Mali, Adreno, PowerVR
  reproduction: |
    1. Profile current GPU benchmark suite
    2. Identify the worst / most inefficient benches
    3. Run across Mali / Adreno / PowerVR devices and compare results
  impact: OpenGL ES render code, shaders, draw calls, runner orchestration, score normalization (TBD — user to confirm when picked up)
  followups: null
  images: null
  evidence: to be provided later (profiler traces, device list, FPS data)
  github_ref: null
plan: |
  Goal: Fix 10 GPU scenes where Adreno 830 < 30% faster than 750 (or slower). Target: surface ~40% hardware delta.

  Scene fixes (10 total):
  | #  | Scene              | Current               | Fix                                                |
  |----|--------------------|-----------------------|----------------------------------------------------|
  | 1  | Triangles (GLES)   | 10K tris, 1080p       | +TRI_COUNT 10K→30K; add 4K viewport                |
  | 2  | Julia/Matrix (GLES)| 128 iter, 1080p, 4×pp | +JULIA_ITER 128→256; add 4K viewport                |
  | 3  | Phong+Particles    | 5K particles, 1080p   | +P_COUNT 5K→20K; EFF-1 pre-alloc; add 4K viewport  |
  | 4  | 12-Octave FBM      | 12 octaves, 1080p     | +FBM_OCT 12→20; add 4K viewport                    |
  | 5  | Vulkan Julia       | 4K, 512 iter          | MAX_ITER 512→1024                                  |
  | 6  | Vulkan Mandelbrot  | 4K, 2048 iter         | iter 2048→4096                                     |
  | 8  | OpenCL Mem BW      | host↔device, 64 MB    | add device→device kernel, 128 MB                   |
  | 9  | OpenCL Julia       | 4K, 512 iter          | MAX_ITER 512→1024                                  |
  | 10 | OpenCL GEMM        | 1024², dispatch 64×64 | N 1024→2048, dispatch 128×128                      |
  | 12 | Super-Sample       | 4K, 64 sp, 48 Newton  | HALTON 64→256, NEWTON 48→96                        |

  Files:
  - MODIFY app/src/main/java/.../gpu/GpuBenchmarkRenderer.kt
    +TRI_COUNT=30_000, +P_COUNT=20_000
    HEAVY_4K_SCENES add: TRIANGLE_RENDERING, COMPUTE_MATRIX, PARTICLE_SYSTEM, TEXTURE_SAMPLING
    EFF-1: pre-allocate particleArray once, reuse each frame
  - MODIFY app/src/main/java/.../gpu/GpuBenchmarkShaders.kt
    COMPUTE_MATRIX_FRAG: 128→256 iter (constant)
    FBM chain: 12→20 octaves
    SUPER_SAMPLE_FRAG: Halton 64→256, Newton 48→96
  - MODIFY app/src/main/cpp/vulkan_benchmark.cpp
    SCENE_CFG: scene 0 iter 12→24 (512→1024)
    SCENE_CFG: scene 1 iter 4→8 (2048→4096)
    SCENE_CFG: scene 2 N 1024→2048, dispatch 64→128
  - MODIFY app/src/main/cpp/opencl_benchmark.cpp
    Julia kernel call: MAX_ITER 512→1024
    GEMM kernel call: N 1024→2048, dispatch sized accordingly
    Mem BW: add device→device copy path, BUF_BYTES doubled

  Approach:
  1. GLES scenes: increase workload to consume full GPU at 4K; add 4K viewport to 4 missing scenes.
  2. Native compute scenes: bump iteration counts and matrix sizes 2× to push ALU harder.
  3. Mem BW: switch to device→device to bypass DDR; both SoCs have similar LPDDR5X BW.
  4. No scoring formula changes — formula is consistent; the issue is the bottleneck.

  Edge cases:
  - 4K viewport on low-end Mali/PowerVR may drop to <5 fps (acceptable)
  - 30K triangles VBO ≈ 5.7 MB (within budget)
  - 20K particles: pre-alloc FloatArray(60K) once
  - 2048² GEMM = 48 MB VRAM; check Mali/Adreno caps (~1 GB+)
  - Mem BW device→device: two CL buffers × 128 MB = 256 MB

  Test plan:
  - Build: ./gradlew :app:assembleRelease
  - Run on 750 + 830 devices; capture FPS for all 12 scenes
  - Expected: every scene shows >30% 830/750 fps delta
  - Visual: 12 scenes render without artifacts

  Deferred:
  - Scene 12 OpenCL compute port (Phase 3, multi-week)
  - EFF-3 real GPU telemetry
  - GAP-3 new memory bandwidth test
  - Scoring formula audit

---
  test_status: parked — code on origin/1.1.x/T7-gpu-benchmark-suite-review, not yet PR'd/merged; awaiting user device validation
---
- id: T8
  title: Replace monochrome themed icon with Gemini F+needle design
  type: feature
  priority: medium
  difficulty: easy
  why: Existing ic_launcher_monochrome.xml (white-stroke outline) didn't render as expected on device; user wants the solid black italic F+needle Gemini image
  really_needed: Yes, themed-icon UX
  impact: app/src/main/res/drawable/ic_launcher_monochrome (xml removed, webp added); ic_launcher.xml + ic_launcher_round.xml references unchanged
  followups: null
  images: ~/Downloads/Gemini_Generated_Image_73kuoh73kuoh73ku.png
  github_ref: null
  plan: null
  test_status: done — built + installed on OnePlus 13R (d30a1726), fb.jks keystore (alias fb2)
---