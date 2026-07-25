# Phase 2: Empty-folder findings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 2-Empty-folder findings
**Mode:** `--auto` (recommended defaults; no interactive prompts)
**Areas discussed:** Readme boundary for Phase 2, Finding list shape, Lint authorization, API surface, Rule severity/autoFixable, Verification surface

---

## Readme boundary for Phase 2

| Option | Description | Selected |
|--------|-------------|----------|
| Fully empty only (exclude non-blank readme) | `empty_folders` = no notes in subtree AND blank readme; readme-only wait for Phase 3 | ✓ |
| Lump all note-empty into empty_folders | Include readme-bearing until Phase 3 splits | |
| Report neither readme case | Under-report until Phase 3 | |

**User's choice:** [auto] Fully empty only (exclude non-blank readme) (recommended default)
**Notes:** Keeps `empty_folders` purge-eligible from day one; avoids interim misclassification Phase 3 would undo.

---

## Finding list shape

| Option | Description | Selected |
|--------|-------------|----------|
| Every empty folder as an item | One `HealthFindingItem` per matching folder (`folderId` + name label) | ✓ |
| Only roots of maximal empty subtrees | Collapse nested empty shells to topmost empty ancestors | |

**User's choice:** [auto] Every empty folder as an item (recommended default)
**Notes:** Matches research “list each empty folder”; nested UI can still expand; purge set is explicit.

---

## Lint authorization

| Option | Description | Selected |
|--------|-------------|----------|
| Write/`assertAuthorization` | Owner only; foreign/anon rejected | ✓ |
| Read/`assertReadAuthorization` | Owners + bazaar/subscribers can run lint | |

**User's choice:** [auto] Write/`assertAuthorization` (recommended default)
**Notes:** Aligns with research health flow and future destructive fix; Health is an owner tool in v1.

---

## API surface

| Option | Description | Selected |
|--------|-------------|----------|
| POST `.../health/lint`, minimal/empty body | Authorized endpoint → `NotebookHealthService.lint` → report; regen OpenAPI/TS | ✓ |
| GET `.../health/lint` | Idempotent GET without body | |
| Service-only (no HTTP yet) | Rule tests only; defer endpoint | |

**User's choice:** [auto] POST `.../health/lint`, minimal/empty body (recommended default)
**Notes:** Roadmap success criteria require calling lint with auth rejection; Phase 1 deferred HTTP to Phase 2.

---

## Rule severity / autoFixable

| Option | Description | Selected |
|--------|-------------|----------|
| `warning` + `autoFixable=true` | Report-only lint; flag reserved for Phase 7 gate | ✓ |
| `warning` + `autoFixable=false` until Phase 7 | Flip flag later when purge ships | |
| `error` + `autoFixable=true` | Stronger severity | |

**User's choice:** [auto] `warning` + `autoFixable=true` (recommended default)
**Notes:** Structural decay = warning; group-level autoFixable reserved per Phase 1; lint still must not mutate.

---

## Verification surface

| Option | Description | Selected |
|--------|-------------|----------|
| Backend unit + MVC/API tests; regen TS client; no E2E UI | Predicate + auth + report shape | ✓ |
| Add `@wip` E2E Health scenarios now | UI not in this phase | |
| Unit rule tests only (no HTTP) | Defer auth proof | |

**User's choice:** [auto] Backend unit + MVC/API tests; regen TS client; no E2E UI (recommended default)
**Notes:** Observable Behavior via API; Health tab is Phase 5.

---

## Claude's Discretion

- Empty-detection query strategy (batch load vs helpers)
- Folder label richness (name vs path)
- `NotebookController` vs dedicated health controller split

## Deferred Ideas

- Readme-only rule — Phase 3
- Dead links — Phase 4
- Health UI — Phase 5
- User defaults — Phase 6
- Purge/fix — Phase 7
- Maximal-root-only listing — rejected for v1
- Read-auth lint for bazaar — out of v1
