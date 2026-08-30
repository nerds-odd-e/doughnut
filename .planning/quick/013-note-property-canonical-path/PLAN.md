# Note property canonical path

**Status:** shipped (2026-08-30). Plan retired.
**Type:** ad-hoc plan (`.planning/quick/`)
**Policy:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Property**, **Property panel**, **Wiki link**), [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) (`#prop:`), Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) (`noteProperty`).
**Human-owned exception:** ADR 0001 / ADR 0004 may depend on Proposed ADR 0005
until that route policy is Accepted. Do not change ADR 0005 status here.

A property’s web location is `noteProperty` (`/n:noteId/p/:propertyKey`): the
note with that **property panel** open. The **property value dialog** is local
editing chrome. Portable spelling is `#prop:<encoded-key>`. Live wiki compiles
to `noteProperty`; paste of a compiled URL resolves the SPA note id to the
portable note target (label is display only).
