# Health UI polish (post-v1 adjustments)

**Status:** in progress (Phases 1–2 done)  
**Location:** `.planning/quick/001-health-ui-polish/`  
**Type:** Ad-hoc quick plan (not on milestone roadmap)  
**Created:** 2026-07-23

## Goal

Improve Health tab UX after v1 ship: clear in-flight feedback for lint and fix, and clearer dead-wiki-link review (flat list + navigate to the note).

## Key decisions

| Decision | Rationale |
|----------|-----------|
| Lint uses **local** loading (disable Run + body spinner), not full-app `blockUi` | User asked specifically for Run disabled + spinner in the result body |
| Fix uses **`apiCallWithLoading(..., { blockUi: true })`** | User asked for a spinner that blocks the entire UI; existing app pattern (`useGoToNextAssimilation`, book layout mutations) |
| Dead-link **nested note groups are not** `daisy-collapse` | Only the top-level “Dead wiki links” section toggles; note + tokens stay visible when the section is open |
| Click navigates via **`noteShowLocation(noteId)`** | Items already carry `noteId`; note show places the user in notebook workspace with folder context. No backend DTO change for this slice |
| **HLTH-11** (scroll/focus first dead-link occurrence in editor) stays deferred | This plan only opens the note; in-editor focus is a separate capability |
| Prefer **frontend unit/component tests**; extend `notebook_health` E2E only if a phase’s main behavior is hard to prove in Vitest | Keep slices ~5 min |

## Current surfaces

- `frontend/src/components/notebook/NotebookHealthPanel.vue` — Run / Fix / Save; no in-flight Run disable; Fix uses default (non-blocking) loading
- `frontend/src/components/notebook/NotebookHealthFindings.vue` — nested dead-link children are collapsible; labels are plain text
- Dead-link wire: child group title = note title; items have `noteId` + `label` (token)

---

## Phase 1: Lint in-flight feedback

**Type:** Behavior  
**Status:** done

**Pre-condition:** User is on notebook Health with no lint request in progress.  
**Trigger:** User clicks **Run lint**.  
**Post-condition:** Until that request finishes (success or error), **Run lint** is disabled and the Health result body shows a spinner (not the idle prompt and not a stale findings list as if ready). After completion, Run is enabled again and idle/findings update as today.

### Implementation notes

- Local `lintRunning` around `runLint`; set true before await, clear in `finally`; early-return if already running.
- Disable Run and Fix while lint is in flight; result body shows `notebook-health-lint-spinner`.
- Tests: `NotebookHealthPanel.lintLoading.spec.ts` (pending + re-run hides stale findings).

### Verification

- Frontend: pending lint keeps Run disabled and shows body spinner; after resolve, spinner gone and findings/idle as appropriate.
- Command: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.lintLoading.spec.ts`

### Learning

- Split lint-loading scenarios into a dedicated spec so `NotebookHealthPanel.spec.ts` stays ≤250 lines.

---

## Phase 2: Fix blocks the entire UI

**Type:** Behavior  
**Status:** done  
**Depends on:** Phase 1 optional but preferred (same panel loading hygiene)

**Pre-condition:** Fix is enabled (Remove empty folders checked + empty_folders ≥ 1).  
**Trigger:** User clicks the Fix control (**Remove N empty folders**).  
**Post-condition:** A **blocking** loading overlay covers the app UI for the duration of the fix call (and the subsequent auto re-lint if that remains chained in the same user action), then clears; findings refresh as today on success.

### Implementation notes

- `runWithBlockingApiLoading` wraps fix + chained re-lint for one continuous overlay via existing `ApiStatusHandler` chrome.
- Guard: early-return when `lintRunning` or Fix not enabled; Phase 1 `lintRunning` still disables Run/Fix during re-lint.
- Tests: `NotebookHealthPanel.fix.spec.ts` — GlobalApiLoadingModal host asserts overlay through fix → re-lint.

### Verification

- Frontend: Fix path shows blocking loading modal through post-fix re-lint.
- Command: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.fix.spec.ts`

### Learning

- Browser Vitest cannot `vi.spyOn` ESM `apiCallWithLoading`; assert via `GlobalApiLoadingModal` + pending fix/relint instead.

---

## Phase 3: Dead wiki links — flat list + open note

**Type:** Behavior  
**Status:** planned

**Pre-condition:** Health report includes dead wiki links (nested by note).  
**Trigger:** User expands **Dead wiki links** (section still toggles) and clicks a dead-link finding (note heading or token item).  
**Post-condition:** Nested note rows are **not** click-to-expand collapses; when the section is open, notes and tokens are visible. Clicking a finding navigates to that note (`noteShow` / `noteShowLocation`). Empty-folder and readme-only groups unchanged.

### Implementation notes

- In `NotebookHealthFindings`, for `dead_wiki_links` children: render a non-collapse block (note title + token list), not nested `daisy-collapse`.
- Top-level group for `dead_wiki_links` keeps existing section collapse behavior.
- Links: `router-link` or `router.push` via `noteShowLocation(noteId)` using each item’s `noteId` (note title may use first item’s `noteId`).
- “Note and folder”: note show is the notebook note route; folder context comes from existing note/workspace chrome — **do not** jump to `folderPage` as the primary action.
- `data-testid`s for clickable findings (e.g. note row / token links) for tests.
- Out of scope: scroll/highlight first `[[token]]` in the editor (HLTH-11 remainder).

### Verification

- Frontend: findings component — dead-link children have no nested collapse checkbox; links use note ids; other groups still collapse as before.
- Optional E2E: one scenario in `notebook_health.feature` click dead-link finding → lands on note — only if Vitest cannot cover router navigation cleanly; tag `@wip` until green.
- Commands:  
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/`  
  (targeted E2E only if added)

---

## Out of scope / deferred

- Focusing first dead-link occurrence inside the note editor (full HLTH-11)
- Click-through for empty-folder / readme-only findings
- Changing lint or fix API contracts
- Full-app `blockUi` for Run lint (explicitly local spinner per Phase 1)

## Stop-safe value

| After phase | User value |
|-------------|------------|
| 1 | Cannot double-run lint; clear that lint is in progress |
| 2 | Destructive fix cannot be confused with background work; UI blocked until done |
| 3 | Dead links easier to scan; one click opens the offending note |

## Execution

Use **execute-plan** on this directory (phases in order). Each phase: Jidoka → implement TDD → post-change-refactor → lint/format → commit → push.

---

*Quick plan: 001-health-ui-polish*
