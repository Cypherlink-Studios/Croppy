---
name: mc-rp-gui-fonts-audio-1218
description: Designs GUI textures with 9-slicing sprites, custom fonts (bitmap and negative space providers for borderless chest menus/actionbars), waypoint styles, atlases (gui.json with mob_effects), and directional mono audio (.ogg) for Minecraft 1.21.8.
version: "1.0"
---

# Minecraft 1.21.8 GUI Sprites, Custom Fonts, Atlases & Audio

## 1. Scope & Objective
Standardize user interface design, custom inventory overlays via negative-space fonts, waypoint styling for locator bars, texture atlas configuration, and directional audio under Minecraft Java 1.21.8 standards.

---

## 2. Inviolable Constraints

1. **ATLAS RESTRUCTURING (1.21.6+):** The `mob_effects.json` atlas was permanently REMOVED. All status effect icons (`textures/mob_effect/`) are now declared inside `assets/minecraft/atlases/gui.json`.
2. **SOUND RELOCATIONS (1.21.6+):** Audio files for `glow_squid`, `squid`, `guardian`, `horse`, `rabbit`, and `pufferfish` moved from directory `entity/` to directory `mob/`.
3. **SPATIAL MONO AUDIO:** Sound files intended for positional playback in the Minecraft world MUST be encoded in **Mono** (1 channel) Ogg Vorbis at 44.1 kHz. Stereo files play in the player's head without directional 3D attenuation.
4. **NO 'object' TEXT COMPONENT:** Do not attempt to use the `object` text component for inline sprites; that capability was introduced in 1.21.9. In 1.21.8, custom GUI and HUD sprites MUST be displayed using `bitmap` and `space` font providers.

---

## 3. GUI Sprites & 9-Slicing (`textures/gui/sprites/`)

UI elements are standalone PNG images. Scaling behavior is defined in `<sprite_name>.png.mcmeta`:

### 9-Slice Scaling (`nine_slice`):
Enables dynamic scaling of menus, dialogs, and borders without corner distortion:
```json
{
  "gui": {
    "scaling": {
      "type": "nine_slice",
      "width": 256,
      "height": 256,
      "stretch_inner": false,
      "border": {
        "left": 4,
        "top": 4,
        "right": 4,
        "bottom": 4
      }
    }
  }
}
```

Other scaling types:
- `"type": "stretch"`: Stretches the image across the allotted area.
- `"type": "tile"`: Repeats the image across the area using `"width"` and `"height"`.

---

## 4. Custom Fonts & Negative-Space GUI Backgrounds (`assets/<namespace>/font/`)

To render borderless custom GUI backgrounds (e.g. recipe books, cooking station layouts) behind standard Chest GUIs:

1. Configure a `space` font provider with negative advances (powers of two: `-1`, `-2`, `-4`, `-8`, `-16`, `-32`, `-64`, `-128`).
2. Configure a `bitmap` font provider referencing the full menu texture (e.g., 256x256).
3. In the inventory title, combine negative advance characters to move the cursor to the top-left boundary of the screen, followed by the bitmap glyph:
   `\uF808\uF808\uF801\uE001`

### Canonical `assets/<namespace>/font/default.json`:
```json
{
  "providers": [
    {
      "type": "space",
      "advances": {
        "\uF801": -1,
        "\uF802": -2,
        "\uF804": -4,
        "\uF808": -8,
        "\uF810": -16,
        "\uF820": -32,
        "\uF840": -64,
        "\uF880": -128
      }
    },
    {
      "type": "bitmap",
      "file": "croppy:font/compendium_menu.png",
      "ascent": 14,
      "height": 166,
      "chars": ["\uE001"]
    }
  ]
}
```

Consult `references/unicode_font_advances.md` for precomputed offset combinations.

---

## 5. Waypoint Styles (`assets/<namespace>/waypoint_style/<name>.json`)

Introduced in 1.21.6 to customize Locator Bar icons:

```json
{
  "near_distance": 128,
  "far_distance": 512,
  "sprites": [
    "croppy:hud/waypoint_near",
    "croppy:hud/waypoint_mid",
    "croppy:hud/waypoint_far"
  ]
}
```
Sprites are located at `assets/<namespace>/textures/gui/sprites/hud/locator_bar_dot/<sprite>.png`.

---

## 6. Audio Event Definitions (`assets/<namespace>/sounds.json`)

```json
{
  "station.cooking_pot.boil": {
    "category": "block",
    "subtitle": "subtitles.croppy.pot_boil",
    "sounds": [
      {
        "name": "croppy:mob/cooking_pot/boil_loop",
        "volume": 0.8,
        "pitch": 1.0,
        "attenuation_distance": 16
      }
    ]
  }
}
```
