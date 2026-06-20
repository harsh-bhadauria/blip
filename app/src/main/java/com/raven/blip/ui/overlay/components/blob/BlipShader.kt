package com.raven.blip.ui.overlay.components.blob

import com.raven.blip.ui.theme.*
import org.intellij.lang.annotations.Language

@Language("AGSL")
const val BLIP_SHADER = """
    uniform float2 resolution;
    uniform float time;
    uniform float urgency;
    
    // Simplex 2D noise
    vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }
    float snoise(vec2 v){
      const vec4 C = vec4(0.211324865405187, 0.366025403784439,
               -0.577350269189626, 0.024390243902439);
      vec2 i  = floor(v + dot(v, C.yy) );
      vec2 x0 = v -   i + dot(i, C.xx);
      vec2 i1;
      i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
      vec4 x12 = x0.xyxy + C.xxzz;
      x12.xy -= i1;
      i = mod(i, 289.0);
      vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
      + i.x + vec3(0.0, i1.x, 1.0 ));
      vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
        dot(x12.zw,x12.zw)), 0.0);
      m = m*m ;
      m = m*m ;
      vec3 x = 2.0 * fract(p * C.www) - 1.0;
      vec3 h = abs(x) - 0.5;
      vec3 ox = floor(x + 0.5);
      vec3 a0 = x - ox;
      m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
      vec3 g;
      g.x  = a0.x  * x0.x  + h.x  * x0.y;
      g.yz = a0.yz * x12.xz + h.yz * x12.yw;
      return 130.0 * dot(m, g);
    }

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        float2 p = uv * 2.0 - 1.0;
        float dist = length(p);
        
        // Create organic wobbly shape
        float speed = time * (0.2 + urgency * 0.8);
        float noiseScale = 1.0 + urgency * 0.5;
        float n = snoise(p * noiseScale + speed) * 0.08 * (0.2 + urgency * 0.4);
        
        float radius = 0.8 + n;
        float alpha = smoothstep(radius, radius - 0.05, dist);
        
        // Color gradient from base to hot based on urgency
        half3 colorBase = half3(0.65, 0.89, 0.63); // #A6E3A1
        half3 colorHot = half3(0.95, 0.53, 0.53);  // #F38BA8
        half3 finalColor = mix(colorBase, colorHot, urgency);
        
        return half4(finalColor * alpha, alpha);
    }
"""
