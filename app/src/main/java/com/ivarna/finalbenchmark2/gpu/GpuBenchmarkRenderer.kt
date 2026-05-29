package com.ivarna.finalbenchmark2.gpu

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

/** All GPU benchmark scenes: OpenGL ES + Vulkan Compute + OpenCL Compute. */
enum class GpuScene {
    // ── OpenGL ES 3.x Graphics ────────────────────────────────────────────
    TRIANGLE_RENDERING,
    COMPUTE_MATRIX,
    PARTICLE_SYSTEM,
    TEXTURE_SAMPLING,
    WIREFRAME_MESH,
    MANDELBROT_DEEP,
    PHONG_MULTI_LIGHT,
    RAY_MARCH_SDF,
    DOMAIN_WARP,
    SUPER_SAMPLE,
    // ── OpenGL ES Extended stress scenes ──────────────────────────────────
    SHADER_COMPILE,
    MEM_BANDWIDTH,
    MSAA_4X,
    VRAM_PRESSURE,
    GEOMETRY_ALU_SATURATION,
    MULTI_PASS_BLOOM,
    // ── Vulkan 1.1+ Compute ───────────────────────────────────────────────
    VULKAN_JULIA_COMPUTE,
    VULKAN_MANDELBROT_COMPUTE,
    VULKAN_GEMM_COMPUTE,
    VULKAN_N_BODY_COMPUTE,
    // ── OpenCL Compute ────────────────────────────────────────────────────
    OPENCL_MEM_BW,
    OPENCL_JULIA_COMPUTE,
    OPENCL_GEMM_COMPUTE,
    OPENCL_N_BODY_COMPUTE,
}

/**
 * OpenGL ES 3.0 renderer driving all 10 GPU benchmark scenes.
 *
 * Timing: glFinish() is called after every draw call and wall-clock time is measured between
 * render start and GPU completion. This gives *uncapped* GPU render-time (not vsync-gated)
 * so heavy scenes correctly report FPS below 60.
 *
 * @param onFrameMetrics Called on the GL thread with (effectiveFps, gpuRenderTimeMs).
 * @param onGpuInfo      Called once on surface creation with (gpuRenderer, glVersion).
 */
class GpuBenchmarkRenderer(
    private val onFrameMetrics: (fps: Float, renderMs: Float) -> Unit,
    private val onGpuInfo: (renderer: String, version: String) -> Unit = { _, _ -> }
) : GLSurfaceView.Renderer {

    @Volatile var currentScene: GpuScene = GpuScene.TRIANGLE_RENDERING

    private var vpW = 1; private var vpH = 1
    private var startTimeMs = 0L

    // Program handles
    private var progTriangle    = 0; private var progCompute     = 0
    private var progParticle    = 0; private var progTexture     = 0
    private var progMesh        = 0; private var progMandelbrot  = 0
    private var progMultiLight  = 0; private var progRayMarch    = 0
    private var progDomainWarp  = 0; private var progSuperSample = 0
    private var progDisplay     = 0
    private var uTexDisplay     = -1
    private var aPosDisplay     = -1

    // Scene 1 - triangles
    private val TRI_COUNT = 10_000
    private lateinit var triBuf: FloatBuffer
    private var triVertCount = 0

    // Scene 3 - particles
    private val P_COUNT = 5_000   // reduced: physics runs on GL thread, keep CPU cost minimal
    private val pX    = FloatArray(P_COUNT); private val pY    = FloatArray(P_COUNT)
    private val pVx   = FloatArray(P_COUNT); private val pVy   = FloatArray(P_COUNT)
    private val pLife = FloatArray(P_COUNT)
    private lateinit var particleBuf: FloatBuffer
    private var lastParticleNs = 0L

    // Scene 2,4,6,7,8,9,10 - fullscreen quad
    private lateinit var quadBuf: FloatBuffer
    private val QUAD = floatArrayOf(-1f,-1f, 1f,-1f, -1f,1f, 1f,-1f, 1f,1f, -1f,1f)

    // Scene 5 - dense mesh
    private val GRID = 250
    private lateinit var meshVerts: FloatBuffer
    private lateinit var meshIdx:   ShortBuffer
    private var meshIdxCount = 0

    // Cached locations for optimization (BUG-2)
    private var uTimeTriangle = -1
    private var uTimeParticle = -1
    private var uTimeMesh = -1
    private var uAspectMesh = -1

    private var aLocalTriangle = -1
    private var aOrbitRTriangle = -1
    private var aOrbitPhTriangle = -1
    private var aOrbitSpdTriangle = -1
    private var aRotSpdTriangle = -1
    private var aColorTriangle = -1

    private var aPosParticle = -1
    private var aLifeParticle = -1

    private var aGridMesh = -1

    private class ProgramLocations(
        val aPos: Int,
        val uTime: Int,
        val uAspect: Int
    )
    private val fullProgramLocs = HashMap<Int, ProgramLocations>()

    // Reusable array for particles to avoid GC pressure (EFF-1)
    private val particleArray = FloatArray(P_COUNT * 3)

    // Timer query properties (GAP-1a)
    private var supportsTimerQuery = false
    private var timerQueryId = 0

    // ── GAP scene program handles ──────────────────────────────────────────
    private var progMemBw       = 0  // GAP-3
    private var progVramPressure = 0  // GAP-5
    private var progMsaaTest    = 0  // GAP-4
    private var progBloomHoriz  = 0  // GAP-7 pass 1
    private var progBloomVert   = 0  // GAP-7 pass 2
    private var progTessBase    = 0  // GAP-6 fallback
    private var progShaderTiming = 0 // GAP-2 result display

    // GAP-3: mem bandwidth texture (1024×1024)
    private var texMemBw = 0
    // GAP-5: 8 VRAM pressure textures (512×512 each)
    private val texVram = IntArray(8)
    // GAP-4: MSAA FBO + resolve FBO
    private var fboMsaa = 0; private var rboMsaaColor = 0; private var rboMsaaDepth = 0
    private var fboMsaaResolve = 0; private var texMsaaResolve = 0
    // GAP-7: bloom FBO pair
    private var fboBloom0 = 0; private var texBloom0 = 0
    private var fboBloom1 = 0; private var texBloom1 = 0
    // GAP-6: tessellation patch VBO
    private var tessVao = 0; private var tessVbo = 0; private var supportsTess = false
    // GAP-2: shader compile timing (ms, smoothed)
    private var shaderCompileMs = 0f
    // Sampler/uniform caches for GAP programs
    private var uTexMemBw = -1
    private var uTimeMemBw = -1
    private var aPosMemBw = -1
    private var uTimeVram = -1
    private var aPosVramPressure = -1
    private val uTexVram = IntArray(8)
    private var uTimeMsaa = -1
    private var uTexBloomH = -1; private var uTimeBloomH = -1
    private var uTexBloomV = -1; private var uTimeBloomV = -1
    private var uTimeTess = -1; private var aPosPlain = -1
    private var uTimeShaderTiming = -1
    private var aPosMsaa = -1; private var aPosBloom0 = -1; private var aPosBloom1 = -1
    private var aPosTess = -1; private var aPosShaderT = -1

    companion object {
        private val HEAVY_4K_SCENES = setOf(
            GpuScene.MANDELBROT_DEEP, GpuScene.PHONG_MULTI_LIGHT,
            GpuScene.RAY_MARCH_SDF,   GpuScene.DOMAIN_WARP,
            GpuScene.SUPER_SAMPLE
        )
    }

    // -------------------------------------------------------------------------
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.07f, 0.07f, 0.10f, 1f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        startTimeMs = System.currentTimeMillis()

        // 1. Log GLSL version, check fragment highp support, and report GPU info (BUG-5)
        try {
            val gpuRenderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown GPU"
            val glVersion   = GLES20.glGetString(GLES20.GL_VERSION)  ?: "Unknown"
            val glslVersion = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION) ?: "Unknown"
            val range = IntArray(2)
            val precision = IntArray(1)
            GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER, GLES20.GL_HIGH_FLOAT, range, 0, precision, 0)
            val supportsHighp = range[0] != 0 || range[1] != 0
            Log.i("GpuRenderer", "Renderer: $gpuRenderer | Version: $glVersion | GLSL: $glslVersion | highp: $supportsHighp")
            onGpuInfo(gpuRenderer, glVersion)
        } catch (e: Exception) {
            Log.e("GpuRenderer", "Error reading GPU info: ${e.message}")
        }

        // 2. Disable vsync swap limit by setting swap interval to 0 (BUG-6)
        try {
            val display = android.opengl.EGL14.eglGetCurrentDisplay()
            if (display != android.opengl.EGL14.EGL_NO_DISPLAY) {
                android.opengl.EGL14.eglSwapInterval(display, 0)
                Log.i("GpuRenderer", "Set EGL swap interval to 0 (vsync disabled)")
            }
        } catch (e: Exception) {
            Log.e("GpuRenderer", "Failed to set swap interval: ${e.message}")
        }

        progTriangle    = prog(GpuBenchmarkShaders.TRIANGLE_VERT,   GpuBenchmarkShaders.TRIANGLE_FRAG)
        progCompute     = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.COMPUTE_FRAG)
        progParticle    = prog(GpuBenchmarkShaders.PARTICLE_VERT,    GpuBenchmarkShaders.PARTICLE_FRAG)
        progTexture     = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.TEXTURE_FRAG)
        progMesh        = prog(GpuBenchmarkShaders.MESH_VERT,        GpuBenchmarkShaders.MESH_FRAG)
        progMandelbrot  = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.MANDELBROT_FRAG)
        progMultiLight  = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.MULTI_LIGHT_FRAG)
        progRayMarch    = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.RAY_MARCH_FRAG)
        progDomainWarp  = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.DOMAIN_WARP_FRAG)
        progSuperSample = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.SUPER_SAMPLE_FRAG)
        progDisplay     = prog(GpuBenchmarkShaders.FULLSCREEN_VERT,  GpuBenchmarkShaders.PASSTHROUGH_FRAG)
        // GAP scenes
        progMemBw        = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.MEM_BW_FRAG)
        progVramPressure = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.VRAM_PRESSURE_FRAG)
        progMsaaTest     = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.MSAA_TEST_FRAG)
        progBloomHoriz   = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.BLOOM_HORIZ_FRAG)
        progBloomVert    = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.BLOOM_VERT_FRAG)
        progTessBase     = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.TESS_BASE_FRAG)
        progShaderTiming = prog(GpuBenchmarkShaders.FULLSCREEN_VERT, GpuBenchmarkShaders.SHADER_TIMING_FRAG)

        // 3. Cache uniform and attribute locations (BUG-2)
        uTimeTriangle = GLES20.glGetUniformLocation(progTriangle, "u_Time")
        aLocalTriangle = GLES20.glGetAttribLocation(progTriangle, "a_Local")
        aOrbitRTriangle = GLES20.glGetAttribLocation(progTriangle, "a_OrbitR")
        aOrbitPhTriangle = GLES20.glGetAttribLocation(progTriangle, "a_OrbitPh")
        aOrbitSpdTriangle = GLES20.glGetAttribLocation(progTriangle, "a_OrbitSpd")
        aRotSpdTriangle = GLES20.glGetAttribLocation(progTriangle, "a_RotSpd")
        aColorTriangle = GLES20.glGetAttribLocation(progTriangle, "a_Color")

        uTimeParticle = GLES20.glGetUniformLocation(progParticle, "u_Time")
        aPosParticle = GLES20.glGetAttribLocation(progParticle, "a_Pos")
        aLifeParticle = GLES20.glGetAttribLocation(progParticle, "a_Life")

        uTimeMesh = GLES20.glGetUniformLocation(progMesh, "u_Time")
        uAspectMesh = GLES20.glGetUniformLocation(progMesh, "u_Aspect")
        aGridMesh = GLES20.glGetAttribLocation(progMesh, "a_Grid")

        val fullscreenProgs = listOf(
            progCompute, progTexture, progMandelbrot,
            progMultiLight, progRayMarch, progDomainWarp, progSuperSample
        )
        for (p in fullscreenProgs) {
            fullProgramLocs[p] = ProgramLocations(
                aPos = GLES20.glGetAttribLocation(p, "a_Pos"),
                uTime = GLES20.glGetUniformLocation(p, "u_Time"),
                uAspect = GLES20.glGetUniformLocation(p, "u_Aspect")
            )
        }
        // GAP program locations
        uTexMemBw  = GLES20.glGetUniformLocation(progMemBw, "u_Tex")
        uTimeMemBw = GLES20.glGetUniformLocation(progMemBw, "u_Time")
        aPosMemBw  = GLES20.glGetAttribLocation(progMemBw, "a_Pos")
        aPosMsaa   = GLES20.glGetAttribLocation(progMsaaTest, "a_Pos")
        uTimeMsaa  = GLES20.glGetUniformLocation(progMsaaTest, "u_Time")
        for (i in 0..7) uTexVram[i] = GLES20.glGetUniformLocation(progVramPressure, "u_T$i")
        uTimeVram  = GLES20.glGetUniformLocation(progVramPressure, "u_Time")
        aPosVramPressure = GLES20.glGetAttribLocation(progVramPressure, "a_Pos")
        uTexBloomH = GLES20.glGetUniformLocation(progBloomHoriz, "u_Tex")
        uTimeBloomH = GLES20.glGetUniformLocation(progBloomHoriz, "u_Time")
        aPosBloom0  = GLES20.glGetAttribLocation(progBloomHoriz, "a_Pos")
        uTexBloomV  = GLES20.glGetUniformLocation(progBloomVert, "u_Tex")
        uTimeBloomV = GLES20.glGetUniformLocation(progBloomVert, "u_Time")
        aPosBloom1  = GLES20.glGetAttribLocation(progBloomVert, "a_Pos")
        uTimeTess   = GLES20.glGetUniformLocation(progTessBase, "u_Time")
        aPosTess    = GLES20.glGetAttribLocation(progTessBase, "a_Pos")
        uTimeShaderTiming = GLES20.glGetUniformLocation(progShaderTiming, "u_Time")
        aPosShaderT = GLES20.glGetAttribLocation(progShaderTiming, "a_Pos")
        uTexDisplay = GLES20.glGetUniformLocation(progDisplay, "u_Tex")
        aPosDisplay = GLES20.glGetAttribLocation(progDisplay, "a_Pos")

        // 4. Initialize GPU disjoint timer query (GAP-1a)
        try {
            val extStr = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
            if (extStr.contains("GL_EXT_disjoint_timer_query")) {
                val queryIds = IntArray(1)
                GLES30.glGenQueries(1, queryIds, 0)
                if (queryIds[0] != 0) {
                    timerQueryId = queryIds[0]
                    supportsTimerQuery = true
                    Log.i("GpuRenderer", "disjoint_timer_query extension supported. Query ID: $timerQueryId")
                }
            } else {
                Log.w("GpuRenderer", "disjoint_timer_query extension NOT supported")
            }
        } catch (e: Exception) {
            Log.e("GpuRenderer", "Failed to initialize timer query: ${e.message}")
        }

        buildTriangleBuffer(); buildQuadBuffer(); initParticles(); buildMeshBuffers()
        particleBuf = ByteBuffer.allocateDirect(P_COUNT * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        // Extended scene resources
        texMemBw = createNoiseTexture(1024, 1024)
        for (i in 0..7) texVram[i] = createNoiseTexture(512, 512)
        initMsaaFbo()
        initBloomFbos()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        vpW = width.coerceAtLeast(1); vpH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, vpW, vpH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val drawStart = System.nanoTime()
        val t = (System.currentTimeMillis() - startTimeMs) / 1000f

        var queryStarted = false
        if (supportsTimerQuery) {
            try {
                GLES30.glBeginQuery(0x88BF, timerQueryId)
                queryStarted = true
            } catch (e: Exception) {
                Log.e("GpuRenderer", "glBeginQuery failed: ${e.message}")
            }
        }

        // UTIL-1: Heavy ALU-bound scenes use a 4K offscreen viewport to force
        // GPU saturation. Light/geometry scenes keep the physical display size.
        // Extended stress scenes manage their own 4K viewport internally.
        val use4k = currentScene in HEAVY_4K_SCENES
        if (use4k) GLES20.glViewport(0, 0, 3840, 2160)


        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        when (currentScene) {
            GpuScene.TRIANGLE_RENDERING -> {
                drawFull(progDomainWarp, t)
                drawTriangleScene(t)
            }
            GpuScene.COMPUTE_MATRIX -> {
                repeat(4) { drawFull(progCompute, t) }
            }
            GpuScene.PARTICLE_SYSTEM -> {
                drawFull(progMultiLight, t)
                drawParticleScene(t)
            }
            GpuScene.TEXTURE_SAMPLING -> {
                repeat(4) { drawFull(progTexture, t) }
            }
            GpuScene.WIREFRAME_MESH -> {
                drawFull(progRayMarch, t)
                drawMeshScene(t)
            }
            GpuScene.MANDELBROT_DEEP -> {
                repeat(4) { drawFull(progMandelbrot, t) }
            }
            GpuScene.PHONG_MULTI_LIGHT -> {
                repeat(4) { drawFull(progMultiLight, t) }
            }
            GpuScene.RAY_MARCH_SDF -> {
                repeat(4) { drawFull(progRayMarch, t) }
            }
            GpuScene.DOMAIN_WARP -> {
                repeat(4) { drawFull(progDomainWarp, t) }
            }
            GpuScene.SUPER_SAMPLE -> {
                repeat(4) { drawFull(progSuperSample, t) }
            }
            GpuScene.SHADER_COMPILE  -> drawShaderCompileScene(t)
            GpuScene.MEM_BANDWIDTH   -> drawMemBandwidthScene(t)
            GpuScene.MSAA_4X         -> drawMsaaScene(t)
            GpuScene.VRAM_PRESSURE   -> drawVramPressureScene(t)
            GpuScene.GEOMETRY_ALU_SATURATION -> drawTessellationScene(t)
            GpuScene.MULTI_PASS_BLOOM -> drawBloomScene(t)
            // ── Vulkan 1.1 compute scenes (actual Vulkan compute runs in background) ──
            GpuScene.VULKAN_JULIA_COMPUTE,
            GpuScene.VULKAN_MANDELBROT_COMPUTE,
            GpuScene.VULKAN_GEMM_COMPUTE -> {
                // Show animated Vulkan indicator pattern
                drawVulkanIndicator(t)
            }
            // ── OpenCL 2.0 compute scenes (actual OpenCL runs in background) ──
            GpuScene.OPENCL_MEM_BW,
            GpuScene.OPENCL_JULIA_COMPUTE,
            GpuScene.OPENCL_GEMM_COMPUTE -> {
                // Show animated OpenCL indicator pattern
                drawOpenCLIndicator(t)
            }
            else -> { /* no-op */ }
        }

        // Restore physical display viewport after heavy scenes
        if (use4k) GLES20.glViewport(0, 0, vpW, vpH)

        if (queryStarted) {
            try {
                GLES30.glEndQuery(0x88BF)
                GLES20.glFinish() // Flush pipeline to ensure query availability
                val available = IntArray(1)
                GLES30.glGetQueryObjectuiv(timerQueryId, GLES30.GL_QUERY_RESULT_AVAILABLE, available, 0)
                if (available[0] != 0) {
                    val timeElapsed = IntArray(1)
                    GLES30.glGetQueryObjectuiv(timerQueryId, GLES30.GL_QUERY_RESULT, timeElapsed, 0)
                    // timeElapsed is in nanoseconds. Convert to milliseconds
                    val renderMs = timeElapsed[0] / 1_000_000f
                    val fps = if (renderMs > 0f) 1000f / renderMs else 999f
                    onFrameMetrics(fps, renderMs)
                    return
                }
            } catch (e: Exception) {
                Log.e("GpuRenderer", "Timer query failed: ${e.message}")
            }
        }

        // Fallback to CPU timing
        GLES20.glFinish()
        val renderMs = maxOf(0.001f, (System.nanoTime() - drawStart) / 1_000_000f)
        val fps = 1000f / renderMs
        onFrameMetrics(fps, renderMs)
    }

    // -------------------------------------------------------------------------
    // Scene 1 – 10 000 orbit-animated triangles (heavy vertex)
    // -------------------------------------------------------------------------
    private fun buildTriangleBuffer() {
        val BASE = floatArrayOf(-0.5f,-0.289f, 0.5f,-0.289f, 0f,0.577f)
        // stride: a_Local(2), a_OrbitR(1), a_OrbitPh(1), a_OrbitSpd(1), a_RotSpd(1), a_Color(3) = 9
        val data = FloatArray(TRI_COUNT * 3 * 9)
        var i = 0
        repeat(TRI_COUNT) { ti ->
            val oR  = 0.05f + (ti * 0.617f    % 850) / 1000f
            val oPh = (ti * 382.61f            % 6283.2f) / 1000f
            val oS  = 0.3f  + (ti * 127.3f    % 900) / 1000f
            val rS  = 0.5f  + (ti * 231.9f    % 2500) / 1000f
            val r   = 0.3f  + (ti * 321.7f    % 700) / 1000f
            val g   = 0.3f  + (ti * 513.7f    % 700) / 1000f
            val b   = 0.3f  + (ti * 719.3f    % 700) / 1000f
            for (v in 0..2) {
                data[i++]=BASE[v*2]; data[i++]=BASE[v*2+1]
                data[i++]=oR; data[i++]=oPh; data[i++]=oS; data[i++]=rS
                data[i++]=r;  data[i++]=g;   data[i++]=b
            }
        }
        triVertCount = TRI_COUNT * 3
        triBuf = ByteBuffer.allocateDirect(data.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        triBuf.put(data).position(0)
    }

    private fun drawTriangleScene(t: Float) {
        GLES20.glUseProgram(progTriangle)
        val stride = 9 * 4
        fun attr(loc: Int, off: Int, sz: Int) {
            if (loc < 0) return
            triBuf.position(off)
            GLES20.glEnableVertexAttribArray(loc)
            GLES20.glVertexAttribPointer(loc, sz, GLES20.GL_FLOAT, false, stride, triBuf)
        }
        attr(aLocalTriangle, 0, 2)
        attr(aOrbitRTriangle, 2, 1)
        attr(aOrbitPhTriangle, 3, 1)
        attr(aOrbitSpdTriangle, 4, 1)
        attr(aRotSpdTriangle, 5, 1)
        attr(aColorTriangle, 6, 3)

        if (uTimeTriangle >= 0) {
            GLES20.glUniform1f(uTimeTriangle, t)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triVertCount)

        if (aLocalTriangle >= 0) GLES20.glDisableVertexAttribArray(aLocalTriangle)
        if (aOrbitRTriangle >= 0) GLES20.glDisableVertexAttribArray(aOrbitRTriangle)
        if (aOrbitPhTriangle >= 0) GLES20.glDisableVertexAttribArray(aOrbitPhTriangle)
        if (aOrbitSpdTriangle >= 0) GLES20.glDisableVertexAttribArray(aOrbitSpdTriangle)
        if (aRotSpdTriangle >= 0) GLES20.glDisableVertexAttribArray(aRotSpdTriangle)
        if (aColorTriangle >= 0) GLES20.glDisableVertexAttribArray(aColorTriangle)
    }

    // -------------------------------------------------------------------------
    // Shared fullscreen quad (scenes 2,4,6,7,8,9,10)
    // -------------------------------------------------------------------------
    private fun buildQuadBuffer() {
        quadBuf = ByteBuffer.allocateDirect(QUAD.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        quadBuf.put(QUAD).position(0)
    }
    private fun drawDisplayTexture(tex: Int) {
        if (progDisplay == 0 || tex == 0) return
        GLES20.glUseProgram(progDisplay)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        if (uTexDisplay >= 0) GLES20.glUniform1i(uTexDisplay, 0)
        if (aPosDisplay >= 0) {
            GLES20.glEnableVertexAttribArray(aPosDisplay)
            GLES20.glVertexAttribPointer(aPosDisplay, 2, GLES20.GL_FLOAT, false, 0, quadBuf)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        if (aPosDisplay >= 0) GLES20.glDisableVertexAttribArray(aPosDisplay)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun drawFull(p: Int, t: Float) {
        if (p == 0) return
        GLES20.glUseProgram(p)
        val locs = fullProgramLocs[p] ?: return
        if (locs.uTime >= 0) {
            GLES20.glUniform1f(locs.uTime, t)
        }
        if (locs.uAspect >= 0) {
            GLES20.glUniform1f(locs.uAspect, vpW.toFloat() / vpH.toFloat().coerceAtLeast(1f))
        }
        if (locs.aPos >= 0) {
            GLES20.glEnableVertexAttribArray(locs.aPos)
            GLES20.glVertexAttribPointer(locs.aPos, 2, GLES20.GL_FLOAT, false, 0, quadBuf)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        if (locs.aPos >= 0) GLES20.glDisableVertexAttribArray(locs.aPos)
    }

    // -------------------------------------------------------------------------
    // Scene 3 – 5 000 CPU-physics particles (BUG-3 fixed: was mislabelled 50 000)
    // -------------------------------------------------------------------------
    private fun initParticles() { for (i in 0 until P_COUNT) spawnP(i) }
    private fun spawnP(i: Int) {
        pX[i]   = Random.nextFloat()*2f-1f; pY[i]   = Random.nextFloat()*0.5f-0.25f
        pVx[i]  = (Random.nextFloat()-0.5f)*0.5f; pVy[i]  = 0.3f+Random.nextFloat()*0.8f
        pLife[i]= 0.2f+Random.nextFloat()*0.8f
    }
    private fun drawParticleScene(t: Float) {
        val nowNs = System.nanoTime()
        val dt = if (lastParticleNs==0L) 0.016f else ((nowNs-lastParticleNs)/1e9f).coerceIn(0.001f,0.05f)
        lastParticleNs = nowNs
        for (i in 0 until P_COUNT) {
            pVy[i]-=0.55f*dt; pX[i]+=pVx[i]*dt; pY[i]+=pVy[i]*dt; pLife[i]-=dt*0.35f
            if (pLife[i]<=0f||pY[i]<-1.1f) spawnP(i)
            particleArray[i*3]=pX[i]; particleArray[i*3+1]=pY[i]; particleArray[i*3+2]=pLife[i]
        }
        particleBuf.position(0); particleBuf.put(particleArray); particleBuf.position(0)
        GLES20.glUseProgram(progParticle)
        if (uTimeParticle >= 0) {
            GLES20.glUniform1f(uTimeParticle, t)
        }
        if (aPosParticle >= 0) {
            particleBuf.position(0)
            GLES20.glEnableVertexAttribArray(aPosParticle)
            GLES20.glVertexAttribPointer(aPosParticle, 2, GLES20.GL_FLOAT, false, 3 * 4, particleBuf)
        }
        if (aLifeParticle >= 0) {
            particleBuf.position(2)
            GLES20.glEnableVertexAttribArray(aLifeParticle)
            GLES20.glVertexAttribPointer(aLifeParticle, 1, GLES20.GL_FLOAT, false, 3 * 4, particleBuf)
        }
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, P_COUNT)
        if (aPosParticle >= 0) GLES20.glDisableVertexAttribArray(aPosParticle)
        if (aLifeParticle >= 0) GLES20.glDisableVertexAttribArray(aLifeParticle)
    }

    // -------------------------------------------------------------------------
    // Scene 5 – 250×250 wave-displaced mesh (geometry throughput)
    // -------------------------------------------------------------------------
    private fun buildMeshBuffers() {
        val verts = FloatArray((GRID+1)*(GRID+1)*2); var vi=0
        for (row in 0..GRID) for (col in 0..GRID) { verts[vi++]=col.toFloat()/GRID*2f-1f; verts[vi++]=row.toFloat()/GRID*2f-1f }
        meshVerts = ByteBuffer.allocateDirect(verts.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        meshVerts.put(verts).position(0)
        val idx = mutableListOf<Short>()
        for (r in 0 until GRID) for (c in 0 until GRID) {
            val tl=(r*(GRID+1)+c).toShort(); val tr=(tl+1).toShort()
            val bl=(tl+(GRID+1)).toShort();  val br=(bl+1).toShort()
            idx+=tl;idx+=bl;idx+=tr; idx+=bl;idx+=br;idx+=tr
        }
        meshIdxCount=idx.size
        meshIdx=ByteBuffer.allocateDirect(meshIdxCount*2).order(ByteOrder.nativeOrder()).asShortBuffer()
        meshIdx.put(idx.toShortArray()).position(0)
    }
    private fun drawMeshScene(t: Float) {
        GLES20.glUseProgram(progMesh)
        if (uTimeMesh >= 0) {
            GLES20.glUniform1f(uTimeMesh, t)
        }
        if (uAspectMesh >= 0) {
            GLES20.glUniform1f(uAspectMesh, vpW.toFloat() / vpH)
        }
        if (aGridMesh >= 0) {
            meshVerts.position(0)
            GLES20.glEnableVertexAttribArray(aGridMesh)
            GLES20.glVertexAttribPointer(aGridMesh, 2, GLES20.GL_FLOAT, false, 0, meshVerts)
        }
        meshIdx.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, meshIdxCount, GLES20.GL_UNSIGNED_SHORT, meshIdx)
        if (aGridMesh >= 0) GLES20.glDisableVertexAttribArray(aGridMesh)
    }

    // =========================================================================
    // GAP scene helpers & draw methods
    // =========================================================================

    /** Upload a 1×1 solid-colour quad using the shared quadBuf + given program aPos location. */
    private fun drawQuadWith(p: Int, aPosLoc: Int, uTimeLoc: Int, t: Float) {
        if (p == 0) return
        GLES20.glUseProgram(p)
        if (uTimeLoc >= 0) GLES20.glUniform1f(uTimeLoc, t)
        if (aPosLoc >= 0) {
            GLES20.glEnableVertexAttribArray(aPosLoc)
            GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuf)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        if (aPosLoc >= 0) GLES20.glDisableVertexAttribArray(aPosLoc)
    }

    // ─── GAP-2: Shader compile speed ─────────────────────────────────────────
    /** Compile 6 shaders of increasing complexity; measure wall-clock ms total. */
    private fun measureShaderCompileTime() {
        val testFrags = listOf(
            GpuBenchmarkShaders.MANDELBROT_FRAG,
            GpuBenchmarkShaders.MULTI_LIGHT_FRAG,
            GpuBenchmarkShaders.RAY_MARCH_FRAG,
            GpuBenchmarkShaders.DOMAIN_WARP_FRAG,
            GpuBenchmarkShaders.SUPER_SAMPLE_FRAG,
            GpuBenchmarkShaders.TEXTURE_FRAG
        )
        val t0 = System.nanoTime()
        for (src in testFrags) {
            val id = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
            GLES20.glShaderSource(id, src.trimIndent())
            GLES20.glCompileShader(id)
            GLES20.glDeleteShader(id)
        }
        GLES20.glFinish() // flush driver compile queue
        shaderCompileMs = (System.nanoTime() - t0) / 1_000_000f
        Log.i("GpuRenderer", "GAP-2 shader compile: ${shaderCompileMs.toInt()} ms for 6 shaders")
    }

    // --- Shader Compile scene: 2x RayMarch + 2x DomainWarp @ 4K (real GPU load) ---
    private var shaderCompileDone = false
    private fun drawShaderCompileScene(t: Float) {
        if (!shaderCompileDone) { measureShaderCompileTime(); shaderCompileDone = true }
        // Real GPU ALU stress: run 2x ray-march + 2x domain-warp at 4K offscreen
        GLES20.glViewport(0, 0, 3840, 2160)
        repeat(2) { drawFull(progRayMarch, t) }
        repeat(2) { drawFull(progDomainWarp, t) }
        GLES20.glViewport(0, 0, vpW, vpH)
    }

    // ─── GAP-3: Memory bandwidth ──────────────────────────────────────────────
    /** 1024×1024 RGBA noise texture uploaded once; dependent sampling stresses bandwidth. */
    private fun createNoiseTexture(w: Int, h: Int): Int {
        val ids = IntArray(1); GLES20.glGenTextures(1, ids, 0)
        val tex = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        val pixels = ByteBuffer.allocateDirect(w * h * 4)
        val rng = java.util.Random(0xDEADBEEF)
        val arr = ByteArray(w * h * 4).also { rng.nextBytes(it) }
        pixels.put(arr).position(0)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return tex
    }

    // --- Memory Bandwidth: 4x 32-dep-sample passes @ 4K ---
    private fun drawMemBandwidthScene(t: Float) {
        if (progMemBw == 0 || texMemBw == 0 || fboBloom0 == 0) { GLES20.glViewport(0, 0, vpW, vpH); return }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom0)
        GLES20.glViewport(0, 0, 3840, 2160)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        repeat(4) {
            GLES20.glUseProgram(progMemBw)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texMemBw)
            if (uTexMemBw >= 0) GLES20.glUniform1i(uTexMemBw, 0)
            if (uTimeMemBw >= 0) GLES20.glUniform1f(uTimeMemBw, t)
            if (aPosMemBw >= 0) {
                GLES20.glEnableVertexAttribArray(aPosMemBw)
                GLES20.glVertexAttribPointer(aPosMemBw, 2, GLES20.GL_FLOAT, false, 0, quadBuf)
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            if (aPosMemBw >= 0) GLES20.glDisableVertexAttribArray(aPosMemBw)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        // Display result to screen
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, vpW, vpH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawDisplayTexture(texBloom0)
    }

    // ─── GAP-4: MSAA 4× ──────────────────────────────────────────────────────
    private fun initMsaaFbo() {
        try {
            // Resolve texture
            val texIds = IntArray(1); GLES20.glGenTextures(1, texIds, 0)
            texMsaaResolve = texIds[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texMsaaResolve)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                3840, 2160, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            // Resolve FBO
            val rFbo = IntArray(1); GLES20.glGenFramebuffers(1, rFbo, 0); fboMsaaResolve = rFbo[0]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMsaaResolve)
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texMsaaResolve, 0)
            // MSAA renderbuffers
            val rbIds = IntArray(2); GLES20.glGenRenderbuffers(2, rbIds, 0)
            rboMsaaColor = rbIds[0]; rboMsaaDepth = rbIds[1]
            GLES30.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rboMsaaColor)
            GLES30.glRenderbufferStorageMultisample(GLES20.GL_RENDERBUFFER, 4, GLES30.GL_RGBA8, 3840, 2160)
            GLES30.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rboMsaaDepth)
            GLES30.glRenderbufferStorageMultisample(GLES20.GL_RENDERBUFFER, 4, GLES30.GL_DEPTH_COMPONENT16, 3840, 2160)
            // MSAA FBO
            val mFbo = IntArray(1); GLES20.glGenFramebuffers(1, mFbo, 0); fboMsaa = mFbo[0]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMsaa)
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_RENDERBUFFER, rboMsaaColor)
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, rboMsaaDepth)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            Log.i("GpuRenderer", "GAP-4: MSAA 4× FBO initialised")
        } catch (e: Exception) {
            Log.e("GpuRenderer", "GAP-4 MSAA FBO init failed: ${e.message}")
        }
    }

    // --- MSAA 4x: 8 render+resolve cycles @ 4K for sustained GPU pressure ---
    private fun drawMsaaScene(t: Float) {
        if (fboMsaa == 0 || progMsaaTest == 0) {
            // Fallback: 4x Mandelbrot at 4K
            GLES20.glViewport(0, 0, 3840, 2160)
            repeat(4) { drawFull(progMandelbrot, t) }
            GLES20.glViewport(0, 0, vpW, vpH)
            return
        }
        // 8 MSAA render+resolve cycles to stress the resolve unit
        repeat(8) {
            // Render into MSAA FBO at 3840x2160
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboMsaa)
            GLES20.glViewport(0, 0, 3840, 2160)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            drawQuadWith(progMsaaTest, aPosMsaa, uTimeMsaa, t)
            // Blit MSAA -> resolve
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fboMsaa)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fboMsaaResolve)
            GLES30.glBlitFramebuffer(0, 0, 3840, 2160, 0, 0, 3840, 2160,
                GLES20.GL_COLOR_BUFFER_BIT, GLES20.GL_NEAREST)
        }
        // Final pass display to screen
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, vpW, vpH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawDisplayTexture(texMsaaResolve)
    }

    // ─── GAP-5: VRAM pressure ────────────────────────────────────────────────
    private fun drawVramPressureScene(t: Float) {
        // VRAM Texture Pressure: 4 passes each sampling all 8 large textures + complex ALU.
        // Render offscreen at 4K to ensure VRAM bandwidth pressure.
        if (progVramPressure == 0 || fboBloom0 == 0) { GLES20.glViewport(0, 0, vpW, vpH); return }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom0)
        GLES20.glViewport(0, 0, 3840, 2160)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(progVramPressure)
        for (i in 0..7) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texVram[i])
            if (uTexVram[i] >= 0) GLES20.glUniform1i(uTexVram[i], i)
        }
        repeat(4) { pass ->
            if (uTimeVram >= 0) GLES20.glUniform1f(uTimeVram, t + pass * 0.33f)
            if (aPosVramPressure >= 0) { GLES20.glEnableVertexAttribArray(aPosVramPressure)
                GLES20.glVertexAttribPointer(aPosVramPressure, 2, GLES20.GL_FLOAT, false, 0, quadBuf) }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        }
        if (aPosVramPressure >= 0) GLES20.glDisableVertexAttribArray(aPosVramPressure)
        for (i in 0..7) { GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0) }
        // Display result to screen
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, vpW, vpH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawDisplayTexture(texBloom0)
    }

    // ─── GAP-6: Tessellation ─────────────────────────────────────────────────
    // ES 3.2 tessellation requires GL_PATCHES primitive + tess control/eval shaders.
    // We detect support and fall back to the Phong fragment scene if unavailable.
    // --- Tessellation / Geometry ALU: 4x Phong 128-light @ 4K ---
    private fun drawTessellationScene(t: Float) {
        // Use 4x Phong 128-light at 4K for heavy GPU saturation (no CPU tessellation dependency)
        if (progMultiLight == 0 || fboBloom0 == 0) { GLES20.glViewport(0, 0, vpW, vpH); return }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom0)
        GLES20.glViewport(0, 0, 3840, 2160)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        repeat(4) { drawFull(progMultiLight, t) }
        // Display result to screen
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, vpW, vpH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawDisplayTexture(texBloom0)
    }

    // ─── GAP-7: Multi-pass bloom ──────────────────────────────────────────────
    private fun initBloomFbos() {
        try {
            val texIds = IntArray(2); GLES20.glGenTextures(2, texIds, 0)
            texBloom0 = texIds[0]; texBloom1 = texIds[1]
            for (tid in texIds) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tid)
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    3840, 2160, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            }
            val fboIds = IntArray(2); GLES20.glGenFramebuffers(2, fboIds, 0)
            fboBloom0 = fboIds[0]; fboBloom1 = fboIds[1]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom0)
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texBloom0, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom1)
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texBloom1, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            Log.i("GpuRenderer", "GAP-7: Bloom FBOs initialised")
        } catch (e: Exception) {
            Log.e("GpuRenderer", "GAP-7 Bloom FBO init failed: ${e.message}")
        }
    }

    // --- 5-Pass Gaussian Bloom @ 4K: 1 scene render + 2 horiz + 2 vert blur passes ---
    private fun drawBloomScene(t: Float) {
        if (fboBloom0 == 0 || fboBloom1 == 0) {
            GLES20.glViewport(0, 0, 3840, 2160)
            repeat(4) { drawFull(progDomainWarp, t) }
            GLES20.glViewport(0, 0, vpW, vpH)
            return
        }
        // Pass 1: render domain-warp scene at 4K into fboBloom0
        GLES20.glViewport(0, 0, 3840, 2160)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom0)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawFull(progDomainWarp, t)
        // Passes 2+3: horizontal gaussian x2 blur chain (ping-pong)
        repeat(2) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom1)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(progBloomHoriz)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBloom0)
            if (uTexBloomH >= 0) GLES20.glUniform1i(uTexBloomH, 0)
            if (uTimeBloomH >= 0) GLES20.glUniform1f(uTimeBloomH, t)
            if (aPosBloom0 >= 0) { GLES20.glEnableVertexAttribArray(aPosBloom0)
                GLES20.glVertexAttribPointer(aPosBloom0, 2, GLES20.GL_FLOAT, false, 0, quadBuf) }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            if (aPosBloom0 >= 0) GLES20.glDisableVertexAttribArray(aPosBloom0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            // Swap
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboBloom0)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(progBloomVert)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBloom1)
            if (uTexBloomV >= 0) GLES20.glUniform1i(uTexBloomV, 0)
            if (uTimeBloomV >= 0) GLES20.glUniform1f(uTimeBloomV, t)
            if (aPosBloom1 >= 0) { GLES20.glEnableVertexAttribArray(aPosBloom1)
                GLES20.glVertexAttribPointer(aPosBloom1, 2, GLES20.GL_FLOAT, false, 0, quadBuf) }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            if (aPosBloom1 >= 0) GLES20.glDisableVertexAttribArray(aPosBloom1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        // Final pass: display to screen
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, vpW, vpH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(progBloomVert)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBloom1)
        if (uTexBloomV >= 0) GLES20.glUniform1i(uTexBloomV, 0)
        if (uTimeBloomV >= 0) GLES20.glUniform1f(uTimeBloomV, t)
        if (aPosBloom1 >= 0) { GLES20.glEnableVertexAttribArray(aPosBloom1)
            GLES20.glVertexAttribPointer(aPosBloom1, 2, GLES20.GL_FLOAT, false, 0, quadBuf) }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        if (aPosBloom1 >= 0) GLES20.glDisableVertexAttribArray(aPosBloom1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun drawVulkanIndicator(t: Float) {
        GLES20.glClearColor(0.15f, 0.05f, 0.2f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawFull(progCompute, t)
        drawFull(progMandelbrot, t)
    }

    private fun drawOpenCLIndicator(t: Float) {
        GLES20.glClearColor(0.05f, 0.15f, 0.1f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawFull(progDomainWarp, t)
        drawFull(progTexture, t)
    }

    // ─── GL shader helpers ────────────────────────────────────────────────────
    private fun compile(type: Int, src: String): Int {
        val id = GLES20.glCreateShader(type)
        GLES20.glShaderSource(id, src.trimIndent()); GLES20.glCompileShader(id)
        val ok = IntArray(1); GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0]==0) { Log.e("GpuRenderer","Compile err: ${GLES20.glGetShaderInfoLog(id)}"); GLES20.glDeleteShader(id); return 0 }
        return id
    }
    private fun prog(vert: String, frag: String): Int {
        val vs=compile(GLES20.GL_VERTEX_SHADER,vert); val fs=compile(GLES20.GL_FRAGMENT_SHADER,frag)
        if (vs==0||fs==0) return 0
        val p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,vs); GLES20.glAttachShader(p,fs); GLES20.glLinkProgram(p)
        val ok=IntArray(1); GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0)
        if (ok[0]==0) { Log.e("GpuRenderer","Link err: ${GLES20.glGetProgramInfoLog(p)}"); GLES20.glDeleteProgram(p); return 0 }
        GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs); return p
    }

}
