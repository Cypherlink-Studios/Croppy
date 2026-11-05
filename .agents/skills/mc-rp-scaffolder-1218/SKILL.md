---
name: mc-rp-scaffolder-1218
description: Initializes, structures, and audits Minecraft Java 1.21.8 Resource Packs (pack_format 64). Use when scaffolding a new resource pack, configuring pack.mcmeta, handling overlays, or auditing Strict Mode JSON and valid asset paths.
version: "1.0"
---

# Minecraft 1.21.8 Resource Pack Scaffolder & Auditor

## 1. Scope & Objective
Establish, structure, and audit the foundational files and directory tree for a **Minecraft Java 1.21.8** Resource Pack (`pack_format: 64`). Ensure complete compliance with the Strict JSON parsing mode introduced in 1.21.6+ and validate namespaced asset paths.

---

## 2. Strict Constraints

- **STRICT PACK FORMAT:** For Minecraft 1.21.8, `pack_format` MUST be `64` (1.21.7 and 1.21.8 share pack format 64).
- **NO DECIMAL FORMATS:** Do NOT use `min_format` or `max_format` as replacements for `pack_format`. Decimal and floating format keys are exclusive to 1.21.9+ (format 69.0+). Use standard integer `pack_format: 64` and optional `supported_formats: {"min_inclusive": 63, "max_inclusive": 64}`.
- **STRICT JSON PARSER ENFORCEMENT:** Minecraft 1.21.8 parses `.json` and `.mcmeta` in strict mode. Trailing commas (`,`), single-line comments (`//`), multi-line comments (`/* */`), or unquoted keys cause the entire file to fail loading.
- **STRICT ASSET PATH REGEX:** Every directory and filename under `assets/` MUST strictly match `^[a-z0-9_.-]+$`. No uppercase characters, no spaces, no non-ASCII symbols.

---

## 3. Directory Layout (assets/<namespace>/)

```
<resourcepack-root>/
├── pack.mcmeta                       # pack_format 64 and description
├── pack.png                          # 128x128 or 256x256 PNG pack icon
└── assets/
    └── <namespace>/
        ├── items/                    # Item model definitions (Added in 1.21.4)
        ├── equipment/                # Armor & mount layers (Added in 1.21.2)
        ├── waypoint_style/           # Locator bar styles (Added in 1.21.6)
        ├── blockstates/              # Block state to model mappings
        ├── models/
        │   ├── block/                # Block 3D models (continuous angle -45 to 45)
        │   └── item/                 # Item 3D models (continuous angle -45 to 45)
        ├── textures/
        │   ├── block/
        │   ├── item/
        │   ├── entity/
        │   │   └── equipment/        # Armor layers (humanoid, happy_ghast_body, etc.)
        │   ├── gui/
        │   │   └── sprites/          # 9-slice and scalable UI sprites
        │   └── font/
        ├── atlases/                  # Atlas declarations (mob_effects merged into gui.json)
        ├── font/                     # Bitmap and space negative advance font providers
        ├── lang/                     # en_us.json, es_es.json, etc.
        ├── sounds/                   # Spatial mono .ogg audio files
        ├── sounds.json               # Sound event mappings
        └── shaders/                  # Core & post GLSL shaders (Uniform Blocks)
```

---

## 4. Canonical `pack.mcmeta` Template

```json
{
  "pack": {
    "pack_format": 64,
    "supported_formats": {
      "min_inclusive": 63,
      "max_inclusive": 64
    },
    "description": [
      {"text": "Croppy Companion Pack ", "color": "#55FF55"},
      {"text": "(1.21.8)", "color": "#888888"}
    ]
  }
}
```

---

## 5. Automated Audit & Quality Gate

Run the bundled Python validator against the resource pack directory to verify strict JSON compliance, asset path naming conventions, and obsolete patterns:

```bash
python .agents/skills/mc-rp-scaffolder-1218/scripts/audit_pack_1218.py <path-to-resourcepack>
```

Always execute this audit script before committing changes to resource pack assets.
