---
description: How to prepare and publish a new FinalBenchmark release
---

# Release Workflow

## Prerequisites

- Version code and version name updated in `app/build.gradle.kts`
- Version text updated in `SettingsScreen.kt`

## Steps

### 1. Clean Working Tree

Ensure no uncommitted or untracked files. If any exist, stage, commit, and push.

```bash
git status
# If dirty:
git add -A
git commit -m "chore: cleanup before release v<VERSION_NAME>"
git push origin main
```

### 2. Build Release APK

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

### 3. Verify APK Commit Match

Confirm the APK was built from the latest commit. Check `output-metadata.json` or compare timestamps.

```bash
git log -1 --format="%H"
ls -l app/build/outputs/apk/release/app-release.apk
```

### 4. Create Release MD File

If `app/release/release-<VERSION>.md` does not exist, create it. Base it on the previous release format and list all new features/fixes since the last tag.

```bash
ls app/release/release-*.md
git log <PREV_TAG>..HEAD --oneline  # gather changelog content
```

Format: headings for each new benchmark category, bullet points for features/fixes.

### 5. Create Changelog TXT (F-Droid)

Create `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt`. Point-wise, each line under 100 characters. List only new benchmark categories introduced (not CPU).

```bash
ls fastlane/metadata/android/en-US/changelogs/
```

### 6. Commit and Push Release Artifacts

```bash
git add app/release/ fastlane/metadata/android/en-US/changelogs/
git commit -m "release: v<VERSION_NAME> — changelog and release notes"
git push origin main
```

### 7. Tag and Create GitHub Release

```bash
git log -1 --format="%H"  # copy commit hash

# Tag
git tag -a v<VERSION_NAME> -m "Release v<VERSION_NAME>"
git push origin v<VERSION_NAME>

# GitHub Release with APK attached
gh release create v<VERSION_NAME> \
  --title "v<VERSION_NAME>" \
  --notes-file app/release/release-<VERSION>.md \
  app/build/outputs/apk/release/app-release.apk
```

### 8. Verify Release Commit Match

Confirm the GitHub release tag points to the same commit that built the APK.

```bash
git rev-list -1 v<VERSION_NAME>
gh release view v<VERSION_NAME> --json tagName,targetCommitish
```

### 9. F-Droid (Auto)

F-Droid metadata uses `UpdateCheckMode: Tags` — new tags are auto-picked up. No manual metadata update needed unless metadata fields change.

---

## Quick Reference

| Artifact | Path |
|----------|------|
| APK | `app/build/outputs/apk/release/app-release.apk` |
| Release notes | `app/release/release-<VERSION>.md` |
| Changelog | `fastlane/metadata/android/en-US/changelogs/<CODE>.txt` |
| Metadata | `com.ivarna.finalbenchmark2.yml` |

## Troubleshooting

### Dirty working tree before build
```bash
git stash  # or commit changes
```

### APK not matching commit
Rebuild: `./gradlew clean assembleRelease`

### Tag needs update
```bash
git tag -d v<VERSION_NAME>
git tag -a v<VERSION_NAME> -m "Release v<VERSION_NAME>"
git push origin v<VERSION_NAME> --force
```
