# Tasks: Plugin Documentation Architecture and Metadata

## 1. Documentation Metadata & Directories

- [x] 1.1 Create `docs/metadata.yml` adhering to DarkBladeDev `github-docs-loader` schema with English descriptions and verify YAML structure
- [x] 1.2 Scaffold directories `docs/en/` and `docs/es/`, migrate `docs/localization.md` to `docs/en/localization.md` and `docs/es/localization.md`, and delete the unorganized root `docs/localization.md`

## 2. English Documentation Suite (`docs/en/`)

- [x] 2.1 Author `docs/en/index.md` (overview, highlights, gameplay loop, terminology)
- [x] 2.2 Author `docs/en/getting-started.md` (installation, PacketEvents dependency, first crop & harvest walkthrough)
- [x] 2.3 Author `docs/en/configuration.md` (`config.yml` breakdown, persistence backends, ticker intervals, rate limits)
- [x] 2.4 Author `docs/en/commands-and-permissions.md` (Cloud 2 `/croppy` command tree, arguments, permission nodes)
- [x] 2.5 Author `docs/en/crops-and-species.md` (YAML crop definitions, growth stages, requirements, models)
- [x] 2.6 Author `docs/en/growth-and-environment.md` (growth simulation, light, temperature, hydration, greenhouse rules)
- [x] 2.7 Author `docs/en/soil-and-fertilizers.md` (NPK nutrients, fertilizer items, application and depletion mechanics)
- [x] 2.8 Author `docs/en/watering-mechanics.md` (watering cans, tiers, moisture evaporation, refill sources)
- [x] 2.9 Author `docs/en/workstations-and-cooking.md` (cooking pot, skillet, physical interaction, cooking rules, burn mechanics)
- [x] 2.10 Author `docs/en/heat-sources-and-fuel.md` (stoves, fuels, burn times, heat curves, thermal maintenance)
- [x] 2.11 Author `docs/en/recipe-discovery.md` (discovery engine, knowledge states, hints, unlock criteria)
- [x] 2.12 Author `docs/en/culinary-compendium.md` (cookbook GUI, category navigation, secret recipes, recipe book item)
- [x] 2.13 Author `docs/en/gui-menus.md` (9xN matrix inventory configuration, slot mapping, conditional items, click actions)
- [x] 2.14 Author `docs/en/items-and-tools.md` (custom items, tools, PDC tags, item registration)

## 3. Spanish Documentation Suite (`docs/es/`)

- [x] 3.1 Author `docs/es/index.md` (visión general, ciclo de juego, terminología clave)
- [x] 3.2 Author `docs/es/getting-started.md` (instalación, dependencia PacketEvents, guía de inicio)
- [x] 3.3 Author `docs/es/configuration.md` (desglose de `config.yml`, persistencia, temporizadores)
- [x] 3.4 Author `docs/es/commands-and-permissions.md` (árbol de comandos `/croppy` y permisos)
- [x] 3.5 Author `docs/es/crops-and-species.md` (definición de cultivos YAML, etapas, requisitos)
- [x] 3.6 Author `docs/es/growth-and-environment.md` (simulación de crecimiento, luz, temperatura, humedad, invernaderos)
- [x] 3.7 Author `docs/es/soil-and-fertilizers.md` (nutrientes N-P-K, fertilizantes, aplicación al suelo)
- [x] 3.8 Author `docs/es/watering-mechanics.md` (regaderas, niveles, evaporación, fuentes de agua)
- [x] 3.9 Author `docs/es/workstations-and-cooking.md` (olla de cocina, sartén, interacción 3D, quemado)
- [x] 3.10 Author `docs/es/heat-sources-and-fuel.md` (estufas, combustibles, tiempos de combustión, disipación)
- [x] 3.11 Author `docs/es/recipe-discovery.md` (motor de descubrimiento, estados de conocimiento, pistas, criterios)
- [x] 3.12 Author `docs/es/culinary-compendium.md` (libro de recetas GUI, navegación, recetas secretas)
- [x] 3.13 Author `docs/es/gui-menus.md` (menús matriciales 9xN, mapeo de slots, acciones)
- [x] 3.14 Author `docs/es/items-and-tools.md` (ítems custom, herramientas, etiquetas PDC)

## 4. Parity & Verification Checks

- [x] 4.1 Verify exact 1:1 slug parity between `docs/en/` and `docs/es/` (exactly 15 files in each locale directory)
- [x] 4.2 Verify Starlight frontmatter (`title`, `description`, `sidebar.order`) in all 30 markdown files
- [x] 4.3 Verify that the full Gradle test suite `./gradlew.bat test` passes with zero regressions
