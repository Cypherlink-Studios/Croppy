# Tasks: Add Licensing, Copyright, Contribution, and Legal Documentation

## 1. Core License & Legal Overview

- [x] 1.1 Create root `LICENSE` with the full canonical, unmodified GNU General Public License v3.0 text (`GPL-3.0-or-later`), verifying file creation at the project root.
- [x] 1.2 Create `LICENSING.md` detailing the dual-licensing framework, GPLv3 permissions/obligations, commercial alternative overview, official vs. third-party addon boundaries, and practical FAQ, verifying document links and "not legal advice" disclaimer.

## 2. Commercial & Copyright Terms

- [x] 2.1 Create `COMMERCIAL-LICENSE.md` as an explicit draft/template with prominent warning banners, placeholder fields, and clear statements preserving existing GPL rights, verifying placeholder formats.
- [x] 2.2 Create `COPYRIGHT.md` declaring copyright notices for Cypherlink Studios & DarkBladeDev with explicit reservations on legal personhood, notice preservation rules, and third-party pointers, verifying file creation.

## 3. Contribution & Third-Party Attributions

- [x] 3.1 Create `CONTRIBUTING.md` defining PR workflows, coding conventions, originality representations, and CLA readiness status, verifying file creation.
- [x] 3.2 Create `CLA.md` as a draft Contributor License Agreement preserving contributor copyright while enabling dual licensing, verifying draft warnings and placeholder fields.
- [x] 3.3 Create `THIRD-PARTY-NOTICES.md` with an exhaustive inventory of dependencies (SnakeYAML, Paper API, PacketEvents, Libby, Cloud, HikariCP, SQLite JDBC, JUnit 5, Kyori Adventure, CC BY-NC-SA 4.0 resource pack), verifying licenses and notices.

## 4. Quality Assurance & Verification

- [x] 4.1 Verify cross-references and relative links across `README.md`, `LICENSE`, `LICENSING.md`, `COMMERCIAL-LICENSE.md`, `COPYRIGHT.md`, `CONTRIBUTING.md`, `CLA.md`, and `THIRD-PARTY-NOTICES.md`.
- [x] 4.2 Execute `./gradlew.bat test` to confirm build integrity and test pass rate (0 failures) satisfying AGENTS.md pre-commit quality gate.
