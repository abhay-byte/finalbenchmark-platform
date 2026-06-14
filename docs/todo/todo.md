---
- id: T2
  title: Power Multiplier reads negative and lacks explanation
  type: bug+feature
  priority: low
  difficulty: hard
  frequency: with specific devices
  expected: Power Multiplier shows meaningful non-negative value with unit/label/tooltip explaining normal range
  actual: Reads negative on certain devices; no labels, no unit, no explanation of what the value means
  reproduction: |
    1. Run benchmark on a device that exhibits the issue
    2. Power Multiplier field shows a negative value
    3. No in-app guidance on what the value means or what range is normal
  impact: Calibrate Power page (UI labels + sensor/compute logic)
  followups: null
  images: null
  evidence: device list (not yet captured)
  github_ref: GH-5
  plan: deferred
---
- id: T4
  title: Hard crash + device reboot on MagicX One 35 during CPU benchmark
  type: bug
  priority: low
  difficulty: hard
  frequency: rare
  expected: Benchmark completes on Mid/High CPU modes without device crash
  actual: App hard-crashes and device reboots on Mid Accuracy-Fast and High Accuracy-Slow workloads
  reproduction: |
    Device: MagicX One 35 (mt6768, 8 cores, Mali-G52 MC2, Android 12 / API 31, Kernel 4.19.191, 4GB-class RAM)
    1. Install app on device
    2. Set Workload Intensity to Mid Accuracy-Fast (or High Accuracy-Slow)
    3. Start CPU benchmark
    4. App crashes; device reboots
    5. Same device runs successfully at Low Accuracy-Fastest
  impact: unknown — likely CPU workload thread pool / heap / OOM path; low-RAM device sensitivity (to confirm in dev-cycle)
  followups: null
  images: null
  evidence: none
  github_ref: GH-3
  plan: deferred
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
  plan: deferred — needs solid CPU benchmark data first (user decision 2026-06-15)
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
  plan: null
---
- id: T5b
  title: Samsung current reading shows zero (hardware-calibration only, not fixable in software)
  type: wont-do
  priority: null
  difficulty: null
  why: Current reading is zero on Samsung because each device needs hardware-based calibration (10-divisor variance); software cannot fix.
  impact: BatteryManager sensor stream; requires per-device physical calibration
  github_ref: GH-2
  plan: rejected — hardware-only
---
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
  plan: null
---
