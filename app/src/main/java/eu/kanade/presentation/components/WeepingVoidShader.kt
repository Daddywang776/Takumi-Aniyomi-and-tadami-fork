package eu.kanade.presentation.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Isolates RuntimeShader (AGSL) usage for the "Weeping Void" background.
 * Prevents class validation crashes on devices below Android 13.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class WeepingVoidShader {

    private val shader = RuntimeShader(WEEPING_VOID_AGSL)
    private val brush = ShaderBrush(shader)

    /** Draw one GPU-driven pass of the weeping eye background. */
    fun DrawScope.drawWeepingVoid(light: Boolean, time: Float) {
        shader.setFloatUniform("u_resolution", size.width, size.height)
        shader.setFloatUniform("u_time", time)
        shader.setFloatUniform("u_light", if (light) 1f else 0f)
        shader.setFloatUniform("u_quality", 0f) // ECO preset
        drawRect(brush = brush)
    }
}

// Keep in sync with void_weeping_bg.agsl
private const val WEEPING_VOID_AGSL = """
uniform float2 u_resolution;
uniform float  u_time;
uniform float  u_light;
uniform float  u_quality;

const float EYE_SIZE = 0.41;
const float PUPIL    = 0.50;
const float BLINK    = 0.13;
const float VEINS    = 1.11;
const float IRIS     = 1.14;
const float TEAR_AMT = 0.34;
const float TEAR_SPD = 0.33;
const float BLOOD    = 0.29;
const float PULSE    = 1.06;
const float FOG      = 1.09;
const float GLITCH   = 0.07;
const float SCAN     = 0.07;
const float VIGNETTE = 0.41;
const float GRAIN    = 0.065;

float hash(float2 p){ p = fract(p * float2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); }

float vnoise(float2 p){
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(float2 p){
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        if (u_quality < 0.5 && i >= 3) { break; }
        v += a * vnoise(p);
        p *= 2.02;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord){
    float2 uv = (fragCoord - 0.5 * u_resolution) / u_resolution.y;
    float T = u_time;

    float gseed = hash(float2(floor(uv.y * 32.0), floor(T * 14.0)));
    float gband = step(1.0 - GLITCH * 0.22, gseed);
    uv.x += gband * (hash(float2(gseed, T)) - 0.5) * 0.18 * GLITCH;

    float pulse = 0.6 + 0.4 * sin(T * PULSE);

    float fog = fbm(uv * 3.0 + float2(0.0, T * 0.06));
    fog *= fbm(uv * 6.5 - float2(T * 0.04, 0.0));

    float rad = length(uv);
    float glow = smoothstep(1.0, 0.0, rad);

    float w = 0.42 * EYE_SIZE;
    float h = 0.23 * EYE_SIZE;
    float bt = T * BLINK;
    float ct = fract(bt);
    float bw = smoothstep(0.0, 0.04, ct) * (1.0 - smoothstep(0.08, 0.15, ct));
    float openA = 1.0 - bw;
    h *= max(openA, 0.02);
    float xn = clamp(uv.x / w, -1.0, 1.0);
    float lid = h * (1.0 - xn * xn);
    float eyeMask = smoothstep(0.006, 0.0, abs(uv.y) - lid) * step(abs(uv.x), w);
    float rimShadow = smoothstep(0.02, 0.0, abs(abs(uv.y) - lid)) * step(abs(uv.x), w) * 0.5 * eyeMask;

    float veinLine = smoothstep(0.62, 0.92, fbm(uv * float2(22.0, 11.0) + 3.0));

    float d = length(uv);
    float irisR = 0.145 * EYE_SIZE;
    float pupilR = irisR * (0.26 + 0.55 * PUPIL);
    float irisMask = smoothstep(irisR, irisR - 0.012, d);
    float pupilMask = smoothstep(pupilR, pupilR - 0.01, d);
    float ang = atan(uv.y, uv.x);
    float rays = 0.5 + 0.5 * sin(ang * 42.0 + fbm(uv * 9.0) * 6.2 + T * 0.5);
    float irisGrad = smoothstep(0.5, 0.0, d / irisR);
    float hl = smoothstep(0.022, 0.0, length(uv - float2(-0.035 * EYE_SIZE, 0.06 * EYE_SIZE)));

    float tears = 0.0;
    float leadEdge = 0.0;
    if (uv.y < 0.02) {
        float envelope = smoothstep(w * 1.15, 0.0, abs(uv.x)) * smoothstep(0.02, -0.02, uv.y);
        float streamN = fbm(float2(uv.x * 9.0, uv.y * 2.6 + T * TEAR_SPD));
        float streamN2 = fbm(float2(uv.x * 17.0 + 9.0, uv.y * 3.4 + T * TEAR_SPD * 1.4));
        float drip = smoothstep(0.5, 0.86, max(streamN, streamN2 * 0.9)) * envelope;
        tears = clamp(drip * TEAR_AMT, 0.0, 1.0);
        leadEdge = smoothstep(0.62, 0.7, streamN) * envelope * TEAR_AMT;
    }

    float3 col;
    if (u_light < 0.5) {
        col = float3(0.02, 0.0, 0.006);
        col += float3(0.55, 0.02, 0.05) * fog * FOG * (0.6 + 0.6 * pulse);
        col += float3(0.35, 0.0, 0.03) * glow * 0.4 * pulse * BLOOD;

        float3 sclera = float3(0.32, 0.05, 0.05) + float3(0.5, 0.0, 0.0) * veinLine * VEINS;
        float3 iris = mix(float3(0.12, 0.0, 0.0), float3(0.7, 0.05, 0.05), rays * IRIS);
        iris = mix(iris, float3(0.95, 0.25, 0.12), irisGrad * 0.35);
        float3 eye = sclera;
        eye = mix(eye, iris, irisMask);
        eye = mix(eye, float3(0.0), pupilMask);
        eye += hl * 0.85;
        col = mix(col, eye, eyeMask);
        col = mix(col, float3(0.0), rimShadow);

        float3 tearCol = mix(float3(0.55, 0.0, 0.03), float3(0.9, 0.1, 0.06), tears);
        col = mix(col, tearCol, tears);
        col += float3(0.4, 0.02, 0.0) * leadEdge;

        float vig = smoothstep(1.25, 0.25, rad);
        col *= mix(1.0, vig, clamp(VIGNETTE, 0.0, 1.5));
    } else {
        float3 bg = float3(0.91, 0.88, 0.85);
        bg -= float3(0.05, 0.06, 0.07) * fog * 0.35;
        col = bg;

        float stain = clamp(fog * FOG * (0.5 + 0.5 * pulse), 0.0, 1.0);
        col = mix(col, float3(0.46, 0.02, 0.05), stain * 0.5);
        col = mix(col, float3(0.5, 0.0, 0.04), glow * 0.3 * pulse * BLOOD);

        float3 sclera = mix(float3(0.86, 0.72, 0.70), float3(0.62, 0.06, 0.06), veinLine * VEINS * 0.7);
        float3 iris = mix(float3(0.06, 0.0, 0.0), float3(0.45, 0.02, 0.03), rays * IRIS);
        iris = mix(iris, float3(0.68, 0.08, 0.05), irisGrad * 0.45);
        float3 eye = sclera;
        eye = mix(eye, iris, irisMask);
        eye = mix(eye, float3(0.02, 0.0, 0.0), pupilMask);
        eye += hl * 0.35;
        col = mix(col, eye, eyeMask);
        col = mix(col, float3(0.22, 0.0, 0.02), rimShadow);

        float3 tearCol = mix(float3(0.5, 0.0, 0.03), float3(0.32, 0.0, 0.02), tears);
        col = mix(col, tearCol, tears);
        col = mix(col, float3(0.55, 0.0, 0.03), leadEdge * 0.6);

        float vig = smoothstep(1.3, 0.3, rad);
        col = mix(col, col * float3(0.72, 0.5, 0.5), (1.0 - vig) * clamp(VIGNETTE, 0.0, 1.5));
    }

    float sl = 0.5 + 0.5 * sin(fragCoord.y * 3.14159);
    col *= 1.0 - SCAN * 0.38 * sl;
    col += (hash(fragCoord + fract(T)) - 0.5) * GRAIN;

    col = max(col, float3(0.0));
    return half4(col.r, col.g, col.b, 1.0);
}
"""
