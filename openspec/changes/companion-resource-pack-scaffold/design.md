# Design: Companion Resource Pack Scaffold

## Context

See `proposal.md` for business and functional motivation. Croppy's `item-registry` and Paper adapter generate items with the native `item_model` Data Component (`Key itemModel`), and station virtual displays use PacketEvents to render item displays. To enable in-game rendering, a companion resource pack adhering to Minecraft Java 1.21.8 (`pack_format: 64`) must be maintained.

## Goals / Non-Goals

**Goals:**
- Establish the `resourcepack/` asset directory in the Croppy project root.
- Author canonical `pack.mcmeta` (format 64) and create a basic pack icon.
- Scaffold the modern asset directory structure under `assets/croppy/`.
- Provide syntactically valid modern item definitions in `assets/croppy/items/` for all registered Croppy items, referencing fallback vanilla models until custom 3D textures are drawn.
- Implement negative-space font advances in `assets/croppy/font/default.json`.
- Validate the entire pack using `audit_pack_1218.py`.

**Non-Goals:**
- Authoring final custom 16x16 / 32x32 pixel textures or 3D Blockbench models in this scaffolding change (deferred to iterative asset creation).
- Modifying server-side Java code in `croppy-api`, `croppy-core`, or `croppy-paper`.
- Publishing zip archives to external download hosts.

## Decisions

### Decision 1: In-Repository Companion Pack (`resourcepack/`)
- **Rationale**: Keeping the companion pack inside the plugin repository enables synchronized commits between YAML item declarations (e.g. `items/watering_cans/copper_can.yml`) and their corresponding resource pack item definitions (`assets/croppy/items/copper_can.json`).
- **Alternative Considered**: External repository. Rejected due to version drift and desynchronization risks during development.

### Decision 2: Modern `assets/<ns>/items/` Architecture with Vanilla Fallbacks
- **Rationale**: Minecraft 1.21.4+ removed support for `overrides` inside `models/item/`. By authoring modern `items/<item>.json` with vanilla fallbacks (e.g. `minecraft:item/iron_hoe` for watering cans, `minecraft:item/knowledge_book` for the recipe book), the resource pack is immediately testable without triggering purple-and-black missing texture models.
- **Alternative Considered**: Empty models or missing fallbacks. Rejected because it breaks client rendering.

### Decision 3: Standard Powers-of-Two Negative Space Fonts
- **Rationale**: Using private use area Unicode points `\uF801` (-1px) through `\uF880` (-128px) enables binary combination offsets for any pixel distance in Chest GUI inventory titles.

### Decision 4: Automated Pre-Commit Audit Gate
- **Rationale**: Minecraft 1.21.8 enforces Strict JSON parsing. Integrating `audit_pack_1218.py` ensures any trailing comma, comment, or uppercase filename is flagged immediately.

## Risks / Trade-offs

- **[Risk] Items show vanilla textures until custom art is added**  
  → *Mitigation*: Fallbacks are explicit and deliberate; custom geometries can be dropped in incrementally without schema changes.
- **[Risk] Path separators on Windows**  
  → *Mitigation*: Asset paths must exclusively use forward slashes (`/`), and `audit_pack_1218.py` normalizes separators during cross-platform validation.
