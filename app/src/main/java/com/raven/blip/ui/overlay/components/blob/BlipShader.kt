package com.raven.blip.ui.overlay.components.blob

import org.intellij.lang.annotations.Language

/**
 * AGSL shader for the Blip blob.
 *
 * Uniforms:
 *   resolution  – pixel dimensions of the draw area
 *   time        – elapsed seconds (drives animation)
 *   urgency     – 0..1, task-pressure level
 *   visualState – encoded BlobVisualState ordinal (0=IDLE … 6=HIDING)
 *   morphOut    – 0..1, shrink-away progress for event transitions
 */
@Language("AGSL")
const val BLIP_SHADER = """
    uniform float2 resolution;
    uniform float time;
    uniform float urgency;
    uniform float visualState;
    uniform float morphOut;

    // ── Simplex 2D noise ────────────────────────────────────────────────
    vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }
    float snoise(vec2 v){
      const vec4 C = vec4(0.211324865405187, 0.366025403784439,
               -0.577350269189626, 0.024390243902439);
      vec2 i  = floor(v + dot(v, C.yy));
      vec2 x0 = v -   i + dot(i, C.xx);
      vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
      vec4 x12 = x0.xyxy + C.xxzz;
      x12.xy -= i1;
      i = mod(i, 289.0);
      vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))
                      + i.x + vec3(0.0, i1.x, 1.0));
      vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
        dot(x12.zw,x12.zw)), 0.0);
      m = m*m;
      m = m*m;
      vec3 x = 2.0 * fract(p * C.www) - 1.0;
      vec3 h = abs(x) - 0.5;
      vec3 ox = floor(x + 0.5);
      vec3 a0 = x - ox;
      m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);
      vec3 g;
      g.x  = a0.x  * x0.x  + h.x  * x0.y;
      g.yz = a0.yz * x12.xz + h.yz * x12.yw;
      return 130.0 * dot(m, g);
    }

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        float2 p  = uv * 2.0 - 1.0;        // -1..1 centred

        // ── State-driven deformation ────────────────────────────────────
        // IDLE(0)  NUDGING(1)  SPEAKING(2)  LISTENING(3)  THINKING(4)  CELEBRATING(5)  HIDING(6)

        float scaleX = 1.0;
        float scaleY = 1.0;
        float wobbleExtra = 0.0;

        // NUDGING: horizontal wiggle
        if (visualState > 0.5 && visualState < 1.5) {
            wobbleExtra = sin(time * 8.0) * 0.04;
        }
        // SPEAKING: slight vertical stretch, lean upward
        if (visualState > 1.5 && visualState < 2.5) {
            scaleY = 1.08;
            scaleX = 0.94;
        }
        // LISTENING: wider, slightly squished — settled, attentive
        if (visualState > 2.5 && visualState < 3.5) {
            scaleX = 1.05;
            scaleY = 0.93;
        }
        // THINKING: gentle rhythmic pulse
        if (visualState > 3.5 && visualState < 4.5) {
            float pulse = sin(time * 3.0) * 0.03;
            scaleX = 1.0 + pulse;
            scaleY = 1.0 + pulse;
        }

        p.x = p.x / scaleX + wobbleExtra;
        p.y = p.y / scaleY;

        float dist = length(p);

        // ── Organic wobble ──────────────────────────────────────────────
        float speed = time * (0.25 + urgency * 0.6);
        float noiseScale = 1.2 + urgency * 0.4;
        float n = snoise(p * noiseScale + speed) * 0.07 * (0.3 + urgency * 0.3);

        // ── Shape ───────────────────────────────────────────────────────
        float baseRadius = 0.78;
        // morphOut shrinks the blob for event transitions
        float radius = (baseRadius + n) * (1.0 - morphOut);
        float alpha  = smoothstep(radius, radius - 0.06, dist);

        // ── Coloring: white body + urgency-tinted underbelly ────────────
        // Base: near-white with very slight cool tint
        half3 bodyColor = half3(0.94, 0.95, 0.97);

        // Underbelly gradient: stronger toward the bottom of the blob
        // Map p.y within the blob to a 0..1 underbelly factor
        // p.y goes from -1 (top) to +1 (bottom) in our coord system
        float bellyFactor = smoothstep(-0.1, 0.7, p.y);  // ramps up in bottom 40%
        bellyFactor *= bellyFactor;  // softer falloff

        // Underbelly color shifts with urgency
        half3 bellyCool = half3(0.72, 0.76, 0.94);   // soft blue-lavender
        half3 bellyWarm = half3(0.94, 0.62, 0.68);   // soft coral-pink
        half3 bellyColor = mix(bellyCool, bellyWarm, urgency);

        // Blend body and underbelly
        half3 finalColor = mix(bodyColor, bellyColor, bellyFactor * 0.55);

        // Subtle rim highlight at the top for depth
        float rimFactor = smoothstep(0.3, -0.5, p.y) * smoothstep(radius - 0.15, radius - 0.06, dist);
        finalColor = mix(finalColor, half3(1.0, 1.0, 1.0), rimFactor * 0.15);

        return half4(finalColor * alpha, alpha);
    }
"""
