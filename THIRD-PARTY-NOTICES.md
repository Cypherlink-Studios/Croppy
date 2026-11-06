# Third-Party Notices & Dependency Attributions

Croppy utilizes open-source software libraries, platform APIs, and build tooling. This document acknowledges the third-party components integrated into or used by the project, along with their respective copyright holders, license terms, and notices.

---

## 1. Runtime & Shaded Dependencies

### SnakeYAML
- **Module:** `croppy-core`
- **Version:** `2.2`
- **License:** [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Copyright:** Copyright (C) 2008-2024 Andrey Somov, Alexander Maslov
- **Source:** [https://bitbucket.org/snakeyaml/snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml)
- **Notice:** Used for parsing crop species, recipes, and station YAML configuration files.

### Incendo Cloud Framework (`cloud-paper` & `cloud-annotations`)
- **Module:** `croppy-paper` (Relocated via Shadow to `dev.darkblade.croppy.libs.cloud`)
- **Version:** `2.0.1` / `2.1.0`
- **License:** [MIT License](https://opensource.org/licenses/MIT)
- **Copyright:** Copyright (C) 2024 Alexander Söderberg & Cloud Contributors
- **Source:** [https://github.com/Incendo/cloud](https://github.com/Incendo/cloud)
- **Notice:** Powers modern command parsing, permission routing, and tab-completion.

### GeantyRef
- **Module:** `croppy-paper` (Transitive dependency of Cloud, relocated to `dev.darkblade.croppy.libs.geantyref`)
- **Version:** `1.3.15`
- **License:** [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Copyright:** Copyright (C) 2017-2024 Wouter Coekaerts, GeantyRef Contributors
- **Source:** [https://github.com/leangen/geantyref](https://github.com/leangen/geantyref)
- **Notice:** Generic type reflection utility.

### Libby (libby-paper)
- **Module:** `croppy-paper` (Relocated via Shadow to `dev.darkblade.croppy.libs.libby`)
- **Version:** `1.3.2`
- **License:** [MIT License](https://opensource.org/licenses/MIT)
- **Copyright:** Copyright (C) 2019-2024 AlessioDP (Alessio De Parmenzi)
- **Source:** [https://github.com/AlessioDP/libby](https://github.com/AlessioDP/libby)
- **Notice:** Runtime library downloader used to fetch SQLite drivers dynamically.

---

## 2. Server Platform & Network APIs (Compile-Only / Provided)

### Paper API
- **Module:** `croppy-paper` (Compile-only / Provided by server runtime)
- **Version:** `1.21.8-R0.1-SNAPSHOT`
- **License:** [GNU General Public License v3.0 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html)
- **Copyright:** Copyright (C) 2015-2024 PaperMC Team
- **Source:** [https://github.com/PaperMC/Paper](https://github.com/PaperMC/Paper)
- **Notice:** Server platform API providing Bukkit/Paper event listeners and world interaction hooks.

### PacketEvents (`packetevents-spigot`)
- **Module:** `croppy-paper` (Compile-only / Provided by server plugin)
- **Version:** `2.14.0`
- **License:** [GNU Lesser General Public License v3.0 (LGPL-3.0)](https://www.gnu.org/licenses/lgpl-3.0.html) / [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Copyright:** Copyright (C) 2021-2024 retrooper & PacketEvents Contributors
- **Source:** [https://github.com/retrooper/packetevents](https://github.com/retrooper/packetevents)
- **Notice:** High-performance cross-version packet listener framework used for virtual 3D crop displays and station interaction box projections.

### Kyori Adventure
- **Module:** `croppy-paper` (Included via Paper API)
- **License:** [MIT License](https://opensource.org/licenses/MIT)
- **Copyright:** Copyright (C) 2017-2024 KyoriPowered
- **Source:** [https://github.com/KyoriPowered/adventure](https://github.com/KyoriPowered/adventure)
- **Notice:** Rich UI component, MiniMessage formatting, and chat rendering library.

---

## 3. Dynamic Database Drivers (Loaded via Libby)

### HikariCP
- **Module:** `croppy-paper` (Compile-only; loaded dynamically if SQLite storage mode is selected)
- **Version:** `7.1.0`
- **License:** [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Copyright:** Copyright (C) 2013-2024 Brett Wooldridge
- **Source:** [https://github.com/brettwooldridge/HikariCP](https://github.com/brettwooldridge/HikariCP)
- **Notice:** High-performance JDBC connection pooling library.

### SQLite JDBC
- **Module:** `croppy-paper` (Compile-only; loaded dynamically if SQLite storage mode is selected)
- **Version:** `3.49.1.0`
- **License:** [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Copyright:** Copyright (C) 2006-2024 Taro L. Saito (Xerial Project)
- **Source:** [https://github.com/xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)
- **Notice:** SQLite database driver for local persistence.

---

## 4. Test Frameworks & Build Tooling

### JUnit 5 (`junit-jupiter` & `junit-bom`)
- **Modules:** `croppy-api`, `croppy-core`, `croppy-paper` (Test scope)
- **Version:** `5.10.2`
- **License:** [Eclipse Public License - v 2.0 (EPL-2.0)](https://www.eclipse.org/legal/epl-2.0/)
- **Copyright:** Copyright (C) 2015-2024 JUnit Team
- **Source:** [https://github.com/junit-team/junit5](https://github.com/junit-team/junit5)

### Shadow Gradle Plugin
- **Scope:** Build Gradle plugin (`com.gradleup.shadow`)
- **Version:** `8.3.7`
- **License:** [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- **Copyright:** Copyright (C) 2014-2024 John Engelman, GradleUp Contributors
- **Source:** [https://github.com/GradleUp/shadow](https://github.com/GradleUp/shadow)

---

## 5. Companion Resource Pack Assets

- **Location:** `resourcepack/`
- **License:** [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)](resourcepack/LICENSE)
- **Copyright:** Copyright (C) 2026 Cypherlink Studios & DarkBladeDev
- **Notice:** Contains custom 3D item models, block display models, UI fonts, and pixel art textures for Minecraft 1.21.8+.
