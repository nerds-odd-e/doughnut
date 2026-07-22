# Roadmap: Notebook Lint & Auto-Fix

## Overview

Ship a notebook **Health** capability: mechanical lint that reports structural decay (recursive empty folders, note-empty folders with non-blank **readme**, dead wiki links in body and frontmatter), then a Health tab where users Run and review nested findings, save per-user defaults, and optionally apply one gated fix — bulk purge of fully empty folder trees. Report-only value lands early; mutation last. Structure only when it enables the immediate next behavior. Domain names use **readme** / `readmeContent` (not index).

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Health lint contract** - Structure: shared findings DTOs and rule-registry skeleton for the first lint rule
- [x] **Phase 2: Empty-folder findings** - Behavior: authorized lint reports recursive empty folders (report-only)
- [x] **Phase 3: Readme-only folder findings** - Behavior: note-empty folders with non-blank readme as a separate finding type
- [x] **Phase 4: Dead-link findings** - Behavior: lint reports dead wiki links in body and frontmatter (report-only)
- [x] **Phase 5: Health tab and Run** - Behavior: Health tab with Run lint and expandable nested findings (no mutation on Run)
- [x] **Phase 6: User-level defaults** - Behavior: save and prefill Health run options at user level
- [ ] **Phase 7: Gated empty-folder purge** - Behavior: optional bulk remove of fully empty folders via dedicated purge

## Phase Details

### Phase 1: Health lint contract

**Goal:** Shared Health lint contract (finding shape, rule interface, runner skeleton) ready so the empty-folder rule can plug in without inventing a second model
**Mode:** mvp
**Type:** Structure
**Depends on:** Nothing (first phase)
**Requirements:** *(none — enables Phase 2)*
**Success Criteria** (what must be TRUE):

  1. Existing product behavior is unchanged (existing unit and targeted E2E tests still pass)
  2. A `HealthRule`-style interface and runner skeleton exist under the Health service package, with OpenAPI-ready findings DTO shapes (report groups/items, rule id, severity, `autoFixable` reserved)
  3. The contract is sufficient for Phase 2 to add one rule and return a typed lint report without reworking the shared model

**Plans:** 2/2 plans executed

Plans:

- [x] 01-01-PLAN.md — OpenAPI-ready findings DTOs (report/group/item/severity) + nested construction test
- [x] 01-02-PLAN.md — HealthRule registry skeleton + NotebookHealthService + zero-rules empty report

### Phase 2: Empty-folder findings

**Goal:** As a notebook owner, I want to run authorized Health lint and receive fully empty folders under an empty-folder findings group, so that I can audit structural decay without mutating my notebook.
**Mode:** mvp
**Type:** Behavior
**Depends on:** Phase 1
**Requirements:** EFOL-01, EFOL-02
**Success Criteria** (what must be TRUE):

  1. Calling notebook Health lint reports folders whose **entire subtree has no notes** (recursive; soft-deleted notes do not count as content)
  2. The empty-folder findings group lists those folders even when auto-fix is not selected (report path does not require fix options)
  3. Lint does not delete folders or otherwise mutate notebook data
  4. Only an authorized notebook actor can run lint (foreign/anon callers are rejected)

**Plans:** 2/2 plans executed

Plans:
**Wave 1**

- [x] 02-01-PLAN.md — EmptyFolderHealthRule + live folder-id query + predicate tests (EFOL-01)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 02-02-PLAN.md — Authorized POST .../health/lint + controller tests + OpenAPI regen (EFOL-02)

### Phase 3: Readme-only folder findings

**Goal:** Note-empty folders that still have non-blank **readme** (`readmeContent`) appear as their own finding type, not lumped with fully empty folders
**Mode:** mvp
**Type:** Behavior
**Depends on:** Phase 2
**Requirements:** EFOL-03
**Success Criteria** (what must be TRUE):

  1. Lint reports note-empty folders with non-blank `readmeContent` under a **separate** finding type/group from fully empty folders
  2. Fully empty folders (no notes in subtree and no meaningful readme) remain only under the empty-folder findings group
  3. Lint remains report-only (no deletes or mutations)

**Plans:** 1/1 plans executed

Plans:
**Wave 1**

- [x] 03-01-PLAN.md — Shared FolderSubtreeLiveNotes + ReadmeOnlyFolderHealthRule + mutual-exclusion tests (EFOL-03)

### Phase 4: Dead-link findings

**Goal:** Lint reports dead `[[wiki links]]` in note body and frontmatter using editor resolve semantics, report-only
**Mode:** mvp
**Type:** Behavior
**Depends on:** Phase 1
**Requirements:** DLNK-01, DLNK-02, DLNK-03
**Success Criteria** (what must be TRUE):

  1. Lint reports dead wiki links in note **body** with the same resolve semantics as the editor (aliases and qualified `Notebook:Title` links included)
  2. Lint reports dead wiki links in note **frontmatter / properties** with the same resolve semantics
  3. Dead-link findings are grouped by note in the report and are **report-only** (no auto-fix or rewrite path)

**Plans:** 2/2 plans executed

Plans:
**Wave 1**

- [x] 04-01-PLAN.md — HealthRunContext viewer + unresolvedWikiLinkTokens + live-notes query (Structure for dead links)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 04-02-PLAN.md — DeadWikiLinkHealthRule + nested findings tests (DLNK-01/02/03)

### Phase 5: Health tab and Run

**Goal:** From Notebook Settings → Health, the user can Run lint on demand and review expandable nested findings without a separate route or dialog, and without mutation on Run
**Mode:** mvp
**Type:** Behavior
**Depends on:** Phase 2, Phase 3, Phase 4
**Requirements:** HLTH-01, HLTH-02, HLTH-03, AFIX-01
**Success Criteria** (what must be TRUE):

  1. User can open a **Health** tab on notebook settings alongside Index/Settings (no dedicated `/health` route or findings dialog)
  2. User can explicitly **Run lint** for the current notebook; merely opening Health does not mutate data
  3. User can review **expandable nested findings** on the Health tab (grouped by rule; dead links nested by note) with an action bar for fix options
  4. Auto-fix is an **optional** run option; Run alone never deletes or mutates notebook data

**Plans:** 2/2 plans executed

Plans:
**Wave 1**

- [x] 05-01-PLAN.md — Health tab shell + action bar + Run lint (idle→report groups; AFIX-01 unit)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 05-02-PLAN.md — Expandable findings polish + targeted notebook_health E2E + landmarks

**UI hint:** yes

### Phase 6: User-level defaults

**Goal:** As a notebook owner, I want to save Remove empty folders as my Health default and see it prefilled on any notebook Health tab, so that my run options persist across notebooks without applying fixes on open or save.
**Mode:** mvp
**Type:** Behavior
**Depends on:** Phase 5
**Requirements:** DFLT-01, DFLT-02
**Success Criteria** (what must be TRUE):

  1. User can save Health lint/auto-fix option defaults at **user** level (apply across notebooks)
  2. Opening Health prefills run options from the user’s saved defaults
  3. Prefilling defaults does not apply fixes or mutate notebook data on open

**Plans:** 2/2 plans executed

Plans:

- [x] 06-01-PLAN.md — Persist healthRemoveEmptyFoldersDefault on User (Flyway + DTO + updateUser + OpenAPI regen)
- [x] 06-02-PLAN.md — Health prefill + Save as defaults + cross-notebook notebook_health E2E

**Wave 1**

- [x] 06-01-PLAN.md — Backend User column + DTO + updateUser + tests + OpenAPI regen

**Wave 2**

- [x] 06-02-PLAN.md — Frontend prefill + Save + frontend tests + E2E

**UI hint:** yes

### Phase 7: Gated empty-folder purge

**Goal:** User can optionally apply the only v1 fix — bulk remove fully empty folder trees — via a dedicated purge path, only when the bulk option is selected; readme-only folders are never deleted by this fix
**Mode:** mvp
**Type:** Behavior
**Depends on:** Phase 5, Phase 6
**Requirements:** AFIX-02, AFIX-03, AFIX-04, AFIX-05
**Success Criteria** (what must be TRUE):

  1. The only v1 fix action is bulk **remove empty folders** (no per-folder multi-select)
  2. **Fix** is enabled only when the user has selected the bulk “remove empty folders” option
  3. Bulk remove deletes only **fully empty** folder trees (no notes in subtree and no meaningful readme); folders in the separate readme-only finding type are **not** deleted
  4. Empty-folder removal uses a **dedicated purge** path (does not use folder dissolve / promote-children)
  5. After a successful fix, the user can re-run lint and no longer sees the purged fully empty folders

**Plans:** TBD
**UI hint:** yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Health lint contract | 2/2 | Complete | 2026-07-22 |
| 2. Empty-folder findings | 2/2 | Complete | 2026-07-22 |
| 3. Readme-only folder findings | 1/1 | Complete | 2026-07-22 |
| 4. Dead-link findings | 2/2 | Complete | 2026-07-22 |
| 5. Health tab and Run | 2/2 | Complete | 2026-07-22 |
| 6. User-level defaults | 2/2 | In Progress|  |
| 7. Gated empty-folder purge | 0/TBD | Not started | - |

## Coverage

| Requirement | Phase |
|-------------|-------|
| EFOL-01 | Phase 2 |
| EFOL-02 | Phase 2 |
| EFOL-03 | Phase 3 |
| DLNK-01 | Phase 4 |
| DLNK-02 | Phase 4 |
| DLNK-03 | Phase 4 |
| HLTH-01 | Phase 5 |
| HLTH-02 | Phase 5 |
| HLTH-03 | Phase 5 |
| AFIX-01 | Phase 5 |
| DFLT-01 | Phase 6 |
| DFLT-02 | Phase 6 |
| AFIX-02 | Phase 7 |
| AFIX-03 | Phase 7 |
| AFIX-04 | Phase 7 |
| AFIX-05 | Phase 7 |

**Coverage:** 16/16 v1 requirements mapped ✓

## Stop-safe value

| Stop after | Value retained |
|------------|----------------|
| Phase 1 | No user-facing Health yet; zero product behavior change |
| Phase 2 | Empty-folder audit via authorized lint API |
| Phase 3 | Plus separate readme-only findings (fix-eligibility boundary clear) |
| Phase 4 | Full mechanical report (empty folders + readme-only + dead links) via API |
| Phase 5 | Full user-facing Run → review loop on Health tab (report-only) |
| Phase 6 | Preferences persist across notebooks |
| Phase 7 | Optional gated purge of fully empty folders |

---
*Roadmap created: 2026-07-22*
*Granularity: fine · Mode: mvp · Grammar: Behavior/Structure, one observable behavior per Behavior phase*
