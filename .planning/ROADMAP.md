# Roadmap: Progressive Cancelable Blocking UI

## Overview

This roadmap adds cancellation without making unsafe promises. It first establishes one shared cancellation contract with no user-visible change, immediately proves that contract on initial note-refinement layout generation, reuses it for extraction-preview generation, and finishes by preserving an explicit noncancelable boundary for transactional note creation while auditing the complete in-scope loading flow.

The project selected fine granularity. Four phases are sufficient because the repository's hard stop-safe rule forbids splitting a cancel interaction into partially working UI, abort, cleanup, and caller-state phases; those pieces must land together as one coherent observable behavior.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned project work
- Decimal phases (2.1, 2.2): Urgent insertions marked `INSERTED`

- [x] **Phase 1: Shared Cancellation Contract** - Add cohesive opt-in cancellation structure without changing existing blocker behavior (completed 2026-07-21)
- [ ] **Phase 2: Cancel Refinement Layout Generation** - Let the user cancel the initial AI layout wait and retry safely
- [ ] **Phase 3: Cancel Extraction Preview Generation** - Reuse the contract while preserving layout selections and preview state
- [ ] **Phase 4: Enforce Safe Blocking Boundaries** - Keep transactional creation noncancelable and verify consistent adoption across the refinement flow

## Phase Details

### Phase 1: Shared Cancellation Contract

**Goal**: Introduce the shared abort ownership, cancelled outcome, and conditional modal control needed only by the immediately following layout-generation behavior
**Mode:** mvp
**Type:** Structure
**UI hint:** yes
**Depends on**: Nothing (first phase)
**Requirements**: COHE-01
**Success Criteria** (what must be TRUE):

  1. Existing whole-UI blockers still show their current messages and do not expose Cancel
  2. Existing nested and concurrent loading states still remain visible and clean up independently
  3. One shared API-loading contract owns abort, cleanup, and cancellation classification without changing current callers

**Plans**: 3/3 plans executed

- [x] 01-01-PLAN.md
- [x] 01-02-PLAN.md
- [x] 01-03-PLAN.md

### Phase 2: Cancel Refinement Layout Generation

**Goal**: As a note author using Refine note, I want to cancel the initial AI layout wait and retry in the same dialog, so that I recover without changing note content or losing my place.
**Mode:** mvp
**Type:** Behavior
**UI hint:** yes
**Depends on**: Phase 1
**Requirements**: CANC-01, CANC-02, CANC-03, CANC-04, REFN-01, REFN-02
**Success Criteria** (what must be TRUE):

  1. While initial refinement layout generation is pending, the global blocking spinner displays its message and an accessible Cancel control
  2. Activating Cancel aborts only that browser request, promptly removes its loading state, and leaves any other concurrent loading states intact
  3. Cancellation produces no error toast, success handling, navigation, or note-content change
  4. The refinement dialog remains open with a retry action that can start a fresh request

**Plans**: 3/3 plans executed

Plans:
**Wave 1**

- [x] 02-01-PLAN.md — Failing Vitest cancel+retry suite and pending-mount helpers (TDD RED)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 02-02-PLAN.md — Cancelable layout load + empty/retry panel (vertical GREEN slice)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 02-03-PLAN.md — Concurrent/idempotent edges, scope-boundary regressions, frontend-api.mdc docs

**Cross-cutting constraints:**

- While initial layout generation is pending, the global blocker shows AI is generating layout... and an accessible Cancel control.
- After cancel, no selected checkboxes / no extract actions until a populated layout returns.

### Phase 3: Cancel Extraction Preview Generation

**Goal**: As a note author using Refine note, I want to cancel AI extraction-preview generation without losing my layout selections, so that I can retry Extract from the same choices or keep my prior preview if I cancelled a regenerate.
**Mode:** mvp
**Type:** Behavior
**UI hint:** yes
**Depends on**: Phase 2
**Requirements**: REFN-03, REFN-04
**Success Criteria** (what must be TRUE):

  1. While extraction-preview generation is pending, the same global blocking spinner displays Cancel
  2. Activating Cancel keeps all selected refinement-layout items and leaves the user before the preview
  3. Cancellation shows no API error and the user can retry preview generation from the preserved selection

**Plans**: 3 plans

Plans:
**Wave 1**

- [ ] 03-01-PLAN.md — Failing Vitest cancel suite and pending-preview helpers (TDD RED)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 03-02-PLAN.md — Cancelable preview load + preserve-selection / keep-prior-preview (vertical GREEN slice)

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 03-03-PLAN.md — Retry/idempotent edges, create-note noncancelable regression, frontend-api.mdc docs

**Cross-cutting constraints:**

- While extraction-preview generation is pending, the global blocker shows AI is generating preview... and an accessible Cancel control.
- After Extract cancel, selections stay and Extract remains the retry CTA (no layout empty panel).
- Create-note remains noncancelable in this phase.

### Phase 4: Enforce Safe Blocking Boundaries

**Goal**: User receives a consistent blocker throughout note extraction without being offered unsafe client-only cancellation for transactional creation
**Mode:** mvp
**Type:** Behavior
**UI hint:** yes
**Depends on**: Phase 3
**Requirements**: REFN-05, COHE-02
**Success Criteria** (what must be TRUE):

  1. While final extracted-note creation is pending, the shared blocking spinner remains visible without Cancel
  2. Successful final creation still completes once and follows the existing navigation behavior
  3. Every existing whole-UI blocker and long-running note-refinement AI request has an explicit cancellation classification, and every safe in-scope request uses the shared contract
  4. No in-scope caller duplicates generic abort, loading cleanup, or cancellation-error suppression logic

**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Shared Cancellation Contract | 3/3 | Complete    | 2026-07-21 |
| 2. Cancel Refinement Layout Generation | 3/3 | Plans complete | 2026-07-21 |
| 3. Cancel Extraction Preview Generation | 0/3 | Planned | - |
| 4. Enforce Safe Blocking Boundaries | 0/TBD | Not started | - |
