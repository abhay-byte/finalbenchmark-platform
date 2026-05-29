---
description: How to prepare and publish a new FinalBenchmark release
---

# Release Workflow

## HARD RULE — Commit Identity

```
APK commit == tag commit == release commit == HEAD
```

All four MUST resolve to the same hash. No drift, no gap. If any is different, delete the tag/release and redo from the correct commit.

## Prerequisites

- Version code and version name updated in `app/build.gradle.kts`
- Version text updated in `SettingsScreen.kt`
- Tree must be clean — all changes committed and pushed BEFORE building

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

Confirm the APK was built from the current HEAD. Both must resolve to the same hash.

```bash
HEAD=$(git log -1 --format="%H")
echo "HEAD: $HEAD"
cat app/build/outputs/apk/release/output-metadata.json | grep version
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

### 8. Verify Release Commit Match — INVARIANT CHECK

All four MUST match. Run this exact check:

```bash
TAG_COMMIT=$(git rev-list -1 v<VERSION_NAME>)
HEAD_COMMIT=$(git log -1 --format="%H")
RELEASE_COMMIT=$(gh release view v<VERSION_NAME> --repo abhay-byte/finalbenchmark-platform --json targetCommitish -q '.targetCommitish')

echo "TAG:     $TAG_COMMIT"
echo "HEAD:    $HEAD_COMMIT"
echo "RELEASE: $RELEASE_COMMIT"

[ "$TAG_COMMIT" = "$HEAD_COMMIT" ] && [ "$TAG_COMMIT" = "$RELEASE_COMMIT" ] \
  && echo "OK: all match" \
  || echo "FAIL: mismatch — delete tag and release, redo from correct commit"
```

Also verify APK from release matches local:

```bash
curl -sL -o /tmp/verify.apk "https://github.com/abhay-byte/finalbenchmark-platform/releases/download/v<VERSION_NAME>/app-release.apk"
sha256sum /tmp/verify.apk app/build/outputs/apk/release/app-release.apk
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

### APK not matching commit (invariant broken)
```bash
# Delete broken release
gh release delete v<VERSION_NAME> --yes
# Rebuild from HEAD
./gradlew clean assembleRelease
# Verify HEAD == APK build commit
git log -1 --format="%H"
# Re-tag HEAD
git tag -d v<VERSION_NAME>
git tag -a v<VERSION_NAME> -m "Release v<VERSION_NAME>"
git push origin v<VERSION_NAME> --force
# Re-create release
gh release create v<VERSION_NAME> --title "v<VERSION_NAME>" \
  --notes-file app/release/release-<VERSION>.md \
  app/build/outputs/apk/release/app-release.apk
# Re-run invariant check (step 8)
```

### Tag needs update
```bash
git tag -d v<VERSION_NAME>
git tag -a v<VERSION_NAME> -m "Release v<VERSION_NAME>"
git push origin v<VERSION_NAME> --force
```
