# Phase 12: Resolve push dry-run (story 5) - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Apply Phase 7’s Story 5 verdict (**strengthen**) in the tree so `/push --dry-run` / `previewPush` matches story 5 acceptance: the preview distinguishes unchanged, locally changed, remotely changed, and divergent notes; it reports exact create and update actions; divergent edits are conflicts (not last-write-wins); the preview does **not** mutate Doughnut, local `.md` files, or sync metadata. Do not remove the capability. Do not change Terry Yin / Tan Yeong Sheng work (HYG-02). Do not implement Story 6 mutating `/push` (Phase 13 owns remove of `@ignore` `cli_push.feature` WIP).

</domain>

<decisions>
## Implementation Decisions

### Gap coverage (PUSH-01)
- **D-01:** Phase 12 closes **both** TRIAGE Story 5 gaps: (1) report exact **create** and **update** actions (including non-intersecting paths the oracle expects), and (2) make dry-run **non-mutating for sync metadata** (stop writing `.doughnut-sync/baseline.json` from `previewPush`). Keep already-green conflict labeling (`(push)` / `(pull)` / `(CONFLICT)`) and non-mutation of Doughnut / workspace `.md`. Partial “taxonomy-only” or “baseline-only” is not enough for PUSH-01. — **Reversibility:** costly — shipping one gap leaves PUSH-01 incomplete and invites a second Story 5 phase.

### Sync metadata (oracle no-mutation)
- **D-02:** Remove `savePushBaseline` from the dry-run path. `previewPush` may **load** an existing baseline (from `/export` seed or Phase 10 successful pull) to classify push/pull/conflict, but must **not write** `.doughnut-sync/` or any other sync metadata. — **Reversibility:** one-way — published E2E currently asserts “preview’s only addition is its own baseline file”; that contract flips to “preview adds nothing.”
- **D-03:** Re-prime directional E2E via **export** (existing Rule: *Exporting primes the baseline…*) or a successful pull — not via a prior dry-run. Flip/replace units that assert dry-run seeds or advances baseline (`seeds the baseline only with…`, `advances the baseline…`, `keeps the baseline…`, etc.). Keep `pushBaseline` helpers intact for export / pull / future Story 6 consumers. — **Reversibility:** one-way — dry-run no longer owns baseline bookkeeping.

### Create / update action taxonomy
- **D-04:** Keep directional status labels `(push)` / `(pull)` / `(CONFLICT)` for notes with a merge-base. Add explicit **create** vs **update** action reporting the oracle asks for:
  - **Update (push):** path exists on both sides; only workspace changed vs baseline (or unlabeled difference that is a content change on an existing remote note).
  - **Create (push):** local-only `.md` (no remote counterpart) that would be a new Doughnut note on a real push.
  - **Update/create (pull):** remote-only or remote-changed notes labeled `(pull)` with create vs update against local absence/presence.
  - **Conflict:** both sides diverged — never reported as update. — **Reversibility:** costly — CLI/E2E wording becomes the Story 5 proof surface.
- **D-05:** Expand beyond intersecting exported∩local only: include **local-only** and **remote-only** Markdown paths in the report so creates are visible. Unchanged intersecting notes stay omitted / contribute to `No changes to push.` when nothing else reports. Reserved `index.md` / `log.md` and `.doughnut-sync/` stay out of ordinary create/update rows (align with Phase 9/10 reserved vocabulary). — **Reversibility:** costly — omitting non-intersecting creates leaves the TRIAGE create/update gap open.

### Surface / Story 6 boundary
- **D-06:** Primary strengthen lands in `previewPush` (+ small helpers / report rendering as needed). Touch `pushSlashCommand` only for help/doc if required. Do **not** implement mutating `/push`, do **not** relax `parsePushArgument`’s `--dry-run` requirement, and do **not** delete `cli_push.feature` (Phase 13). Prefer not changing shared `readWorkspace` / `exportNotebook` / `pushBaseline` APIs unless a dry-run proof is blocked. — **Reversibility:** reversible for help text; costly if shared readers regress export/pull.

### Proof strategy
- **D-07:** Prove via `cli_push_dry_run.feature` (create + update actions, conflict preserved, **zero** sync-metadata / `.md` / Doughnut mutation, export-primed directional labels) plus `cli/tests/previewPush.test.ts` (and focused helpers) for taxonomy/baseline-load edge cases. Capability-named tests only — no phase numbers in product/test names.

### Plan / commit sizing (user request)
- **D-08:** Config granularity stays **coarse** (already max). Phase 12 plans/commits must be **slightly larger than Phase 11**: prefer **1 plan** with **1 (at most 2) larger task(s)** that land **both** gaps + units + E2E together. Prefer **one implementation commit** (or one commit for code+units and one for E2E only if stop-safe forces it) — avoid Phase 11’s separate units / E2E / refactor micro-commits. — **Reversibility:** reversible — planning/execution preference only.

### Claude's Discretion
- Exact create/update label wording and whether it appears beside `(push)`/`(pull)` or replaces the path header line
- Whether remote-only create uses the same `renderNoteDiff` shape or a create-only line without a useless empty-side diff
- How to phrase the flipped non-mutation E2E (inventory with no baseline file vs assert baseline mtime/content unchanged when already present)
- Small report-helper extractions from `diffReport` vs inline in `previewPush`

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar
- `.planning/REQUIREMENTS.md` — PUSH-01 (this phase); HYG-02 standing constraint
- `.planning/ROADMAP.md` — Phase 12 goal and success criteria
- `.planning/STATE.md` — Current milestone position
- `.planning/config.json` — `granularity: coarse` (D-08: fewer/larger plans & commits)

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Story 5 acceptance examples (4 bullets)

### Published triage (sole action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Story 5 verdict **strengthen**, gap proofs, delete/keep set, entrypoints, Phase 12 finish sketch
- `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` — D-01..D-04 dossier rules
- `.planning/phases/10-resolve-incremental-pull-story-3/10-CONTEXT.md` — baseline written on successful mutate pull (D-05 there); dry-run must load, not write
- `.planning/phases/11-resolve-workspace-lint-story-4/11-CONTEXT.md` — coarse sizing precedent (D-11); Phase 12 D-08 goes slightly larger
- `.planning/phases/09-resolve-preview-before-pull-story-2/09-CONTEXT.md` — reserved / invalid-mapping vocabulary for what dry-run must not treat as ordinary notes

### Implementation targets (from TRIAGE)
- `cli/src/commands/notebook/pushSlashCommand.tsx` — `/push` entry (dry-run-only; keep)
- `cli/src/sync/previewPush.ts` — conflict-aware preview (primary strengthen)
- `cli/src/sync/pushArgument.ts` — requires `--dry-run` (keep; Story 6 boundary)
- `cli/src/sync/pushBaseline.ts` — load/save helpers (load-only from dry-run)
- `cli/src/sync/diffReport.ts` — report rendering (extend carefully)
- `cli/src/sync/readWorkspace.ts`, `exportNotebook.ts`, `unzip.ts` — shared inputs
- `cli/src/sync/writeNotebookExport.ts` — export baseline seed (preferred priming path)
- `e2e_test/features/cli/cli_push_dry_run.feature` — capability E2E (flip baseline-write scenarios)
- `cli/tests/previewPush.test.ts`, `pushArgument.test.ts`, `pushBaseline.test.ts` — units

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `previewPush` — classifies vs baseline; reports path + unified diff + `(push)`/`(pull)`/`(CONFLICT)`; **always** `savePushBaseline(nextBaseline(...))` today
- `classify` / `nextBaseline` — merge-base semantics (directional labels need a prior agreed baseline)
- `loadPushBaseline` / `savePushBaseline` — `.doughnut-sync/baseline.json`; export + pull already write; dry-run must stop writing
- Green E2E: `cli_push_dry_run.feature` (first difference, directional labels after priming dry-run, conflict, export-primed push, baseline-file addition)
- Green units: `previewPush.test.ts` including intentional baseline seed/advance proofs

### Established Patterns
- `/push --dry-run` only — `parsePushArgument` rejects mutate push (Story 6 remove path)
- Export Rule already proves directional labels without a priming dry-run
- Phase 9/10 reserved `index.md` / `log.md` / `.doughnut-sync/` vocabulary
- Capability E2E under `e2e_test/features/cli/`; CLI units under `cli/tests/`

### Integration Points
- `/push --dry-run <dir>` → `pushSlashCommand` → `previewPush`
- Baseline consumers: export seed, Phase 10 applyPull success, (future) Story 6 mutate push
- Shared export/unzip/readWorkspace with Stories 1–3 — change carefully; preserve pull/export

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- User asked again to make commit granularity slightly bigger: keep `coarse`, and lock D-08 so Phase 12 prefers **1 plan / 1–2 larger tasks** and **fewer commits than Phase 11** (combine units+E2E; avoid separate refactor micro-commits).
- TRIAGE finish sketch: keep conflict-aware dry-run labeling and non-mutation of Doughnut / `.md`; stop writing baseline on preview; add create vs update (and non-intersecting path) reporting.
- Current E2E Feature blurb and Scenario *The preview's only addition is its own baseline file* intentionally allow metadata mutation — those proofs must be inverted as part of strengthen.

</specifics>

<deferred>
## Deferred Ideas

- Story 6 safe push / remove `@ignore` `cli_push.feature` — Phase 13
- Mutating `/push` implementation — out of this milestone’s Story 6 remove verdict
- SEED-001 spelling follow-ons — parked
- Stories 7–10 portable create-rename-move — out of milestone

</deferred>

---

*Phase: 12-Resolve push dry-run (story 5)*
*Context gathered: 2026-08-03*
