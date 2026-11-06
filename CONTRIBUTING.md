# Contributing to Croppy

Thank you for your interest in contributing to **Croppy**! We welcome bug reports, feature discussions, documentation enhancements, and pull requests from the community.

Please take a few moments to review these guidelines before submitting contributions.

---

## 1. Development Principles & Code Standards

Croppy enforces strict architectural boundaries to maintain high code quality and testability. All code contributions must adhere to the conventions documented in [`AGENTS.md`](AGENTS.md):

- **Hexagonal Architecture (Zero Bukkit in Core)**:
  - `croppy-api`: Pure Java 21 records, interfaces, and DTOs with **zero external dependencies**.
  - `croppy-core`: Pure Java 21 domain engine, simulation, codecs, and registries (depends only on `croppy-api` and SnakeYAML). Never import Bukkit or Paper classes into Core.
  - `croppy-paper`: Platform adapter handling Bukkit/Paper events, PacketEvents displays, and SQLite/PDC I/O.
- **Modern Java 21 Features**: Use immutable `record`s for data carriers, coordinates, and configs. Enforce null-safety using standard Java 21 checks (`Objects.requireNonNull`).
- **English-Only Code & Comments**: All source code, javadocs, comments, logging messages, and MiniMessage strings must be written in Standard English.
- **Automated Verification Gate**: Before submitting any pull request or finalizing changes, all tests must pass cleanly:
  ```bash
  ./gradlew test
  ```

---

## 2. Originality & Intellectual Property Rules

To ensure the legal integrity of the codebase, all contributors must adhere to the following IP rules:

1. **Original Work**: By submitting a pull request, you represent and warrant that the code, documentation, or assets submitted are your original creation, or that you have sufficient legal rights and permissions to license them to the project.
2. **Third-Party Code & Snippets**:
   - Do **not** copy and paste proprietary code, decompiled Minecraft source (NMS/CraftBukkit), or snippets from other plugins without explicit authorization and compatible open-source licensing.
   - If including permissive open-source code (e.g., MIT, Apache 2.0, BSD), you must retain the original copyright notice and license headers, and notify maintainers in the pull request description.
3. **AI-Generated Code**: If you utilize AI coding tools or LLMs to assist your contributions, you remain personally responsible for reviewing the code to ensure it does not infringe third-party intellectual property or violate licensing boundaries.
4. **Visual & Audio Assets**: Models, textures, fonts, and sound effects submitted for the companion resource pack must be original works compatible with the **[CC BY-NC-SA 4.0](resourcepack/LICENSE)** license.

---

## 3. Contributor License Agreement (CLA) Status

Croppy operates under a dual-licensing strategy ([`LICENSING.md`](LICENSING.md)).

- **Proposed CLA**: A draft Contributor License Agreement is available for review in [`CLA.md`](CLA.md). Under this agreement, contributors retain copyright ownership of their submissions while granting the project the rights necessary to distribute under GPLv3 and under alternative commercial terms.
- **Current Operational Status**: An automated CLA signing mechanism is currently in preparation. Contributions are governed by the standard open-source inbound/outbound licensing principle (contributions submitted to a GPLv3 repository are licensed under GPL-3.0-or-later) until the formal CLA workflow is activated.

---

## 4. How to Submit Contributions

1. **Check Issues**: Browse existing issues or start a discussion to confirm that your proposed change aligns with project roadmap goals.
2. **Fork & Branch**: Fork the repository and create a feature branch (`feat/your-feature-name` or `fix/issue-description`).
3. **OpenSpec Governance**: For significant architectural features or mechanics, formulate an OpenSpec change proposal following project practices.
4. **Write Tests**: Provide JUnit 5 unit tests covering new simulation logic, edge cases, and codecs.
5. **Open Pull Request**: Open a clear PR detailing:
   - What problem is solved.
   - Which components/modules are affected.
   - Confirmation that `./gradlew test` passes with 0 failures.

---

## 5. Helpful Links

- Architecture & Standards: [`AGENTS.md`](AGENTS.md)
- Public License: [`LICENSE`](LICENSE)
- Licensing Model & FAQ: [`LICENSING.md`](LICENSING.md)
- Draft Contributor Agreement: [`CLA.md`](CLA.md)
- Third-Party Notices: [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)
