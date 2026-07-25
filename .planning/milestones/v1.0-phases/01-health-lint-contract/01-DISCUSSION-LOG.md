# Phase 1: Health lint contract - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 1-Health lint contract
**Mode:** `--auto` (recommended defaults selected)
**Areas discussed:** Phase 1 delivery surface, Report and finding shapes, Rule interface and registry, Package layout and naming, Verification

---

## Phase 1 delivery surface

| Option | Description | Selected |
|--------|-------------|----------|
| Backend-only contract (DTOs + rule interface + runner + service skeleton; no HTTP) | Keeps product behavior unchanged; Phase 2 owns authorized lint path | ✓ |
| Stub lint endpoint returning empty report | Exposes OpenAPI early but adds a user-reachable no-op API | |
| Full request/fix DTO surface in Phase 1 | Premature; fix path is Phase 7 | |

**User's choice:** [auto] Backend-only contract (recommended default)
**Notes:** Aligns with ROADMAP Structure success criteria and research Phase 1 deliverables.

---

## Report and finding shapes

| Option | Description | Selected |
|--------|-------------|----------|
| Nested groups (`items` + `children`) with reserved severity/autoFixable | UI-ready expandable tree; Phase 4 needs children for dead links by note | ✓ |
| Flat findings list with ruleId only | Forces frontend regrouping and Phase 4 model rework | |
| Groups with items only (no children) | Insufficient for dead-link-by-note without contract change | |

**User's choice:** [auto] Nested groups (recommended default)
**Notes:** Matches `.planning/research/ARCHITECTURE.md` conceptual OpenAPI types.

---

## Rule interface and registry

| Option | Description | Selected |
|--------|-------------|----------|
| `HealthRule` with id/severity/autoFixable/evaluate; Spring `List<HealthRule>` injection; zero rules in Phase 1 | LM Wiki pattern; empty registry returns empty report | ✓ |
| Hardcoded rule list in service | Less extensible for Phases 2–4 | |
| Metadata only on findings, not rules | Weaker alignment with research/registry inspiration | |

**User's choice:** [auto] HealthRule + Spring list injection (recommended default)
**Notes:** Phase 1 registers zero rule beans.

---

## Package layout and naming

| Option | Description | Selected |
|--------|-------------|----------|
| `services/health/` + `controllers/dto/` + `NotebookHealthService`; domain **readme** | Matches research layout and product rename | ✓ |
| All types under services only | Breaks OpenAPI DTO convention | |
| Keep `indexContent` naming from older research snippets | Conflicts with PRODUCT/ROADMAP rename | |

**User's choice:** [auto] Research package layout + readme naming (recommended default)
**Notes:** Stale `indexContent` mentions in research are non-canonical.

---

## Verification

| Option | Description | Selected |
|--------|-------------|----------|
| Focused backend unit tests; no E2E; no TS client regen | Structure phase; behavior unchanged | ✓ |
| Regenerate OpenAPI/TS client without endpoints | No schemas appear without controller; wasted churn | |
| Add `@wip` E2E for Health | No UI/API yet | |

**User's choice:** [auto] Focused backend unit tests only (recommended default)

---

## Claude's Discretion

- Java record vs class for DTOs
- Exact OpenAPI annotation style
- `@Order` vs natural list order for rules
- Public vs package visibility of `lint` on service in Phase 1

## Deferred Ideas

- Empty-folder rule + lint API — Phase 2
- Readme-only findings — Phase 3
- Dead links — Phase 4
- Health tab — Phase 5
- User defaults — Phase 6
- Fix/purge — Phase 7
