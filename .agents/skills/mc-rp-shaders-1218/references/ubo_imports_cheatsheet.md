# Uniform Blocks (UBOs) Cheatsheet — Minecraft 1.21.8

This document summarizes the `std140` memory layout and structs exposed by `#moj_import` in Minecraft Java 1.21.8.

---

## 1. Matrix and Transform Uniform Blocks

### `<minecraft:projection.glsl>`
Exposes the camera projection matrix:
```glsl
layout(std140) uniform Projection {
    mat4 ProjMat;
};
```

### `<minecraft:dynamictransforms.glsl>`
Exposes model-view transformations, item color tinting, and glint modulation:
```glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec4 ModelOffset;
    float GlintAlpha;
};
```

---

## 2. Environment & Lighting Blocks

### `<minecraft:globals.glsl>`
Global screen and runtime state:
```glsl
layout(std140) uniform Globals {
    vec2 ScreenSize;
    float GameTime;
    float MenuBlurRadius;
};
```

### `<minecraft:fog.glsl>`
Fog configuration and distance calculation functions:
```glsl
layout(std140) uniform Fog {
    vec4 color;
    float start;
    float end;
    int shape; // 0 = spherical, 1 = cylindrical
} Fog;

float fog_spherical_distance(vec3 pos);
float fog_cylindrical_distance(vec3 pos);
vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor);
```

### `<minecraft:light.glsl>`
Sun/moon directional lighting:
```glsl
layout(std140) uniform Lighting {
    vec3 Light0_Direction;
    vec3 Light1_Direction;
};

vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color);
```
