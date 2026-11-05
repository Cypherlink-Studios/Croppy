---
name: mc-rp-shaders-1218
description: Develops and adapts GLSL Core and Post-Processing shaders for Minecraft 1.21.8 using Uniform Blocks (UBOs), #moj_import conventions, and split spherical/cylindrical fog.
version: "1.0"
---

# Minecraft 1.21.8 GLSL Shaders (Uniform Blocks Architecture)

## 1. Scope & Objective
Guide the development, debugging, and migration of GLSL Core shaders (`shaders/core/`) and Post-Processing shaders (`shaders/post/`) under the **Uniform Blocks (UBOs)** architecture introduced in Minecraft 1.21.6 and stabilized in 1.21.8.

---

## 2. Inviolable Constraints

1. **PROHIBITED LOOSE UNIFORMS:** Standard built-in uniforms such as `uniform mat4 ModelViewMat;` or `uniform mat4 ProjMat;` are strictly prohibited. In 1.21.6+, all built-in uniforms are grouped into `layout(std140) uniform` blocks imported via `#moj_import`.
2. **ACTIVE RENDERTYPE PROGRAMS (1.21.8 vs 26.x):** In 1.21.8, the classic rendertype programs (`rendertype_text`, `rendertype_entity_cutout`, `rendertype_cutout_mipped`, etc.) still exist. They were not consolidated into `core/text` until 26.2.
3. **SPLIT FOG SYSTEM (1.21.6+):** `FOG_IS_SKY` was removed. Vertex shaders must calculate both spherical (ambient) and cylindrical (render distance) fog distances, passing both to fragment shaders.

---

## 3. Standard `#moj_import` Blocks

Import uniform blocks from `assets/minecraft/shaders/include/`:

| Import Directive | Exposed Block / Variables |
|---|---|
| `#moj_import <minecraft:projection.glsl>` | `ProjMat` |
| `#moj_import <minecraft:dynamictransforms.glsl>` | `ModelViewMat`, `ColorModulator`, `GlintAlpha`, `ModelOffset` |
| `#moj_import <minecraft:globals.glsl>` | `ScreenSize`, `GameTime`, `MenuBlurRadius` |
| `#moj_import <minecraft:fog.glsl>` | `Fog` block, `fog_spherical_distance(pos)`, `fog_cylindrical_distance(pos)` |
| `#moj_import <minecraft:light.glsl>` | `Light0_Direction`, `Light1_Direction`, `minecraft_mix_light(...)` |

---

## 4. Canonical Vertex Shader (`shaders/core/rendertype_entity_cutout.vsh`)

```glsl
#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
}
```

---

## 5. Canonical Fragment Shader (`shaders/core/rendertype_entity_cutout.fsh`)

```glsl
#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    float vertexDistance = max(sphericalVertexDistance, cylindricalVertexDistance);
    fragColor = linear_fog(color, vertexDistance, Fog.start, Fog.end, Fog.color);
}
```

For detailed uniform memory alignment, see `references/ubo_imports_cheatsheet.md`.
