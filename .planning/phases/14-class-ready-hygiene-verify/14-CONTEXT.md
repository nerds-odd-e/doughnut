# Phase 14: Class-ready hygiene verify - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Close the v1.2 milestone hygiene bar: confirm stories 1–6 participant WIP/debris are gone (HYG-01), Terry Yin / Tan Yeong Sheng work remains untouched by this milestone’s removals/rewrites (HYG-02 final verify), and retained portable-workspace CLI capabilities are green under targeted unit + CLI E2E (HYG-03). This is a **verify + light cleanup** phase — not a new product capability and not a re-triage of Phases 8–13 verdicts. Do not implement Story 6 mutate push. Do not rewrite Terry/YS files while verifying.

</domain>

<decisions>
## Implementation Decisions

### HYG-01 — spent debris cleanup
- **D-01:** Treat HYG-01 as a **product-tree** sweep: remove leftover Stories 1–6 training WIP and spent docs that no longer describe the tree; do **not** re-open keep/strengthen/remove verdicts from TRIAGE or re-implement closed EXP/LINT/PUSH gaps. — **Reversibility:** reversible for planning checkboxes; one-way for deleted spent docs.
- **D-02:** **Delete set (spent training docs under `docs/plans/`):** trash these three plans that are WIP-toned, outdated vs Phase 12/13, or spent agent-execution diaries for portable workspace:
  - `docs/plans/2026-07-30-cli-push-dry-run-known-issues.md` (claims unfixed issues Phase 12 already closed; stale checkout narrative)
  - `docs/plans/2026-07-28-cli-export-notebook.md` (Status: In progress — spent training plan)
  - `docs/plans/2026-07-28-export-notebook-markdown-zip.md` (spent agent plan artifact under `docs/plans/`; product behavior lives in code + E2E)
  Prefer `trash` over `rm`. — **Reversibility:** one-way — files leave the tree (recoverable from git history).
- **D-03:** **Keep / do not delete:** living oracle `.planning/notes/2026-07-24-portable-notebook-workspace.md`; retained capability code/tests/E2E from Phases 8–12 (`cli_export`, `cli_sync_*`, `cli_lint_workspace`, `cli_push_dry_run`, dry-run `/push`); TRIAGE.md and phase CONTEXT/SUMMARY under `.planning/phases/` for this milestone. Bulk archive of `.planning/phases/07–13` diaries is **out of this phase** — defer to `/gsd-complete-milestone` / `/gsd-cleanup` after HYG closes. — **Reversibility:** costly if oracle or green capability surfaces are deleted by mistake.
- **D-04:** **WIP scan proof:** confirm no `@wip` / `@ignore` under `e2e_test/features/cli/` for stories 1–6; `cli_push.feature` remains absent; no `applyPush` (or equivalent mutate-push) module. If any new Story 1–6-only orphaned WIP appears during the scan, trash it under the same remove-by-default bar — do not invent product features to “finish” it. — **Reversibility:** one-way for deletes; scan itself is reversible.

### HYG-02 — Terry / Yeong Sheng untouched (final verify)
- **D-05:** Prove HYG-02 with a **bounded author/file audit**, not a full history rewrite. Method: (1) list protected surfaces called out in TRIAGE / prior CONTEXT (notably Terry-authored `cli/src/sync/previewPullActions.ts` and any Tan Yeong Sheng–attributed paths named in TRIAGE); (2) confirm Phases 8–13 treated them as **import-only / do-not-rewrite** (spot-check `git log` / blame / milestone diffs — no content rewrites of those files beyond allowed participant-owned neighbors); (3) record evidence in the phase SUMMARY/VERIFICATION. Do **not** “clean,” reformat, or refactor Terry/YS files as part of verify. — **Reversibility:** reversible — audit/docs only; rewriting Terry/YS would violate HYG-02 itself.

### HYG-03 — retained capability green proof
- **D-06:** Prove green with the **retained CLI capability matrix** (targeted, not full E2E suite):
  - Units: `CURSOR_DEV=true nix develop -c pnpm cli:test` (or equivalent focused suite covering export/sync/lint/push dry-run helpers)
  - Targeted E2E (assume `pnpm sut` already running): `cli_export.feature`, `cli_sync_dry_run.feature`, `cli_sync_pull.feature`, `cli_lint_workspace.feature`, `cli_push_dry_run.feature`
  Do **not** require full Cypress suite or unrelated CLI features (`cli_gmail`, `cli_recall`, etc.) for HYG-03. — **Reversibility:** reversible — proof set can widen later; narrowing below this matrix leaves HYG-03 incomplete.

### Close-out
- **D-07:** On green proofs + debris gone + HYG-02 audit recorded: mark **HYG-01 / HYG-02 / HYG-03** complete in REQUIREMENTS.md and ROADMAP Phase 14 success criteria; update STATE for milestone-ready handoff. Capability-named artifacts only — no phase numbers in product/test names. — **Reversibility:** reversible for planning checkboxes.

### Plan / commit sizing (user request)
- **D-08:** Config granularity stays **coarse** (already max — `config.json` `granularity: coarse`). Phase 14 plans/commits must be **slightly larger than Phase 13**: prefer **1 plan** with **1 task** that lands debris trash + HYG-02 audit notes + full HYG-03 green matrix + REQUIREMENTS/ROADMAP/STATE close **together**. Prefer **one implementation commit** bundling cleanup + verify evidence + planning close (or one product/docs commit + one planning commit only if hooks force a split) — avoid separate micro-commits for “trash docs” vs “run tests” vs “tick HYG boxes”. — **Reversibility:** reversible — planning/execution preference only.

### Claude's Discretion
- Exact SUMMARY/VERIFICATION wording for the HYG-02 audit table
- Whether to run the five E2E specs in one Cypress invocation or sequential `--spec` calls
- Tiny non-product formatting of REQUIREMENTS/ROADMAP close-out text
- Whether any additional spent note under `docs/` (outside the D-02 trio) turns up in the scan and clearly matches HYG-01 — trash only if unambiguously Stories 1–6 training debris that misrepresents the tree

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar; WIP remove-by-default; class-ready goal
- `.planning/REQUIREMENTS.md` — HYG-01, HYG-02, HYG-03 (this phase); prior EXP/LINT/PUSH marked complete
- `.planning/ROADMAP.md` — Phase 14 goal and success criteria (4 bullets)
- `.planning/STATE.md` — Current position after Phase 13; next = Phase 14
- `.planning/config.json` — `granularity: coarse` (D-08: fewer/larger plans & commits)

### Acceptance oracle & triage
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Stories 1–6 acceptance oracle (living; keep)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Verdicts + author filter + delete/keep sets; HYG-02 author appendix
- `.planning/phases/13-resolve-safe-push-story-6/13-CONTEXT.md` — Deferred broader spent-doc cleanup to Phase 14; dry-run keep set; PUSH-02 removed cleanly
- `.planning/phases/13-resolve-safe-push-story-6/13-01-SUMMARY.md` — Notes dry-run known-issues left for Phase 14 HYG-01
- `.planning/phases/12-resolve-push-dry-run-story-5/12-CONTEXT.md` — Shared dry-run surface to preserve under HYG-03
- `.cursor/rules/planning.mdc` — Active history cleanup when plan/milestone done; targeted E2E (not full suite)

### Spent docs (delete targets)
- `docs/plans/2026-07-30-cli-push-dry-run-known-issues.md` — D-02 trash
- `docs/plans/2026-07-28-cli-export-notebook.md` — D-02 trash
- `docs/plans/2026-07-28-export-notebook-markdown-zip.md` — D-02 trash

### Retained proof surfaces (HYG-03)
- `e2e_test/features/cli/cli_export.feature`
- `e2e_test/features/cli/cli_sync_dry_run.feature`
- `e2e_test/features/cli/cli_sync_pull.feature`
- `e2e_test/features/cli/cli_lint_workspace.feature`
- `e2e_test/features/cli/cli_push_dry_run.feature`
- `cli/` unit tests via `pnpm cli:test`
- `cli/src/sync/previewPullActions.ts` — Terry-authored; HYG-02 import-only (do not rewrite)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 13 already removed `cli_push.feature`; no `@wip`/`@ignore` under `e2e_test/features/cli/` at discuss time
- Green retained E2E set from Phases 8–12 (export, sync dry-run, sync pull, lint, push dry-run)
- `pnpm cli:test` + targeted `pnpm cypress run --spec …` patterns from `.planning/codebase/TESTING.md`
- `trash` available in Nix env for deletes

### Established Patterns
- HYG-02 standing constraint throughout Phases 8–13 — Phase 14 only verifies, does not rewrite instructors
- WIP remove-by-default; capability-named E2E only
- Coarse plan sizing ratchet: Phases 10→13 each preferred fewer/larger commits; Phase 14 continues (D-08)
- Milestone diary archive deferred to complete-milestone/cleanup — not mid-verify mass delete of `.planning/phases/`

### Integration Points
- REQUIREMENTS/ROADMAP/STATE close-out when HYG-01..03 proofs land
- Optional later `/gsd-complete-milestone` for phase-dir archive after this phase

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- User asked again to make commit granularity slightly bigger: keep `coarse` (already max in config), and lock D-08 so Phase 14 prefers **1 plan / 1 task** and **one implementation commit** bundling debris cleanup + HYG-02 audit + HYG-03 green matrix + HYG checkbox close — slightly larger than Phase 13 by avoiding micro-splits across the three HYG requirements.
- Scout found `cli_push.feature` already absent and no `@wip`/`@ignore` in CLI features — HYG-01 is primarily spent `docs/plans/` + confirmation scan, not another code remove wave.

</specifics>

<deferred>
## Deferred Ideas

- Bulk archive/prune of `.planning/phases/07–13` diaries — `/gsd-complete-milestone` / `/gsd-cleanup` after HYG closes
- Implementing Story 6 mutate push — future milestone (PUSH-02 already removed cleanly)
- Stories 7–10 portable create/rename/move — out of milestone
- SEED-001 spelling follow-ons — parked
- Full Cypress suite as a release gate — not required for HYG-03

</deferred>

---

*Phase: 14-Class-ready hygiene verify*
*Context gathered: 2026-08-03*
