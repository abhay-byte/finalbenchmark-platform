package com.ivarna.finalbenchmark2.gpu

/**
 * GLSL ES 2.0 shader sources for all five GPU benchmark scenes.
 *
 * Scene 1 – TRIANGLE_RENDERING : thousands of orbiting, rotating triangles → vertex throughput
 * Scene 2 – COMPUTE_MATRIX     : full-screen Julia-set + matrix chains  → shader ALU
 * Scene 3 – PARTICLE_SYSTEM    : 8 000 CPU-simulated particles (GL_POINTS) → data throughput
 * Scene 4 – TEXTURE_SAMPLING   : 8-octave FBM procedural noise           → texture fill-rate
 * Scene 5 – WIREFRAME_MESH     : 80×80 wave-displaced grid               → geometry throughput
 */
object GpuBenchmarkShaders {

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 1 ── Triangle Rendering
    // Each triangle carries its own orbit radius / phase / speed and local
    // rotation speed as per-vertex attributes, all baked into a single VBO.
    // ─────────────────────────────────────────────────────────────────────

    const val TRIANGLE_VERT = """
attribute vec2  a_Local;
attribute float a_OrbitR;
attribute float a_OrbitPh;
attribute float a_OrbitSpd;
attribute float a_RotSpd;
attribute vec3  a_Color;

uniform float u_Time;

varying vec3 v_Color;

void main() {
    // Orbit center
    float angle  = u_Time * a_OrbitSpd + a_OrbitPh;
    vec2  center = vec2(cos(angle) * a_OrbitR, sin(angle) * a_OrbitR);

    // Local rotation
    float la = u_Time * a_RotSpd;
    float lc = cos(la);
    float ls = sin(la);
    vec2  rot = vec2(a_Local.x * lc - a_Local.y * ls,
                     a_Local.x * ls + a_Local.y * lc);

    gl_Position = vec4(center + rot * 0.055, 0.0, 1.0);
    v_Color = a_Color;
}"""

    const val TRIANGLE_FRAG = """
precision mediump float;
varying vec3 v_Color;
uniform float u_Time;

void main() {
    // Slight shimmer so fragments are never trivially discarded
    float pulse = 0.85 + 0.15 * sin(u_Time * 4.0);
    gl_FragColor = vec4(v_Color * pulse, 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 2 ── Compute / Matrix (heavy ALU fragment shader)
    // ─────────────────────────────────────────────────────────────────────

    const val FULLSCREEN_VERT = """
attribute vec2 a_Pos;
varying   vec2 v_UV;

void main() {
    v_UV        = a_Pos * 0.5 + 0.5;
    gl_Position = vec4(a_Pos, 0.0, 1.0);
}"""

    /** Julia-set iteration + chained mat4 multiplies per pixel — stresses shader ALU. */
    const val COMPUTE_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;

mat4 rotY(float a) {
    float c = cos(a); float s = sin(a);
    return mat4( c, 0.0, s, 0.0,
                0.0, 1.0, 0.0, 0.0,
                -s, 0.0,  c, 0.0,
                0.0, 0.0, 0.0, 1.0);
}

mat4 rotZ(float a) {
    float c = cos(a); float s = sin(a);
    return mat4(  c,  s, 0.0, 0.0,
                 -s,  c, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0);
}

void main() {
    vec2 uv = v_UV * 3.6 - 1.8;

    // Chain 16 mat4 multiplies to burn ALU (each mat4×mat4 = 64 muls + 48 adds)
    mat4 m = rotY(u_Time * 0.31);
    m = m * rotZ(u_Time * 0.19 + 0.7);
    m = m * rotY(u_Time * 0.23 + 1.4);
    m = m * rotZ(u_Time * 0.17 + 2.1);
    m = m * rotY(u_Time * 0.29 + 2.8);
    m = m * rotZ(u_Time * 0.13 + 3.5);
    m = m * rotY(u_Time * 0.37 + 4.2);
    m = m * rotZ(u_Time * 0.11 + 4.9);
    m = m * rotY(u_Time * 0.41 + 5.6);
    m = m * rotZ(u_Time * 0.07 + 6.3);
    m = m * rotY(u_Time * 0.43 + 7.0);
    m = m * rotZ(u_Time * 0.09 + 7.7);
    m = m * rotY(u_Time * 0.47 + 8.4);
    m = m * rotZ(u_Time * 0.05 + 9.1);
    m = m * rotY(u_Time * 0.53 + 9.8);
    m = m * rotZ(u_Time * 0.03 + 10.5);
    vec4 p = m * vec4(uv, 0.2, 1.0);

    // Julia-set iteration (128 iterations — always full, no early exit → max ALU pressure)
    vec2 z = uv;
    vec2 c = vec2(0.355 + 0.12 * sin(u_Time * 0.5),
                  0.355 + 0.12 * cos(u_Time * 0.4));
    float iter = 0.0;
    float escaped = 0.0;
    for (int i = 0; i < 128; i++) {
        // Always compute — no break — keeps ALU busy on every pixel
        float escaped_flag = step(4.0, dot(z, z));
        vec2 next = vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c;
        z    = mix(next, z, escaped_flag);
        iter += 1.0 - escaped_flag;
        escaped = max(escaped, escaped_flag);
    }

    float t = iter / 128.0;
    vec3 col = 0.5 + 0.5 * vec3(sin(t * 6.28318 + u_Time),
                                  sin(t * 6.28318 + u_Time + 2.094),
                                  sin(t * 6.28318 + u_Time + 4.189));
    col = col * (0.7 + escaped * 0.0) + abs(p.xyz) * 0.3;
    gl_FragColor = vec4(col, 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 3 ── Particle System (GL_POINTS)
    // ─────────────────────────────────────────────────────────────────────

    const val PARTICLE_VERT = """
attribute vec2  a_Pos;
attribute float a_Life;  // 0 = dead, 1 = newborn

uniform float u_Time;

varying float v_Life;

void main() {
    gl_Position  = vec4(a_Pos, 0.0, 1.0);
    gl_PointSize = max(2.0, 10.0 * a_Life);
    v_Life       = a_Life;
}"""

    const val PARTICLE_FRAG = """
precision mediump float;
varying float v_Life;
uniform float u_Time;

void main() {
    vec2  coord = gl_PointCoord - vec2(0.5);
    float d     = length(coord);
    if (d > 0.5) discard;

    float alpha = (1.0 - d * 2.0) * v_Life;
    vec3  col   = vec3(0.6 + 0.4 * sin(u_Time + v_Life * 6.28),
                       0.3 + 0.3 * cos(u_Time * 0.8),
                       0.8 - 0.3 * sin(u_Time * 1.3));
    gl_FragColor = vec4(col, alpha * 0.9);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 4 ── Texture Sampling  (8-octave FBM — many dependent fetches)
    // ─────────────────────────────────────────────────────────────────────

    const val TEXTURE_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i),             hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0,1.0)), hash(i + vec2(1.0,1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 12; i++) {
        v += a * noise(p);
        p  = p * 2.03 + vec2(0.31 * float(i), 0.17 * float(i));
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2  uv = v_UV;
    float t  = u_Time * 0.07;

    // Six cascaded dependent fbm lookups — each depends on the previous
    float n1 = fbm(uv * 3.0 + t);
    float n2 = fbm(uv * 6.0 - t * 1.3 + n1 * 0.8);
    float n3 = fbm(uv * 12.0 + t * 0.7 + n2 * 0.6);
    float n4 = fbm(uv * 4.0  + vec2(n2, n3) * 1.5);
    float n5 = fbm(uv * 8.0  - t * 0.9 + n3 * 0.5 + vec2(n4, n1) * 0.7);
    float n6 = fbm(uv * 2.0  + t * 0.4 + vec2(n5, n4) * 1.2);

    vec3 a = vec3(0.1, 0.3, 0.8);
    vec3 b = vec3(0.9, 0.5, 0.1);
    vec3 c = vec3(0.5, 0.9, 0.3);
    vec3 col = mix(mix(a, b, n1 * n6), mix(b, c, n2 * n5), n3 * n4);
    col += 0.1 * vec3(sin(n5 * 6.28), cos(n6 * 4.71), sin(n4 * 3.14));
    col = pow(max(col, vec3(0.0)), vec3(0.75));
    gl_FragColor = vec4(col, 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 5 ── Dense Mesh / Geometry Throughput
    // ─────────────────────────────────────────────────────────────────────

    const val MESH_VERT = """
attribute vec2 a_Grid;      // normalised [-1, 1] grid position

uniform float u_Time;
uniform float u_Aspect;     // height / width

varying float v_H;          // displaced height for colouring
varying vec2  v_Grid;

void main() {
    float h = 0.18 * sin(a_Grid.x * 7.0 + u_Time * 1.8)
                   * cos(a_Grid.y * 5.5 + u_Time * 1.4)
            + 0.06 * sin(a_Grid.x * 14.0 + u_Time * 3.1)
                   * cos(a_Grid.y * 11.0 + u_Time * 2.7);
    v_H    = h * 0.5 + 0.5;
    v_Grid = a_Grid;
    gl_Position = vec4(a_Grid.x, a_Grid.y * u_Aspect, h, 1.0);
}"""

    const val MESH_FRAG = """
precision mediump float;
varying float v_H;
varying vec2  v_Grid;
uniform float u_Time;

void main() {
    vec3 cool = vec3(0.05, 0.30, 0.85);
    vec3 warm = vec3(0.95, 0.25, 0.20);
    vec3 col  = mix(cool, warm, v_H);
    col += 0.12 * vec3(sin(u_Time * 1.1 + v_Grid.x * 4.0),
                        cos(u_Time * 0.9 + v_Grid.y * 3.5),
                        sin(u_Time * 1.3));
    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 6 ── Mandelbrot Deep Zoom (512 iterations, smooth colouring)
    // ─────────────────────────────────────────────────────────────────────
    const val MANDELBROT_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;

void main() {
    float zoom = 0.3 + 0.25 * sin(u_Time * 0.07);
    vec2  c    = (v_UV - 0.5) * (2.6 * zoom) + vec2(-0.7269, 0.1889);
    vec2  z    = vec2(0.0);
    float iter = 0.0;
    const float MAX = 512.0;
    for (float i = 0.0; i < MAX; i++) {
        if (dot(z, z) > 4.0) break;
        z    = vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c;
        iter = i;
    }
    float smooth_i = iter - log2(log2(dot(z,z))) + 4.0;
    float t = smooth_i / MAX;
    vec3  col = 0.5 + 0.5 * cos(6.28318 * (vec3(0.0, 0.33, 0.67) + t * 3.0));
    gl_FragColor = vec4(col, 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 7 ── Phong 128-Light Array (analytic per-pixel)
    // ─────────────────────────────────────────────────────────────────────
    const val MULTI_LIGHT_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;

float hash(float n) { return fract(sin(n) * 43758.5453123); }

void main() {
    vec2 uv  = v_UV * 2.0 - 1.0;
    vec3 pos = vec3(uv, 0.0);
    vec3 nor = normalize(vec3(sin(uv.x * 2.1 + u_Time * 0.4),
                               cos(uv.y * 1.9 + u_Time * 0.3), 1.0));
    vec3 col = vec3(0.0);
    for (int i = 0; i < 128; i++) {
        float fi  = float(i);
        float spd = 0.4 + hash(fi * 3.7) * 0.6;
        float rx  = hash(fi * 1.1) * 2.0 - 1.0;
        float ry  = hash(fi * 1.7) * 2.0 - 1.0;
        vec3  lp  = vec3(rx * cos(u_Time * spd + fi),
                          ry * sin(u_Time * spd * 0.8 + fi * 0.5),
                          0.8 + 0.2 * sin(fi));
        vec3  lc  = vec3(hash(fi * 2.1), hash(fi * 2.9), hash(fi * 3.3));
        vec3  ld  = normalize(lp - pos);
        float diff = max(dot(nor, ld), 0.0);
        vec3  ref  = reflect(-ld, nor);
        float spec = pow(max(dot(ref, vec3(0.0,0.0,1.0)), 0.0), 32.0);
        float atten = 1.0 / (1.0 + dot(lp - pos, lp - pos) * 2.0);
        col += lc * (diff * 0.6 + spec * 0.3) * atten;
    }
    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 8 ── Ray March SDF Scene (100 steps, soft shadows, AO)
    // Objects centered; u_Time-based animation keeps them on screen
    // ─────────────────────────────────────────────────────────────────────
    const val RAY_MARCH_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;
uniform float u_Aspect;   // vpW / vpH — corrects sphere shape on landscape

float sdSphere(vec3 p, float r) { return length(p) - r; }
float sdPlane(vec3 p) { return p.y + 0.9; }
float sdBox(vec3 p, vec3 b) {
    vec3 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, max(d.y, d.z)), 0.0);
}
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5*(b-a)/k, 0.0, 1.0);
    return mix(b, a, h) - k*h*(1.0-h);
}
float scene(vec3 p) {
    // All objects centered around x=0, y near 0
    float s1 = sdSphere(p - vec3(sin(u_Time * 0.7) * 0.45, 0.0, 0.0), 0.32);
    float s2 = sdSphere(p - vec3(-0.5 + 0.1*cos(u_Time*0.5), 0.0, 0.55), 0.24);
    float bx = sdBox(p - vec3(0.42, -0.3, -0.25), vec3(0.20));
    float pl = sdPlane(p);
    return smin(smin(s1, smin(s2, bx, 0.14), 0.11), pl, 0.04);
}
vec3 normal(vec3 p) {
    const float e = 0.001;
    return normalize(vec3(scene(p+vec3(e,0,0))-scene(p-vec3(e,0,0)),
                           scene(p+vec3(0,e,0))-scene(p-vec3(0,e,0)),
                           scene(p+vec3(0,0,e))-scene(p-vec3(0,0,e))));
}
float softShadow(vec3 ro, vec3 rd, float mint, float maxt, float k) {
    float res = 1.0;
    float t   = mint;
    for (int i = 0; i < 32; i++) {
        float h = scene(ro + rd * t);
        if (h < 0.001) return 0.0;
        res = min(res, k * h / t);
        t  += clamp(h, 0.01, 0.2);
        if (t > maxt) break;
    }
    return clamp(res, 0.0, 1.0);
}
float ao(vec3 p, vec3 n) {
    float occ = 0.0, scale = 1.0;
    for (int i = 0; i < 5; i++) {
        float h = 0.01 + 0.12 * float(i) / 4.0;
        occ += (h - scene(p + n * h)) * scale;
        scale *= 0.95;
    }
    return clamp(1.0 - 3.0 * occ, 0.0, 1.0);
}
void main() {
    vec2  uv  = (v_UV - 0.5) * 2.0;
    uv.x *= u_Aspect;   // aspect-ratio correction — circles stay circular
    vec3  ro  = vec3(0.0, 0.6, 2.8);
    vec3  rd  = normalize(vec3(uv.x, uv.y - 0.1, -1.5));
    vec3  ld  = normalize(vec3(0.3, 1.0, 0.6));
    float t   = 0.0;
    vec3  col = vec3(0.10, 0.15, 0.25);
    for (int i = 0; i < 100; i++) {
        vec3  p = ro + rd * t;
        float d = scene(p);
        if (d < 0.001) {
            vec3 n  = normal(p);
            float sh = softShadow(p + n * 0.002, ld, 0.02, 4.0, 16.0);
            float ao_v = ao(p, n);
            float diff = max(dot(n, ld), 0.0);
            vec3  ref  = reflect(rd, n);
            float spec = pow(max(dot(ref, ld), 0.0), 48.0);
            col = (vec3(0.7, 0.75, 0.8) * diff * sh + vec3(0.4) * spec) * ao_v
                  + vec3(0.05, 0.07, 0.12) * (1.0 - ao_v);
            break;
        }
        t += d;
        if (t > 20.0) break;
    }
    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 9 ── Triple Domain Warp FBM (3 × 12-octave noise passes)
    // ─────────────────────────────────────────────────────────────────────
    const val DOMAIN_WARP_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}
float noise(vec2 p) {
    vec2  i = floor(p), f = fract(p);
    vec2  u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i+vec2(0,0)), hash(i+vec2(1,0)), u.x),
               mix(hash(i+vec2(0,1)), hash(i+vec2(1,1)), u.x), u.y) * 2.0 - 1.0;
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 12; i++) {
        v += a * noise(p);
        p  = p * 2.01 + vec2(3.7, 1.9);
        a *= 0.5;
    }
    return v;
}
void main() {
    vec2 p  = (v_UV - 0.5) * 4.0;
    float t = u_Time * 0.12;
    // First warp
    vec2 q  = vec2(fbm(p + t), fbm(p + vec2(5.2, 1.3) + t));
    // Second warp
    vec2 r  = vec2(fbm(p + 4.0*q + vec2(1.7, 9.2) + t*0.6),
                   fbm(p + 4.0*q + vec2(8.3, 2.8) + t*0.6));
    // Third warp
    float f = fbm(p + 4.0*r + t*0.3);
    vec3 col = mix(vec3(0.1,0.1,0.4), vec3(0.9,0.6,0.1),
                   clamp(f*f*4.0, 0.0, 1.0));
    col = mix(col, vec3(0.0,0.0,0.5),
              clamp(length(q), 0.0, 1.0));
    col = mix(col, vec3(1.0,0.9,1.0),
              clamp(r.x*r.x + r.y*r.y, 0.0, 1.0));
    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}"""

    // ─────────────────────────────────────────────────────────────────────
    // SCENE 10 ── 32× Super-Sampled Newton Fractal
    // ─────────────────────────────────────────────────────────────────────
    const val SUPER_SAMPLE_FRAG = """
precision highp float;
varying vec2  v_UV;
uniform float u_Time;
uniform float u_Aspect;

vec2 cmul(vec2 a, vec2 b) { return vec2(a.x*b.x - a.y*b.y, a.x*b.y + a.y*b.x); }
vec2 cdiv(vec2 a, vec2 b) {
    float d = dot(b,b);
    return vec2(dot(a,b), a.y*b.x - a.x*b.y) / d;
}
vec3 newton(vec2 z) {
    for (int i = 0; i < 48; i++) {
        vec2 z2 = cmul(z,z);
        vec2 z3 = cmul(z2,z);
        // z - (z^3 - 1) / (3*z^2)
        z = z - cdiv(z3 - vec2(1.0,0.0), 3.0 * z2);
        float d0 = length(z - vec2(1.0, 0.0));
        float d1 = length(z - vec2(-0.5,  0.866));
        float d2 = length(z - vec2(-0.5, -0.866));
        float mn = min(d0, min(d1, d2));
        if (mn < 0.001) {
            float fi = float(i) / 48.0;
            if (mn == d0) return mix(vec3(1.0,0.2,0.1), vec3(1.0,0.9,0.7), fi);
            if (mn == d1) return mix(vec3(0.1,0.7,0.2), vec3(0.7,1.0,0.8), fi);
            return mix(vec3(0.1,0.2,0.9), vec3(0.7,0.8,1.0), fi);
        }
    }
    return vec3(0.0);
}
// Halton low-discrepancy sequence offset
vec2 halton(int i) {
    float x = 0.0; float f = 0.5;
    int n = i;
    for (int j = 0; j < 6; j++) {
        x += f * float(n - (n/2)*2);
        n /= 2; f *= 0.5;
    }
    float y = 0.0; f = 1.0/3.0;
    n = i;
    for (int j = 0; j < 4; j++) {
        y += f * float(n - (n/3)*3);
        n /= 3; f /= 3.0;
    }
    return vec2(x, y) - 0.5;
}
void main() {
    float scale = 1.6 + 0.4 * sin(u_Time * 0.05);
    vec3 acc = vec3(0.0);
    const float N = 32.0;
    const float PIX = 0.002;
    for (int i = 0; i < 32; i++) {
        vec2 jitter = halton(i) * PIX;
        vec2 uv = (v_UV - 0.5) * scale * 2.5 + jitter;
        uv.x *= u_Aspect;
        acc += newton(uv);
    }
    gl_FragColor = vec4(acc / N, 1.0);
}"""

    // ─── GAP-3: Memory Bandwidth — dependent texture reads ─────────────────
    // Allocates one 1024×1024 RGBA texture (4MB); samples it with offset chain
    // so GPU can't coalesce → stresses texture cache & memory bandwidth.
    const val MEM_BW_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform sampler2D u_Tex;
uniform float u_Time;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }
void main() {
    vec4 c = vec4(0.0);
    // 32 independent texture samples with hash-based UV offsets (no dependency chain).
    // Each sample at unique coordinate → maximal texture bandwidth pressure, zero serial stall.
    for (int i = 0; i < 32; i++) {
        float fi = float(i);
        vec2 off = vec2(hash(v_UV + fi * 0.13 + u_Time * 0.07),
                        hash(v_UV + fi * 0.19 - u_Time * 0.11));
        c += texture2D(u_Tex, fract(v_UV + off * 0.6));
    }
    gl_FragColor = vec4(c.rgb / 32.0, 1.0);
}"""

    // --- VRAM Pressure: 8 large textures sampled per pixel + ALU work ---
    const val VRAM_PRESSURE_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform sampler2D u_T0, u_T1, u_T2, u_T3, u_T4, u_T5, u_T6, u_T7;
uniform float u_Time;
void main() {
    vec2 uv0 = v_UV;
    vec2 uv1 = fract(v_UV * 1.3 + 0.1);
    vec2 uv2 = fract(v_UV * 0.7 + 0.2);
    vec2 uv3 = fract(v_UV * 1.7 + 0.3);
    vec4 c  = texture2D(u_T0, uv0) + texture2D(u_T1, uv1)
            + texture2D(u_T2, uv2) + texture2D(u_T3, uv3)
            + texture2D(u_T4, fract(uv0 + 0.5)) + texture2D(u_T5, fract(uv1 + 0.5))
            + texture2D(u_T6, fract(uv2 + 0.5)) + texture2D(u_T7, fract(uv3 + 0.5));
    // ALU: Phong on blended normal to prevent driver from discarding texture work
    vec3 n = normalize(c.rgb * 2.0 - 1.0);
    vec3 L = normalize(vec3(cos(u_Time * 0.5), sin(u_Time * 0.5), 1.0));
    float diff = max(dot(n, L), 0.0);
    float spec = pow(max(dot(reflect(-L, n), vec3(0.0, 0.0, 1.0)), 0.0), 32.0);
    gl_FragColor = vec4(c.rgb / 8.0 * diff + spec * 0.3, 1.0);
}"""

    // ─── GAP-4: MSAA helper — solid animated color (rendered 4× per MSAA) ──
    const val MSAA_TEST_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform float u_Time;
// Cheap but realistic: Mandelbrot at 64-iter inside MSAA resolve path
vec3 mandel(vec2 c) {
    vec2 z = c; int i = 0;
    for (int j = 0; j < 64; j++) { if (dot(z,z) > 4.0) break; z = vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c; i = j; }
    float t = float(i) / 64.0;
    return vec3(t * 0.9, t * 0.5, 1.0 - t);
}
void main() {
    vec2 c = (v_UV - 0.5) * 3.5 + vec2(-0.7, 0.0);
    c += 0.3 * vec2(cos(u_Time * 0.05), sin(u_Time * 0.07));
    gl_FragColor = vec4(mandel(c), 1.0);
}"""

    // ─── GAP-7: Bloom — horizontal gaussian pass ────────────────────────────
    const val BLOOM_HORIZ_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform sampler2D u_Tex;
uniform float u_Time;
void main() {
    vec2 sz = vec2(1.0 / 3840.0, 0.0);
    vec4 c = texture2D(u_Tex, v_UV) * 0.1415;
    c += (texture2D(u_Tex, v_UV + sz*1.0) + texture2D(u_Tex, v_UV - sz*1.0)) * 0.1379;
    c += (texture2D(u_Tex, v_UV + sz*2.0) + texture2D(u_Tex, v_UV - sz*2.0)) * 0.1295;
    c += (texture2D(u_Tex, v_UV + sz*3.0) + texture2D(u_Tex, v_UV - sz*3.0)) * 0.1109;
    c += (texture2D(u_Tex, v_UV + sz*4.0) + texture2D(u_Tex, v_UV - sz*4.0)) * 0.0863;
    c += (texture2D(u_Tex, v_UV + sz*5.0) + texture2D(u_Tex, v_UV - sz*5.0)) * 0.0610;
    c += (texture2D(u_Tex, v_UV + sz*6.0) + texture2D(u_Tex, v_UV - sz*6.0)) * 0.0391;
    c += (texture2D(u_Tex, v_UV + sz*7.0) + texture2D(u_Tex, v_UV - sz*7.0)) * 0.0228;
    gl_FragColor = c;
}"""

    // ─── GAP-7: Bloom — vertical gaussian pass ──────────────────────────────
    const val BLOOM_VERT_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform sampler2D u_Tex;
uniform float u_Time;
void main() {
    vec2 sz = vec2(0.0, 1.0 / 2160.0);
    vec4 c = texture2D(u_Tex, v_UV) * 0.1415;
    c += (texture2D(u_Tex, v_UV + sz*1.0) + texture2D(u_Tex, v_UV - sz*1.0)) * 0.1379;
    c += (texture2D(u_Tex, v_UV + sz*2.0) + texture2D(u_Tex, v_UV - sz*2.0)) * 0.1295;
    c += (texture2D(u_Tex, v_UV + sz*3.0) + texture2D(u_Tex, v_UV - sz*3.0)) * 0.1109;
    c += (texture2D(u_Tex, v_UV + sz*4.0) + texture2D(u_Tex, v_UV - sz*4.0)) * 0.0863;
    c += (texture2D(u_Tex, v_UV + sz*5.0) + texture2D(u_Tex, v_UV - sz*5.0)) * 0.0610;
    c += (texture2D(u_Tex, v_UV + sz*6.0) + texture2D(u_Tex, v_UV - sz*6.0)) * 0.0391;
    c += (texture2D(u_Tex, v_UV + sz*7.0) + texture2D(u_Tex, v_UV - sz*7.0)) * 0.0228;
    c.rgb += max(c.rgb - 0.6, 0.0) * 2.5;
    gl_FragColor = c;
}"""

    // ─── Simple Passthrough Display ──────────────────────────────────────────
    const val PASSTHROUGH_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform sampler2D u_Tex;
void main() {
    gl_FragColor = texture2D(u_Tex, v_UV);
}"""

    // ─── GAP-6: Tessellation passthrough vert (ES 3.2 path; no tess = skip) ─
    // Actual tess uses GLES30.glDrawArrays + patch primitive; shaders injected at runtime
    const val TESS_BASE_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform float u_Time;
// Phong shading on tessellated Bezier surface — color by patch normal
void main() {
    vec3 N = normalize(vec3(v_UV * 2.0 - 1.0, sqrt(max(0.0,
        1.0 - dot(v_UV*2.0-1.0, v_UV*2.0-1.0)))));
    vec3 L = normalize(vec3(cos(u_Time*0.5), sin(u_Time*0.5), 1.0));
    float diff = max(dot(N, L), 0.0);
    float spec = pow(max(dot(reflect(-L, N), vec3(0,0,1)), 0.0), 32.0);
    vec3 col = vec3(0.2, 0.5, 0.9) * diff + vec3(1.0) * spec * 0.6;
    gl_FragColor = vec4(col, 1.0);
}"""

    // ─── GAP-2: Shader Compilation — result display frag ───────────────────
    // The scene measures wall-clock time to compile a pool of 6 shaders.
    // This frag visualises result: compile time heat-map.
    const val SHADER_TIMING_FRAG = """
precision highp float;
varying vec2 v_UV;
uniform float u_Time;   // repurposed: compile_ms normalised 0-1
void main() {
    // Green (fast) → Red (slow) bar chart feel
    float t = clamp(u_Time, 0.0, 1.0);
    vec3 fast = vec3(0.1, 0.9, 0.3);
    vec3 slow = vec3(1.0, 0.2, 0.1);
    vec3 col = mix(fast, slow, t);
    // Scanline pattern
    float grid = step(0.02, fract(v_UV.y * 20.0));
    col *= 0.7 + 0.3 * grid;
    gl_FragColor = vec4(col, 1.0);
}"""

}
