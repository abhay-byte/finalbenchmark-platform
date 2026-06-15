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
