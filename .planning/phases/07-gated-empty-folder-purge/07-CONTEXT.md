# Phase 7: Gated empty-folder purge - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Behavior.** User can optionally apply the only v1 Health fix — **bulk remove fully empty folder trees** — via a **dedicated purge** path, only when the **Remove empty folders** run option is selected. Readme-only folders are never deleted by this fix. Lint remains report-only; Run alone never mutates.

Delivers AFIX-02, AFIX-03, AFIX-04, and AFIX-05. Extends the Phase 5/6 Health action bar (checkbox already gates intent; prefill/save from Phase 6). No per-folder multi-select. No auto-fix for dead links or readme-only. No dissolve / promote-children.

</domain>

<decisions>
## Implementation Decisions

### Fix control (AFIX-02, AFIX-03)
- **D-01:** Add a **Fix** control on the Health **action bar** (same bar as Run lint, Remove empty folders checkbox, Save as defaults). No per-folder multi-select and no second fix menu.
- **D-02:** Fix is **enabled only when** the run-scoped **Remove empty folders** checkbox is checked **and** the last successful lint report has **≥1** item in the `empty_folders` group (findings list = preview). Otherwise Fix is **disabled** (visible but not active) so the gate is discoverable.
- **D-03:** Button copy prefers explicit language with count when known: **“Remove N empty folders”** (N = `empty_folders` item count from last report). Fall back to **“Remove empty folders”** if count unavailable. Do **not** use a generic “Apply” / “Fix” alone as the only label.
- **D-04:** No extra confirm dialog in v1 — consent is the checked bulk option plus the findings preview. (Server still re-validates emptiness.)

### API / server gate (AFIX-03, AFIX-05)
- **D-05:** Dedicated fix endpoint on `NotebookHealthController`: **`POST /api/notebooks/{notebook}/health/fix`** with body requiring **`removeEmptyFolders: true`**. Do **not** overload bodyless `POST .../health/lint` with mutation. Do **not** send user defaults on the fix request — only the explicit run opt-in flag.
- **D-06:** Authorize with **`assertAuthorization(notebook)`** (owner/write), same gate as lint. Foreign/anon callers rejected.
- **D-07:** Server **must** reject the fix when `removeEmptyFolders` is missing or not `true` (client disable is insufficient). Client-only gating is never enough.
- **D-08:** Server **recomputes** the fully-empty set at fix time using the **same predicate** as `empty_folders` / `FolderSubtreeLiveNotes` (no notes in subtree, blank own `readmeContent`; soft-deleted notes do not count as content). Do **not** trust client-supplied folder IDs as authority for what to delete. TOCTOU: folders that gained notes or non-blank readme between lint and fix are not deleted.

### Purge semantics (AFIX-04, AFIX-05)
- **D-09:** Implement a **dedicated purge applicator** (capability-named, e.g. empty-folder bulk purge on `NotebookHealthService` or a focused collaborator). **Hard-delete** folder rows that match the fully-empty predicate, including nested empty shells, in **one transaction**, children-before-parents (or any equivalent order that never leaves orphan FK issues).
- **D-10:** **Must not** call `dissolveFolder` / promote-children / `DELETE .../folders/{folder}` dissolve semantics. Purge removes empty trees; dissolve promotes contents.
- **D-11:** Folders in **`readme_only_folders`** (note-empty but non-blank `readmeContent`) are **never** deleted by this fix. Dead-link findings are unaffected (report-only).
- **D-12:** After purge, notebook structure must not show promoted empty shells at parent level (the dissolve anti-pattern).

### Post-fix UX (success criterion 5)
- **D-13:** On successful fix, **automatically re-run lint** (same bodyless lint path) and replace the panel report so the user immediately sees purged fully empty folders gone; readme-only and dead-link groups remain as applicable. User can also manually Run again (same outcome).
- **D-14:** On failure, surface error via `apiCallWithLoading` / existing error handling; leave prior report visible; no partial UI claim of success.

### Verification
- **D-15:** Backend: unit/service tests for purge predicate + order; reject without opt-in; never deletes readme-only; never invokes dissolve; authorized fix removes empty set; unauthorized rejected. Controller/MVC coverage for the fix endpoint.
- **D-16:** Frontend unit/component: Fix disabled when checkbox off or no empty_folders items; enabled when checkbox on and report has empty folders; Fix calls health/fix then refreshes report.
- **D-17:** Targeted E2E (extend capability-named `notebook_health`): seed fully empty folder(s) + readme-only folder → check Remove empty folders → Run lint → see empty folder finding → Fix → empty folders gone from findings / notebook; readme-only finding still present; no promote-to-parent shells. Tag `@wip` until green; remove `@wip` when scenarios pass. Do not run the full E2E suite unless required.

### Claude's Discretion
- Exact Java type name for the applicator (`EmptyFolderBulkPurge` vs method on `NotebookHealthService`) — prefer cohesive package under health services; no phase numbers in product names.
- Exact response DTO for fix (void + client re-lint vs returning purge count) — prefer **void/204 or simple count** and client re-lint per D-13; researcher/planner may choose the smallest OpenAPI surface.
- DaisyUI classes for Fix: secondary/warning/sm is fine; **primary accent stays on Run lint** (Phase 5 UI-SPEC). Destructive styling only if it matches existing delete patterns without fighting the action bar.
- Whether Fix sits left or right of Save as defaults — keep action-bar density and `data-testid`s consistent (`notebook-health-fix` or `notebook-health-remove-empty-folders-fix`).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 7 goal, Behavior type, success criteria (AFIX-02–05 + re-run no longer sees purged folders), UI hint
- `.planning/REQUIREMENTS.md` — AFIX-02, AFIX-03, AFIX-04, AFIX-05; Out of Scope: per-folder multi-select, dead-link auto-fix
- `.planning/PROJECT.md` — lint ≠ fix; only bulk remove empty folders; Fix active only when option selected; dedicated purge not dissolve

### Prior phase decisions
- `.planning/phases/02-empty-folder-findings/02-CONTEXT.md` — fully-empty predicate; `empty_folders` group; `autoFixable=true` reserved for this purge
- `.planning/phases/03-readme-only-folder-findings/03-CONTEXT.md` — `readme_only_folders` never fix-eligible (`autoFixable=false`)
- `.planning/phases/05-health-tab-and-run/05-CONTEXT.md` — Health tab, action bar, bodyless lint, checkbox UI-only until this phase
- `.planning/phases/05-health-tab-and-run/05-UI-SPEC.md` — action-bar layout; primary reserved for Run lint; Fix not in Phase 5
- `.planning/phases/06-user-level-defaults/06-CONTEXT.md` — same checkbox for Fix gate; defaults only prefill UI; lint stays bodyless

### Research (purge pitfalls and API shape)
- `.planning/research/ARCHITECTURE.md` — `POST .../health/fix` + `{ removeEmptyFolders: true }`; dedicated applicator; never dissolve; server enforces gate
- `.planning/research/PITFALLS.md` — Pitfall 4 dissolve misuse; TOCTOU re-check; children-before-parents; findings list as preview
- `.planning/research/FEATURES.md` — single bulk structural fix; no dry-run mode; findings = preview
- `.planning/research/SUMMARY.md` — milestone ordering; report before fix

### Frontend / API conventions
- `.planning/codebase/CONVENTIONS.md` — `data-testid`, capability naming, `apiCallWithLoading`
- `.planning/codebase/ARCHITECTURE.md` — thin controllers + `AuthorizationService`
- `.cursor/rules/frontend-api.mdc` — generated SDK + `apiCallWithLoading` + error handling
- `.cursor/rules/e2e-authoring.mdc` — capability-named features, `@wip`, page objects
- `.cursor/skills/generate-api-client/SKILL.md` — regenerate TS client after OpenAPI health/fix change

### Implemented surfaces to extend (must not replace)
- `frontend/src/components/notebook/NotebookHealthPanel.vue` — action bar + `removeEmptyFolders` + Run lint + Save as defaults; add gated Fix
- `frontend/src/components/notebook/NotebookHealthFindings.vue` — findings preview (empty_folders items drive N and enablement)
- `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java` — add fix endpoint alongside lint
- `backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java` — purge orchestration
- `backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java` / `FolderSubtreeLiveNotes` — shared fully-empty predicate
- `backend/src/main/java/com/odde/doughnut/services/FolderRelocationService.java` / `NotebookController.dissolveFolder` — **do not call** for Health fix
- `packages/generated/doughnut-backend-api/sdk.gen.ts` — regen after fix endpoint
- `e2e_test/features/notebooks/notebook_health.feature` — extend for gated purge

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NotebookHealthPanel` — action bar with Run lint, Remove empty folders checkbox (prefilled from `currentUser.healthRemoveEmptyFoldersDefault`), Save as defaults; local `report` + `removeEmptyFolders` refs
- `NotebookHealthFindings` — expandable groups; `empty_folders` items supply labels and count N
- `NotebookHealthController.lint` — bodyless report-only POST; write-auth
- `EmptyFolderHealthRule` + `FolderSubtreeLiveNotes.noteEmptyFolderItems` — fully-empty vs readme-only complementary gates
- `apiCallWithLoading` — user-initiated loading/errors for Fix and post-fix lint refresh
- Generated SDK pattern for new controller methods after OpenAPI regen

### Established Patterns
- Controllers authorize then delegate to services; Health is notebook-owner write tool
- Lint never mutates; mutation is a separate explicit path
- Domain language: **readme** / `readmeContent`; rule id `empty_folders`
- Dissolve promotes children — wrong for cleanup (research Pitfall 4)
- E2E: capability-named `notebook_health`; `@wip` while red

### Integration Points
- New `POST .../health/fix` on `NotebookHealthController` → service purge applicator
- Regen OpenAPI / TypeScript client for fix method + request body
- `NotebookHealthPanel`: compute Fix enabled from checkbox + `empty_folders` item count; call fix then re-lint
- Reuse fully-empty predicate; never dissolve; never delete `readme_only_folders`

</code_context>

<specifics>
## Specific Ideas

- `--auto` discussion locked: gated Fix on action bar; dedicated `POST .../health/fix` with `removeEmptyFolders: true`; server recomputes fully-empty set; dedicated hard-delete purge (not dissolve); auto re-lint after success; findings list is preview (Fix needs ≥1 empty folder in last report); button label “Remove N empty folders”; no confirm dialog; extend `notebook_health` E2E.
- Research package names (`EmptyFolderBulkFixApplicator`, `/health/fix`) are guidance — capability names, not phase numbers.

</specifics>

<deferred>
## Deferred Ideas

- Per-folder multi-select delete — out of v1 (rejected by requirements)
- Auto-fix for dead wiki links or readme-only — out of v1
- Extra confirm modal beyond checkbox + findings preview — not needed for v1
- Dry-run fix mode separate from findings — redundant (findings = preview)
- Undo / recycle bin for purged folders — not in milestone scope

None — discussion stayed within phase scope

</deferred>

---

*Phase: 7-Gated empty-folder purge*
*Context gathered: 2026-07-22*
