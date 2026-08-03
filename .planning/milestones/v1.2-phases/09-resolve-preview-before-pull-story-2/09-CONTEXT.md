# Phase 9: Resolve preview-before-pull (story 2) - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Apply Phase 7’s Story 2 verdict (**strengthen**) in the tree so `/sync --dry-run` / `previewPull` matches story 2 acceptance: exact target paths and actions reported; reserved filenames, duplicate paths, and invalid mappings reported clearly; preview mutates nothing (Doughnut, workspace, sync metadata). Do not remove the capability. Do not change Terry Yin / Tan Yeong Sheng work (HYG-02). Do not implement Story 3 `applyPull` create/rename/move/metadata (Phase 10).

</domain>

<decisions>
## Implementation Decisions

### Gap coverage (EXP-02)
- **D-01:** Phase 9 closes **both** TRIAGE Story 2 gaps: (1) reserved / duplicate / invalid-mapping diagnostics, and (2) action taxonomy beyond content-overwrite diffs (create / update / reject, plus move when identity proves a path change). Partial “diagnostics-only” is not enough for EXP-02. — **Reversibility:** costly — shipping only one gap leaves EXP-02 incomplete and invites a second Story 2 phase.

### Preview action taxonomy
- **D-02:** Dry-run report labels each relevant path with an explicit action: **create** (remote `.md` with no workspace file), **update** (same path, content differs), **move** (same stable `doughnut_id` at a different path than the workspace file that holds that id), **reject** (reserved name / duplicate path / invalid mapping). Do not list every unchanged note individually; keep a concise summary (existing “N note(s) would change” style may expand to counts by action). — **Reversibility:** costly — CLI/E2E contract for preview wording becomes the Story 2 proof surface.
- **D-03:** **Move** is preview-only inference from `doughnut_id` path mismatch; do not invent moves without identity. Implementing filesystem create/rename/move on real `/sync` remains Phase 10 (`applyPull`). — **Reversibility:** reversible — Phase 10 can refine move application without changing the preview labels if they stay identity-based.

### Reserved / duplicate / invalid mappings
- **D-04:** Align reserved-name reporting with existing portable/OKF notions already used by workspace lint: at least `index.md` and `log.md` as reserved role files (not ordinary note create/update targets in the preview sense), plus anything under `.doughnut-sync/` as sync metadata (never a pull target). Duplicate paths = two exported notes resolving to the same workspace-relative path (or case-clash if the platform would collide). Invalid mappings = unsafe/out-of-tree paths, empty segments, or other non-portable path shapes the export/unzip path already rejects or should reject before write. Each finding names the path and a short actionable reason. — **Reversibility:** costly — lint (Phase 11) and preview should stay consistent on reserved vocabulary.
- **D-05:** Reject findings are first-class preview output (clearly visible), not silent skips. A dry-run that only has rejects (no create/update/move) still reports those rejects and must not claim a clean no-op if rejects exist.

### Non-mutation (already green — keep)
- **D-06:** Strengthen must preserve: preview only reads workspace + exports zip + compares in memory; no `writeFile`, no baseline/sync-metadata writes, no Doughnut mutations. Extend the existing E2E Rule `The preview leaves nothing behind` if new scenarios touch the filesystem.

### Implementation surface
- **D-07:** Primary strengthen lands in `previewPull` and report helpers (`diffReport` / related). Reuse `readWorkspace`, `exportNotebook`, `unzip` as compare inputs. Do **not** change `applyPull` behavior in this phase. Touch `syncSlashCommand` / `syncArgument` only if needed to surface the new report shape for `/sync --dry-run`. — **Reversibility:** costly — splitting preview logic into applyPull creates Story 2/3 entanglement.
- **D-08:** Prefer identity-aware create/move detection using exported `doughnut_id` (Phase 8 contract). If a remote note lacks `doughnut_id`, treat path-keyed create/update only (no move inference).

### Proof strategy
- **D-09:** Prove via `cli_sync_dry_run.feature` (integration: action labels, reserved/duplicate/invalid reporting, non-mutation) plus `cli/tests/previewPull.test.ts` (and small unit coverage for new helpers) for taxonomy edge cases. Capability-named tests only — no phase numbers in product/test names.

### Claude's Discretion
- Exact report wording / ordering / summary line format (as long as D-02–D-05 hold and E2E proves them)
- Whether reject vs update precedence when a path is both reserved and content-differing (prefer reject)
- Plan count under **coarse** granularity (prefer 1–2 plans: strengthen preview + E2E wrap-up, or a single combined plan if stop-safe)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar
- `.planning/REQUIREMENTS.md` — EXP-02 (this phase); HYG-02 standing constraint
- `.planning/ROADMAP.md` — Phase 9 goal and success criteria
- `.planning/STATE.md` — Current milestone position

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Story 2 acceptance examples (3 bullets)

### Published triage (sole action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Story 2 verdict **strengthen**, gap proofs, delete/keep set, entrypoints, Phase 9 finish sketch
- `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` — D-01..D-04 dossier rules
- `.planning/phases/08-resolve-pull-export-story-1/08-CONTEXT.md` — D-01 `doughnut_id` contract and D-07 (preview deferred to Phase 9)

### Implementation targets (from TRIAGE)
- `cli/src/commands/notebook/syncSlashCommand.tsx` — `/sync` → dry-run vs apply
- `cli/src/sync/previewPull.ts` — dry-run compare (primary strengthen)
- `cli/src/sync/diffReport.ts`, `cli/src/sync/unifiedDiff.ts` — report assembly
- `cli/src/sync/syncArgument.ts` — `--dry-run` parsing (shared → Story 3)
- `cli/src/sync/readWorkspace.ts`, `cli/src/sync/exportNotebook.ts`, `cli/src/sync/unzip.ts` — compare inputs
- `e2e_test/features/cli/cli_sync_dry_run.feature` — capability E2E
- `cli/tests/previewPull.test.ts`, `cli/tests/syncArgument.test.ts` — unit coverage
- `cli/src/lint/lintWorkspace.ts` — reserved `index.md` / `log.md` vocabulary to align with (read-only reference; do not implement Story 4 here)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `previewPull` — reads workspace, exports zip, in-memory unzip, content-diff intersecting `.md` paths; returns `renderDiffReport` or `No changes to pull.`
- `diffReport` / `unifiedDiff` — path + unified hunk + “N note(s) would change”
- Green E2E: `cli_sync_dry_run.feature` (changed note, local edit overwrite preview, no-diff, workspace-not-written Rule)
- Phase 8 zip now carries `doughnut_id` — enables identity-based **move** preview without touching `applyPull`

### Established Patterns
- Dry-run never writes; real pull is `applyPull` (Story 3 / Phase 10)
- Sync metadata under `.doughnut-sync/`; portable Markdown beside it
- Lint already treats `index.md` / `log.md` as reserved roles — preview diagnostics should reuse that vocabulary
- Capability E2E under `e2e_test/features/cli/`; CLI units under `cli/tests/`

### Integration Points
- `/sync --dry-run <dir>` via `syncSlashCommand` → `previewPull`
- Shared export/unzip/readWorkspace with Stories 1 and 3 — change carefully; no applyPull strengthen here
- Phase 10 will apply create/rename/move; Phase 9 only previews them where detectable

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- Planning granularity set to **coarse** (was standard) so Phase 9 plans/commits are slightly larger (typically 1–3 plans).
- TRIAGE finish sketch: keep non-mutating dry-run; add reserved/duplicate/invalid reporting; expand actions beyond content overwrite diffs.

</specifics>

<deferred>
## Deferred Ideas

- Story 3 `applyPull` create / rename / move + sync-metadata updates — Phase 10
- Story 4 `/lint` full portable contract — Phase 11 (may share reserved-name vocabulary)
- Stories 5–6 push preview/push — Phases 12–13
- SEED-001 spelling follow-ons — parked

</deferred>

---

*Phase: 9-Resolve preview-before-pull (story 2)*
*Context gathered: 2026-08-03*
