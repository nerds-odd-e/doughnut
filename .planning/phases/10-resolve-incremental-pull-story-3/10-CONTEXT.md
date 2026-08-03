# Phase 10: Resolve incremental pull (story 3) - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Apply Phase 7’s Story 3 verdict (**strengthen**) in the tree so `/sync` (non-dry-run) / `applyPull` matches story 3 acceptance: unchanged files keep content and mtime; new / changed / renamed / moved remote notes produce the expected local changes; re-pull with no intervening changes is a filesystem no-op; sync metadata updates only after a successful mutating operation; a no-change pull creates no irrelevant VCS diffs. Do not remove the capability. Do not change Terry Yin / Tan Yeong Sheng work (HYG-02). Do not implement Story 4 lint gaps or Stories 5–6 push behavior.

</domain>

<decisions>
## Implementation Decisions

### Gap coverage (EXP-03)
- **D-01:** Phase 10 closes **both** TRIAGE Story 3 gaps: (1) apply remote **create / rename / move** (not only intersecting-path overwrite), and (2) update `.doughnut-sync` sync metadata after a **successful mutating** pull. Partial “create-only” or “metadata-only” is not enough for EXP-03. — **Reversibility:** costly — shipping one gap leaves EXP-03 incomplete and invites a second Story 3 phase.

### Apply action taxonomy
- **D-02:** Reuse Phase 9 pull classification semantics (`classifyPreviewPullNotes` / create · update · move · reject). `applyPull` must **apply** create (write new remote-only `.md`), update (overwrite differing intersecting path), and move (`doughnut_id` path change: rename/remove old path then write new path with remote content). Do **not** invent moves without identity — path-keyed create/update only when `doughnut_id` is missing (same as Phase 9 D-08). — **Reversibility:** costly — CLI/E2E contract for applied actions becomes the Story 3 proof surface; preview and apply must stay aligned.
- **D-03:** **Reject** paths (reserved names, duplicates, invalid mappings) are **not written**. Report rejects clearly in the apply result (do not silently skip). Safe create/update/move actions may still apply in the same run. Local-only files remain untouched. Unchanged intersecting files must not be rewritten (preserve mtime). Remote notes absent locally that are rejected must not be created. — **Reversibility:** reversible — report wording can refine later if semantics hold.
- **D-04:** Do **not** delete local-only Markdown when remote no longer has that path (oracle does not require remote-driven deletes). Leave local-only files alone. — **Reversibility:** reversible.

### Sync metadata
- **D-05:** After a successful **mutating** pull (at least one create/update/move applied), update `.doughnut-sync/baseline.json` via existing `savePushBaseline` so pull and push share the same merge-base shape (`notebookId` + per-path agreed content). Include agreed content for notes touched by the pull (and keep consistency with how `/export` seeds baseline). — **Reversibility:** costly — push dry-run/push (Phases 12–13) depend on baseline meaning.
- **D-06:** A no-op pull (`No changes to pull.` — no creates/updates/moves applied) must **not** rewrite baseline or other sync metadata (oracle: no irrelevant VCS diffs). Rejects-only with zero applied mutations also must not rewrite baseline. — **Reversibility:** costly — false baseline churn breaks the no-change VCS acceptance bullet.

### Implementation surface
- **D-07:** Primary strengthen lands in `applyPull` (and small helpers as needed). Prefer reusing `classifyPreviewPullNotes` from Phase 9 rather than duplicating create/move/reject rules. Touch `syncSlashCommand` only if needed to surface apply summaries. Do **not** weaken Phase 9 non-mutating dry-run. Prefer not changing backend zip in this phase unless a Story 3 proof is blocked (Phase 8 already supplies `doughnut_id`). — **Reversibility:** costly — splitting apply taxonomy away from preview reintroduces Story 2/3 drift.
- **D-08:** Flip/replace the intentional anti-create contract: unit `does not create a file for a remote-only note` and E2E `No new local file for a remote-only note` become create (and move) proofs. Keep green behaviors: intersecting update, local-only untouched, idempotent re-pull / no-op summary, `@perfSync` budget. — **Reversibility:** one-way — published E2E scenarios change from “never create” to “create/move when remote requires it”.

### Proof strategy
- **D-09:** Prove via `cli_sync_pull.feature` (create, update, move when feasible, local-only untouched, idempotent re-pull, baseline updated only on mutate success / untouched on no-op) plus `cli/tests/applyPull.test.ts` (and focused helper units) for taxonomy edge cases. Capability-named tests only — no phase numbers in product/test names.

### Plan / commit sizing (user request)
- **D-10:** Config granularity stays **coarse**. Phase 10 plans/commits must be **slightly larger** than Phases 8–9 micro-slices: prefer **1 plan** with **2–3 larger tasks** that each land a coherent observable chunk (e.g. apply create/move/update + units together; E2E + baseline proofs in the same plan when stop-safe). Avoid a separate tiny “E2E-only” plan unless a real wave dependency forces it. Prefer fewer commits that group related unit+E2E for the same behavior over per-test micro-commits. — **Reversibility:** reversible — planning preference only.

### Claude's Discretion
- Exact apply summary / reject wording and ordering (as long as D-02–D-06 hold and E2E proves them)
- Whether move is implemented as rename + write vs write-new + delete-old (must leave workspace correct and preserve local-only files)
- Exact baseline merge algorithm for paths not touched in this pull (keep prior agreed entries vs refresh from full remote export — choose the smallest change that keeps push baseline coherent)
- Whether rejects-only messaging reuses preview reject rendering helpers

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar
- `.planning/REQUIREMENTS.md` — EXP-03 (this phase); HYG-02 standing constraint
- `.planning/ROADMAP.md` — Phase 10 goal and success criteria
- `.planning/STATE.md` — Current milestone position
- `.planning/config.json` — `granularity: coarse` (D-10: prefer fewer/larger plans)

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Story 3 acceptance examples (5 bullets)

### Published triage (sole action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Story 3 verdict **strengthen**, gap proofs, delete/keep set, entrypoints, Phase 10 finish sketch
- `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` — D-01..D-04 dossier rules
- `.planning/phases/09-resolve-preview-before-pull-story-2/09-CONTEXT.md` — D-02..D-08 preview taxonomy / identity move / reject vocabulary (apply must align)
- `.planning/phases/08-resolve-pull-export-story-1/08-CONTEXT.md` — `doughnut_id` contract used for move identity

### Implementation targets (from TRIAGE)
- `cli/src/commands/notebook/syncSlashCommand.tsx` — `/sync` → dry-run vs apply
- `cli/src/sync/applyPull.ts` — mutating pull (primary strengthen)
- `cli/src/sync/previewPullActions.ts` — shared classify create/update/move/reject (reuse)
- `cli/src/sync/previewPull.ts` — dry-run (read-only reference; do not regress)
- `cli/src/sync/pushBaseline.ts` — `loadPushBaseline` / `savePushBaseline` for metadata gap
- `cli/src/sync/readWorkspace.ts`, `cli/src/sync/exportNotebook.ts`, `cli/src/sync/unzip.ts` — compare/apply inputs
- `e2e_test/features/cli/cli_sync_pull.feature` — capability E2E (flip anti-create)
- `cli/tests/applyPull.test.ts`, `cli/tests/syncArgument.test.ts` — unit coverage

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `applyPull` — currently intersecting-path overwrite only; skips remote-only; no baseline write; returns `N note(s) updated.` / `No changes to pull.`
- `classifyPreviewPullNotes` (`previewPullActions.ts`) — Phase 9 create/update/move/reject classification to reuse for apply
- `savePushBaseline` / `loadPushBaseline` — `.doughnut-sync/baseline.json` merge-base already used by export + push preview
- Green E2E: `cli_sync_pull.feature` (update, local-only untouched, anti-create, no-op, `@perfSync`)
- Green units: `cli/tests/applyPull.test.ts` (including intentional anti-create + perf)

### Established Patterns
- `/sync --dry-run` → `previewPull` (non-mutating); `/sync` → `applyPull` (mutating)
- Sync metadata under `.doughnut-sync/`; portable Markdown beside it
- Move inference requires exported `doughnut_id`; otherwise path-keyed only
- Reserved `index.md` / `log.md` and `.doughnut-sync/` never ordinary pull targets
- Capability E2E under `e2e_test/features/cli/`; CLI units under `cli/tests/`

### Integration Points
- `/sync <dir>` via `syncSlashCommand` → `applyPull`
- Shared export/unzip/readWorkspace with Stories 1–2 — change carefully; preserve dry-run non-mutation
- Push stories (12–13) will consume baseline written here

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- User asked to make commit granularity slightly bigger: keep `coarse`, and lock D-10 so Phase 10 prefers **1 plan / larger tasks** instead of Phase 9’s two micro-plans (~3–6 min each).
- TRIAGE finish sketch: keep intersecting update + no-op safety; add create/rename/move; update sync metadata after successful mutate pulls.
- Current E2E/unit intentionally assert remote-only notes are **not** created — those proofs must be inverted as part of strengthen.

</specifics>

<deferred>
## Deferred Ideas

- Story 4 `/lint` full portable contract — Phase 11
- Stories 5–6 push preview/push — Phases 12–13 (consume baseline semantics from D-05)
- Remote-driven **delete** of local notes when absent from export — out of oracle; not in this phase
- SEED-001 spelling follow-ons — parked

</deferred>

---

*Phase: 10-Resolve incremental pull (story 3)*
*Context gathered: 2026-08-03*
