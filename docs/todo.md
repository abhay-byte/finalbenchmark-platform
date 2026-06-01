# Productivity Warmup Fix — Done

## Result

| Mode                        | Score | Video Decode     |
|-----------------------------|-------|------------------|
| Standalone (run 1)          | 99    | 93 pts · 394 fps |
| Standalone (run 2)          | 98    | 94 pts · 400 fps |
| Full Benchmark              | 95    | 95 pts · 403 fps |
| Standalone ↔ Full gap       | -3 to -4 % (was -10.6 %) | — |

## Completed

- Warmup dispatch: replaced hardcoded `800L` with `PROD_WARMUP_DUR_MS`,
  propagated `isWarmup` flag through `runTest` + 9 bench fns, tick loop
  short-circuits on completion, zero-op warmup emits `Log.w`.
- Video setup phase: warmup uses `PROD_WARMUP_KEYFRAMES=10` (was 20),
  time-boxed via `PROD_WARMUP_SETUP_FRAC=0.4`, unified deadline so total
  wall-clock ≤ `durationMs`.
- Overflow fix: `setupDeadlineMs` sentinel changed from
  `Long.MAX_VALUE + warmupStartMs` (overflow → negative → loop never
  ran) to `Long.MAX_VALUE shr 1`.
- Drain loop bounded to 100 ms wall-clock (was 1 s `dequeueOutputBuffer`
  timeout that ate the warmup window).
- Warmup constants bumped: 1000 → 2000 ms, KFs 5 → 10, frac 0.5 → 0.4.
- Build verified: `./gradlew :app:assembleRelease` → `BUILD SUCCESSFUL`,
  installed + run on `CPH2691IN`.

## Files Changed

- `app/src/main/java/com/ivarna/finalbenchmark2/ui/viewmodels/ProductivityBenchmarkViewModel.kt`

## Notes

- Remaining ~3 % gap is consistent with thermal throttling (Productivity
  is the last of 6 phases in Full mode, SoC pre-heated). Acceptable
  per the original ±5 % regression target.
- User flagged warmup is "still too fast" — left as-is per request to
  ship current state.
