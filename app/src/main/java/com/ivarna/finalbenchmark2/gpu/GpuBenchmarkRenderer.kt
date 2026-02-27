package com.ivarna.finalbenchmark2.gpu

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

/** All ten rendering scenes. */
enum class GpuScene {
    TRIANGLE_RENDERING,
    COMPUTE_MATRIX,
    PARTICLE_SYSTEM,
    TEXTURE_SAMPLING,
    WIREFRAME_MESH,
    MANDELBROT_DEEP,
    PHONG_MULTI_LIGHT,
    RAY_MARCH_SDF,
    DOMAIN_WARP,
    SUPER_SAMPLE
}

/**
 * OpenGL ES 2.0 renderer driving all 10 GPU benchmark scenes.
 *
 * Timing: glFinish() is called after every draw call and wall-clock time is measured between
 * render start and GPU completion. This gives *uncapped* GPU render-time (not vsync-gated)
 * so heavy scenes correctly report FPS below 60.
 *
 * @param onFrameMetrics Called on the GL thread with (effectiveFps, gpuRenderTimeMs).
 */
class GpuBenchmarkRenderer(
    private val onFrameMetrics: (fps: Float, renderMs: Float) -> Unit
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

    // -------------------------------------------------------------------------
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.07f, 0.07f, 0.10f, 1f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        startTimeMs = System.currentTimeMillis()

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

        buildTriangleBuffer(); buildQuadBuffer(); initParticles(); buildMeshBuffers()
        particleBuf = ByteBuffer.allocateDirect(P_COUNT * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        vpW = width.coerceAtLeast(1); vpH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, vpW, vpH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val drawStart = System.nanoTime()
        val t = (System.currentTimeMillis() - startTimeMs) / 1000f
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        when (currentScene) {
            // Scenes 1, 3, 5: low fragment coverage on their own; add a heavy fullscreen
            // pre-pass so the fill-rate + ALU work forces real GPU effort every frame.
            GpuScene.TRIANGLE_RENDERING -> {
                // 4 domain-warp passes + triangle overlay
                repeat(4) { drawFull(progDomainWarp, t) }
                drawTriangleScene(t)
            }
            GpuScene.COMPUTE_MATRIX -> {
                // 4 compute passes — 16 mat4 chains + 128 Julia each → ~512 mat4 chains/frame
                repeat(4) { drawFull(progCompute, t) }
            }
            GpuScene.PARTICLE_SYSTEM -> {
                // 4 multi-light passes + particle overlay
                repeat(4) { drawFull(progMultiLight, t) }
                drawParticleScene(t)
            }
            GpuScene.TEXTURE_SAMPLING -> {
                // 4 texture/FBM passes — 6×12-octave FBM chains each
                repeat(4) { drawFull(progTexture, t) }
            }
            GpuScene.WIREFRAME_MESH -> {
                // 4 ray-march passes + mesh overlay
                repeat(4) { drawFull(progRayMarch, t) }
                drawMeshScene(t)
            }
            GpuScene.MANDELBROT_DEEP -> {
                // 4 mandelbrot passes — 512 iterations each
                repeat(4) { drawFull(progMandelbrot, t) }
            }
            GpuScene.PHONG_MULTI_LIGHT -> {
                // 4 multi-light passes — 128 analytic lights each
                repeat(4) { drawFull(progMultiLight, t) }
            }
            GpuScene.RAY_MARCH_SDF -> {
                // 4 ray-march passes — 100 SDF steps + soft-shadow each
                repeat(4) { drawFull(progRayMarch, t) }
            }
            GpuScene.DOMAIN_WARP -> {
                // 4 triple-domain-warp passes — 3×12-octave FBM each
                repeat(4) { drawFull(progDomainWarp, t) }
            }
            GpuScene.SUPER_SAMPLE -> {
                // 4 Newton-fractal super-sample passes — 64 samples × 48 Newton steps each
                repeat(4) { drawFull(progSuperSample, t) }
            }
        }
        // Flush GPU pipeline → uncapped accurate render time
        GLES20.glFinish()
        val renderMs = (System.nanoTime() - drawStart) / 1_000_000f
        val fps = if (renderMs > 0f) 1000f / renderMs else 999f
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
        fun attr(name: String, off: Int, sz: Int) {
            val loc = GLES20.glGetAttribLocation(progTriangle, name)
            if (loc < 0) return
            triBuf.position(off)
            GLES20.glEnableVertexAttribArray(loc)
            GLES20.glVertexAttribPointer(loc, sz, GLES20.GL_FLOAT, false, stride, triBuf)
        }
        attr("a_Local",0,2); attr("a_OrbitR",2,1); attr("a_OrbitPh",3,1)
        attr("a_OrbitSpd",4,1); attr("a_RotSpd",5,1); attr("a_Color",6,3)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(progTriangle,"u_Time"), t)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triVertCount)
        for (n in listOf("a_Local","a_OrbitR","a_OrbitPh","a_OrbitSpd","a_RotSpd","a_Color")) {
            val loc = GLES20.glGetAttribLocation(progTriangle, n)
            if (loc >= 0) GLES20.glDisableVertexAttribArray(loc)
        }
    }

    // -------------------------------------------------------------------------
    // Shared fullscreen quad (scenes 2,4,6,7,8,9,10)
    // -------------------------------------------------------------------------
    private fun buildQuadBuffer() {
        quadBuf = ByteBuffer.allocateDirect(QUAD.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        quadBuf.put(QUAD).position(0)
    }
    private fun drawFull(p: Int, t: Float) {
        if (p == 0) return
        GLES20.glUseProgram(p); quadBuf.position(0)
        val aPos = GLES20.glGetAttribLocation(p, "a_Pos")
        GLES20.glUniform1f(GLES20.glGetUniformLocation(p, "u_Time"), t)
        if (aPos >= 0) { GLES20.glEnableVertexAttribArray(aPos); GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quadBuf) }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        if (aPos >= 0) GLES20.glDisableVertexAttribArray(aPos)
    }

    // -------------------------------------------------------------------------
    // Scene 3 – 50 000 CPU-physics particles
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
        val arr = FloatArray(P_COUNT*3)
        for (i in 0 until P_COUNT) {
            pVy[i]-=0.55f*dt; pX[i]+=pVx[i]*dt; pY[i]+=pVy[i]*dt; pLife[i]-=dt*0.35f
            if (pLife[i]<=0f||pY[i]<-1.1f) spawnP(i)
            arr[i*3]=pX[i]; arr[i*3+1]=pY[i]; arr[i*3+2]=pLife[i]
        }
        particleBuf.position(0); particleBuf.put(arr); particleBuf.position(0)
        GLES20.glUseProgram(progParticle)
        val aPos = GLES20.glGetAttribLocation(progParticle,"a_Pos")
        val aLife= GLES20.glGetAttribLocation(progParticle,"a_Life")
        GLES20.glUniform1f(GLES20.glGetUniformLocation(progParticle,"u_Time"),t)
        if (aPos>=0) { particleBuf.position(0); GLES20.glEnableVertexAttribArray(aPos); GLES20.glVertexAttribPointer(aPos,2,GLES20.GL_FLOAT,false,3*4,particleBuf) }
        if (aLife>=0){ particleBuf.position(2); GLES20.glEnableVertexAttribArray(aLife);GLES20.glVertexAttribPointer(aLife,1,GLES20.GL_FLOAT,false,3*4,particleBuf) }
        GLES20.glDrawArrays(GLES20.GL_POINTS,0,P_COUNT)
        if (aPos>=0) GLES20.glDisableVertexAttribArray(aPos)
        if (aLife>=0) GLES20.glDisableVertexAttribArray(aLife)
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
        GLES20.glUseProgram(progMesh); meshVerts.position(0)
        val aGrid=GLES20.glGetAttribLocation(progMesh,"a_Grid")
        GLES20.glUniform1f(GLES20.glGetUniformLocation(progMesh,"u_Time"),t)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(progMesh,"u_Aspect"),vpW.toFloat()/vpH)
        if (aGrid>=0) { GLES20.glEnableVertexAttribArray(aGrid); GLES20.glVertexAttribPointer(aGrid,2,GLES20.GL_FLOAT,false,0,meshVerts) }
        meshIdx.position(0); GLES20.glDrawElements(GLES20.GL_TRIANGLES,meshIdxCount,GLES20.GL_UNSIGNED_SHORT,meshIdx)
        if (aGrid>=0) GLES20.glDisableVertexAttribArray(aGrid)
    }

    // -------------------------------------------------------------------------
    // GL shader helpers
    // -------------------------------------------------------------------------
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
