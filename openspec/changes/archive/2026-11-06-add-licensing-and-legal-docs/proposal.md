# Proposal: Add Licensing, Copyright, Contribution, and Legal Documentation

## Why

The repository recently introduced a comprehensive root `README.md` referencing critical legal and governance files (`LICENSE`, `LICENSING.md`, `COMMERCIAL-LICENSE.md`, `COPYRIGHT.md`, `CONTRIBUTING.md`, `CLA.md`, and `THIRD-PARTY-NOTICES.md`). Authoring these documents establishes a clear, consistent, and legally accurate dual-licensing framework adhering strictly to the owner's policy brief (`.internal/docs/Croppy-Licensing-Agent-Brief.md`).

## What Changes

Create the required legal and governance documentation suite at the repository root:
- **`LICENSE`**: The full canonical, unmodified GNU General Public License v3.0 text (`GPL-3.0-or-later`).
- **`LICENSING.md`**: Human- and developer-friendly policy overview of the dual-licensing structure, explaining GPLv3 permissions/obligations, the commercial alternative, official vs. third-party addon boundaries, API modularity caveats, and a practical FAQ.
- **`COMMERCIAL-LICENSE.md`**: Clearly marked draft/template of alternative commercial terms granting permissions beyond GPLv3 without inventing fees, contacts, or entities.
- **`COPYRIGHT.md`**: Copyright declarations, attribution rules, and explicit placeholders for unresolved legal entities or rights holders.
- **`CONTRIBUTING.md`**: Contributor workflow guidelines, submission originality confirmations, and policy on third-party code and CLA readiness.
- **`CLA.md`**: Proposed Contributor License Agreement draft enabling dual licensing while allowing contributors to retain copyright ownership.
- **`THIRD-PARTY-NOTICES.md`**: Comprehensive inventory of dependencies, libraries, and assets actually present in the repository and build scripts with respective licenses and notices.

## Capabilities

### New Capabilities
*(None - documentation and legal policy only. Spec updates skipped via `skip_specs: true` in `.openspec.yaml`)*.

### Modified Capabilities
*(None - no behavioral requirements or runtime contracts are being modified)*.

## Impact

- **Code/APIs**: Zero impact on Java simulation engine, platform adapters, or public contracts.
- **Repository Governance**: Establishes unambiguous legal foundations for open-source distribution and reserved commercial pathways.
