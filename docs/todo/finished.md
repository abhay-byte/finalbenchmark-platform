---
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
  test_status: pending — user has no devices available; branch pushed to origin, awaiting on-device validation + PR

