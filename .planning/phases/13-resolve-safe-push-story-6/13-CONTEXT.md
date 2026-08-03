# Phase 13: Resolve safe push (story 6) - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Apply Phase 7’s Story 6 verdict (**remove**) in the tree so Story-6-only WIP for mutating `/push` is cleanly gone: delete `@ignore` `cli_push.feature` (and any other Story-6-only WIP debris), leave the shared dry-run `/push --dry-run` surface from Phase 12 intact, and do **not** implement a mutating push / `applyPush`. Do not change Terry Yin / Tan Yeong Sheng work (HYG-02). PUSH-02 closes as **removed cleanly**, not as a green mutate-push capability.

</domain>

<decisions>
## Implementation Decisions

### Verdict application (PUSH-02)
- **D-01:** Phase 13 applies TRIAGE Story 6 **remove**. Do **not** strengthen or build mutating `/push`, version-safe update, conflict-refuse-on-mutate, or post-push baseline refresh. Those acceptance bullets remain unmet by design until a future milestone; this phase only clears WIP debris. — **Reversibility:** one-way — deleting the `@ignore` feature removes the unfinished Story 6 E2E scaffold (rebuild later if product wants mutate push).

### Delete set
- **D-02:** Delete `e2e_test/features/cli/cli_push.feature` (TRIAGE sole Story-6-only delete target). Before close, quickly scan for other Story-6-only WIP that exists solely for that feature (orphaned glue, spent training plans that describe only mutate push). Do **not** delete shared dry-run modules, `cli_push_dry_run.feature`, or `docs/plans/2026-07-30-cli-push-dry-run-known-issues.md` (Story 5 / dry-run surface — Phase 14 hygiene if still spent). — **Reversibility:** one-way for the feature file; shared keep set is reversible polish only.

### Shared dry-run surface (leave Phase 12)
- **D-03:** Keep TRIAGE shared keep set untouched for behavior: `pushSlashCommand`, `previewPush`, `pushArgument` (still requires `--dry-run`), `pushBaseline`, `diffReport`, export/read/unzip helpers, and their units / `cli_push_dry_run.feature`. Phase 12 already closed PUSH-01; Phase 13 must not regress dry-run. — **Reversibility:** costly if shared readers/preview regress export/pull/dry-run.
- **D-04:** Optional light help/doc polish only: if `pushDoc` still says “Only --dry-run is supported so far.” (WIP tone implying mutate is coming), rephrase to durable dry-run-only product copy with no promise of a future mutate push in this command. Do **not** relax `parsePushArgument`’s `--dry-run` requirement. — **Reversibility:** reversible — copy only.

### Proof strategy
- **D-05:** Prove remove via: (1) `cli_push.feature` absent from the tree; (2) no Story-6 `@ignore` mutate-push E2E left under `e2e_test/features/cli/`; (3) `parsePushArgument` still rejects non–dry-run; (4) no `applyPush` (or equivalent) module appears; (5) targeted `cli_push_dry_run` E2E (or existing green dry-run units) still pass so shared surface is intact; (6) mark **PUSH-02** complete in REQUIREMENTS/ROADMAP as **removed cleanly**. Capability-named artifacts only — no phase numbers in product/test names. — **Reversibility:** reversible for planning checkboxes; proofs are the contract for Phase 14 hygiene.

### Plan / commit sizing (user request)
- **D-06:** Config granularity stays **coarse** (already max). Phase 13 plans/commits must be **slightly larger than Phase 12**: prefer **1 plan** with **1 task** that deletes WIP + optional help polish + REQUIREMENTS/ROADMAP/STATE closure together. Prefer **one implementation commit** (or one code commit + one docs commit only if hooks force it) — avoid separate micro-commits for “delete file” vs “help tweak” vs “mark PUSH-02 done”. — **Reversibility:** reversible — planning/execution preference only.

### Claude's Discretion
- Exact durable wording for dry-run-only `/push` help (as long as D-04 holds and no mutate promise)
- Whether a tiny unit asserting `--dry-run` mandatory remains untouched or gets a one-line comment cleanup
- Whether spent Story-6-only training notes under `docs/plans/` or `.planning/` are left for Phase 14 vs deleted here if unambiguously mutate-push-only debris

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar; WIP remove-by-default
- `.planning/REQUIREMENTS.md` — PUSH-02 (this phase); HYG-02 standing constraint
- `.planning/ROADMAP.md` — Phase 13 goal and success criteria
- `.planning/STATE.md` — Current milestone position
- `.planning/config.json` — `granularity: coarse` (D-06: fewer/larger plans & commits)

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Story 6 acceptance examples (5 bullets) — cited as gaps justifying **remove**, not as Phase 13 implement targets

### Published triage (sole action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Story 6 verdict **remove**, delete/keep set, WIP proofs, Phase 13 finish sketch
- `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` — D-01..D-04 dossier rules
- `.planning/phases/12-resolve-push-dry-run-story-5/12-CONTEXT.md` — D-06 Story 6 boundary (Phase 12 left `cli_push.feature` for Phase 13); dry-run surface to preserve
- `.planning/phases/12-resolve-push-dry-run-story-5/12-01-SUMMARY.md` — PUSH-01 closed; shared dry-run green baseline for Phase 13 non-regression

### Implementation targets (from TRIAGE)
- `e2e_test/features/cli/cli_push.feature` — `@ignore` WIP — **delete**
- `cli/src/commands/notebook/pushSlashCommand.tsx` — dry-run `/push` (keep; optional D-04 help polish)
- `cli/src/sync/pushArgument.ts` — requires `--dry-run` (keep; do not relax)
- `cli/src/sync/previewPush.ts` — conflict-aware preview (keep; Phase 12)
- `cli/src/sync/pushBaseline.ts` — baseline helpers (keep)
- `e2e_test/features/cli/cli_push_dry_run.feature` — dry-run E2E (keep; non-regression)
- `cli/tests/pushArgument.test.ts`, `previewPush.test.ts`, `pushBaseline.test.ts` — keep

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Green Phase 12 dry-run: `previewPush` + `cli_push_dry_run.feature` (load-only baseline, create/update reporting, conflicts)
- `parsePushArgument` — still mandates `--dry-run`; USAGE on mutate attempt
- `pushDoc` — usage `/push --dry-run <workspace path>`; description still WIP-toned (“so far”)
- No `applyPush` / mutate module under `cli/src/sync/`

### Established Patterns
- TRIAGE delete/keep sets are the sole action source for Phases 8–13
- WIP remove-by-default: `@ignore` half-wired E2E without keepable capability → delete
- Capability E2E under `e2e_test/features/cli/`; do not invent mutate-push scenarios in this phase
- Prefer `trash` over `rm -f` when deleting files in this repo

### Integration Points
- `/push` registry stays dry-run-only after remove
- Phase 14 hygiene verify will re-check no leftover Story 1–6 WIP and HYG-02

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- User asked again to make commit granularity slightly bigger: keep `coarse`, and lock D-06 so Phase 13 prefers **1 plan / 1 task** and **one implementation commit** bundling delete + optional help polish + PUSH-02 closure — slightly larger than Phase 12’s already-coarse shape by avoiding micro-splits.
- TRIAGE finish sketch: delete `cli_push.feature`; leave shared dry-run to Phase 12; future safe mutate push is new work, not strengthen of this empty surface.

</specifics>

<deferred>
## Deferred Ideas

- Implementing Story 6 mutate push (body/frontmatter update, version guard, conflict refuse, metadata refresh, idempotent re-push) — future milestone, not Phase 13
- Broader spent training-doc cleanup (`docs/plans/*`, phase diaries) — Phase 14 HYG-01
- SEED-001 spelling follow-ons — parked
- Stories 7–10 portable create-rename-move — out of milestone

</deferred>

---

*Phase: 13-Resolve safe push (story 6)*
*Context gathered: 2026-08-03*
