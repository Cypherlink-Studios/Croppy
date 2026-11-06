# Croppy — Licensing & Legal Architecture

> **Notice:** This document provides an explanation of the licensing model, permissions, obligations, and ecosystem policies governing the Croppy repository. It is provided for informational and administrative purposes only and **does not constitute legal advice**.

---

## 1. Dual-Licensing Strategy Overview

Croppy adopts a **dual-licensing framework** designed to balance open collaboration, community transparency, and long-term project sustainability:

```
+---------------------------------------------------------------------------------+
|                         CROPPY DUAL-LICENSING MODEL                             |
+---------------------------------------------------------------------------------+
|                                                                                 |
|   PUBLIC CODEBASE                               COMMERCIAL PATHWAY (Reserved)   |
|   +---------------------------------------+     +-----------------------------+ |
|   | - License: GPL-3.0-or-later           |     | - Commercial License Terms  | |
|   | - Modules: croppy-api, core, paper    |     | - Official Proprietary Addons| |
|   | - Terms: Copyleft, source available   |     | - Bespoke Commercial Rights | |
|   +---------------------------------------+     +-----------------------------+ |
|                       \                             /                           |
|                        v                           v                            |
|                     CONTRIBUTOR LICENSE AGREEMENT (CLA)                         |
|           (Contributors retain copyright; grant dual-licensing rights)          |
+---------------------------------------------------------------------------------+
```

1. **Public Codebase (Open Source)**: The public source code for the entire Croppy project—including `croppy-api`, `croppy-core`, and `croppy-paper`—is licensed under the **GNU General Public License v3.0 or later** ([`GPL-3.0-or-later`](LICENSE)).
2. **Commercial Alternative (Reserved)**: The copyright holders reserve the right to offer separate commercial licensing terms for specific downstream integrations, proprietary enterprise deployments, or official commercial addons. See [`COMMERCIAL-LICENSE.md`](COMMERCIAL-LICENSE.md) for commercial licensing terms and inquiries.
3. **Companion Resource Pack Assets**: Visual models, textures, GUI fonts, and sound effects bundled under `resourcepack/` are licensed separately under the **[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0)](resourcepack/LICENSE)**.

---

## 2. Public Codebase: GPLv3 Permissions & Obligations

The core engine and platform adapters are distributed under the terms of the **GNU General Public License Version 3** ([LICENSE](LICENSE)).

### What You Are Permitted To Do:
- **Run the Software**: You may run Croppy on any server—including public, private, commercial, and monetized Minecraft servers—for any purpose.
- **Study & Inspect**: You have full access to inspect, decompile, and study the pure Java 21 simulation engine and platform adapters.
- **Modify**: You may modify, customize, and adapt the source code to suit your server's gameplay requirements.
- **Redistribute**: You may redistribute verbatim or modified copies of the software, provided you comply with GPLv3 conditions.

### Key Obligations Under GPLv3:
- **Conveying Copies Triggers Source Distribution**: If you distribute, transmit, or convey copies of Croppy (or derivative works based upon it) to third parties, you **must** provide the corresponding source code under GPLv3 and preserve all copyright and license notices.
- **Same License for Derivatives**: Derivative works and combined programs conveyed to third parties must be licensed as a whole under GPLv3.
- **No Additional Restrictions**: You may not place restrictive technological measures (DRM) or licensing terms that deny recipients the rights granted by GPLv3.

### Server & Network Use (GPLv3 is NOT AGPL):
- **Private Modifications on Servers**: Running a customized, private build of Croppy on a Minecraft server network does **not** constitute "conveying" under GPLv3. You are **not** legally obligated to publish your private server-side modifications merely because players connect to your server. (This distinguishes GPLv3 from network-copyleft licenses like the AGPL).

---

## 3. Addon & API Policy

The relationship between Paper plugins, APIs, and downstream extensions involves nuanced technical and legal considerations.

### 3.1 Official Addons
The project owner reserves the right to develop and distribute official proprietary addons in the future. This will be accomplished through the commercial licensing pathway where the copyright holders have sufficient authority over the underlying codebase.

### 3.2 Third-Party Addons
- **Independent Evaluation**: The Croppy project does not and cannot guarantee that an arbitrary closed-source or proprietary third-party addon can legally link to GPL-licensed Croppy components. Third-party developers must independently ensure that their distribution model complies with applicable licensing obligations.
- **Technical Separation is Not Legal Independence**: Developing an addon as a separate JAR file, separate Gradle subproject, or interacting via reflection/interfaces does **not** automatically establish that the addon is an independent work under copyright law. If an addon forms a combined work or derivative work with GPL code, conveying that addon to third parties may require licensing it under GPLv3.
- **GPL-Compatible Addons**: Open-source addons distributed under GPLv3 or GPLv3-compatible licenses (e.g., MIT, Apache 2.0) are fully welcomed and encouraged.

### 3.3 API Design is Not a Legal Shortcut
While `croppy-api` is designed with Hexagonal Ports and clean Java 21 records, architectural modularity is an engineering practice, not a legal exemption. If unconstrained proprietary third-party extensions become a core requirement in the future, the project may evaluate alternative licensing strategies (such as permissive licensing for API interfaces), but the current authoritative license remains GPLv3.

---

## 4. Contributions, Copyright & Relicensing

### 4.1 Copyright Ownership
The Croppy project does not require copyright assignment. Contributors retain ownership of their copyright in their individual contributions. See [`COPYRIGHT.md`](COPYRIGHT.md) for details.

### 4.2 Contributor License Agreement (CLA)
To enable the dual-licensing model and protect the community, contributors may be asked to accept a Contributor License Agreement ([`CLA.md`](CLA.md)). Under the proposed CLA:
- Contributors retain copyright ownership.
- Contributors grant the project a non-exclusive, perpetual license to distribute contributions under GPLv3 and under the project's commercial licensing alternatives.
- *Note: The CLA is currently in draft status and will become active only upon formal approval and integration of an acceptance mechanism.*

### 4.3 Future Relicensing
The project owner preserves the right to update or relicense future releases, subject to holding sufficient legal rights over all incorporated contributions. A future license change cannot retroactively revoke GPL rights already granted for previous releases.

---

## 5. Frequently Asked Questions (FAQ)

### Can I use Croppy on a monetized Minecraft server?
**Yes.** GPLv3 explicitly permits commercial use. You are completely free to run Croppy on servers with donation stores, battle passes, cosmetics, or ranks.

### Do I have to publish my source code if I customize Croppy for my server?
**No, as long as the build remains private to your server.** Under GPLv3, running modified software on a server for connected players does not constitute conveying copies. You only incur source distribution obligations if you distribute the JAR binary to others outside your organization.

### If I distribute a modified version of Croppy, what must I do?
You must provide the complete corresponding source code under GPL-3.0-or-later, include all original copyright notices, and clearly indicate that you modified the files.

### Can past versions of Croppy be closed-source or revoked?
**No.** GPLv3 licenses are irrevocable once granted for released versions. Any future relicensing would apply strictly to new, subsequent releases.

---

## 6. Document Cross-References

- Canonical License: [`LICENSE`](LICENSE)
- Commercial Terms: [`COMMERCIAL-LICENSE.md`](COMMERCIAL-LICENSE.md)
- Copyright Declarations: [`COPYRIGHT.md`](COPYRIGHT.md)
- Contribution Guide: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Proposed Contributor Agreement: [`CLA.md`](CLA.md)
- Third-Party Attributions: [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)
- Companion Resource Pack License: [`resourcepack/LICENSE`](resourcepack/LICENSE)
