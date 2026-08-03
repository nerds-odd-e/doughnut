# Phase 8: Resolve pull/export (story 1) - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Apply Phase 7’s Story 1 verdict (**strengthen**) in the tree so pull/export matches story 1 acceptance: hierarchy, stable Doughnut identity frontmatter, `index.md` indexes, usable ordinary Markdown links, usable remote attachment refs, portable content separated from sync state / no secrets, and failed pull not presented as success. Do not remove the capability. Do not change Terry Yin / Tan Yeong Sheng work (HYG-02). Defer Stories 2–3 sync/pull strengthen work to Phases 9–10.

</domain>

<decisions>
## Implementation Decisions

### Stable Doughnut identity frontmatter
- **D-01:** Restore stable identity as frontmatter key `doughnut_id` with value = note numeric id (string form matching prior export plan). Every exported note file must include it. — **Reversibility:** costly — push, lint, and sync preview/pull will treat this key as the portable identity contract; renaming later forces multi-surface rework.
- **D-02:** Merge identity into existing author-authored property frontmatter when present; if a note has no frontmatter, emit a minimal `doughnut_id`-only YAML block. Do not strip participant-intended author properties solely to inject id. — **Reversibility:** reversible — merge policy can be tightened later without breaking the `doughnut_id` contract.

### Link and attachment rewrite (close all Story 1 gaps)
- **D-03:** Phase 8 closes **all three** TRIAGE Story 1 gaps in this phase: (1) stable identity, (2) internal refs → ordinary Markdown links, (3) attachment refs remain usable while attachments stay remote. Partial “identity-only” is not enough for EXP-01. — **Reversibility:** costly — shipping identity without link/attachment proofs leaves EXP-01 incomplete and invites a second Story 1 phase.
- **D-04:** Wiki/internal note references in exported bodies are rewritten to ordinary relative Markdown links targeting the deterministic exported paths. Unresolvable targets get a documented, consistent fallback (researcher/planner pick exact algorithm; must be E2E-observable).
- **D-05:** Attachments are not copied into the zip; attachment references in note bodies are rewritten (or already emitted) so they remain usable as remote URLs after export. Prove with E2E under `cli_export.feature` (or equivalent capability E2E).

### Implementation surface
- **D-06:** Primary strengthen lands in backend zip construction (`NotebookZipBuilder` / export pipeline) so HTTP export and CLI `/export` share one zip shape. CLI proves end-to-end via `/export` + `cli_export.feature` + existing unit coverage; do not duplicate identity/link rewrite in the CLI unzip path. — **Reversibility:** costly — splitting rewrite across backend and CLI creates dual sources of truth for Stories 2–3 zip consumers.

### Story boundary / shared modules
- **D-07:** Do not change `applyPull` / preview-before-pull behavior in this phase. Touch shared modules (`exportNotebook`, `unzip`, baseline seed, registry) only when required to keep `/export` green or to honor the identity contract already seeded at export. Story 2–3 dossier actions stay deferred. — **Reversibility:** reversible — later phases still own those shared strengthen rows.
- **D-08:** Strengthening may reverse in-scope participant choices that removed identity (e.g. Eric Yeh “no id of its own”) because HYG-02 only protects Terry Yin / Tan Yeong Sheng — not LIA participant commits. Do not rewrite Terry/YS-owned hunks.

### Claude's Discretion
- Exact wiki-link resolution / path-mapping algorithm and attachment URL shape (as long as D-04/D-05 hold and E2E proves them)
- Unit vs E2E split for zip-builder edge cases; how many plans under **standard** granularity
- Whether title `# heading` + frontmatter merge ordering needs a tiny helper extract (keep cohesion with `NoteLeadingFrontmatter`)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar
- `.planning/REQUIREMENTS.md` — EXP-01 (this phase); HYG-02 standing constraint
- `.planning/ROADMAP.md` — Phase 8 goal and success criteria
- `.planning/STATE.md` — Current milestone position

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Story 1 acceptance examples (7 bullets)

### Published triage (sole action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Story 1 verdict **strengthen**, gap proofs, delete/keep set, entrypoints
- `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` — D-01..D-04 dossier rules carried into action phases

### Prior export / identity contract notes
- `docs/plans/2026-07-28-export-notebook-markdown-zip.md` — Historical `doughnut_id` + `index.md` alignment (may be partially superseded by current tree; use as intent, verify against code)
- `docs/plans/2026-07-28-cli-export-notebook.md` — CLI `/export` surface notes
- `docs/refinement/2026-07-27/QUESTIONS-for-export-team.md` — Sync-team contract questions (stable identity, determinism)

### Implementation targets (from TRIAGE)
- `backend/src/main/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilder.java` — zip note content (identity / links / attachments)
- `backend/src/main/java/com/odde/doughnut/services/NotebookExportService.java` — zip orchestration
- `cli/src/commands/notebook/exportSlashCommand.tsx` — `/export`
- `cli/src/sync/writeNotebookExport.ts` — unzip → filesystem + baseline seed
- `e2e_test/features/cli/cli_export.feature` — capability E2E
- `cli/tests/writeNotebookExport.test.ts` — unit coverage

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NotebookZipBuilder.noteFileContent` — current export note assembly (title heading + verbatim author frontmatter; **no** `doughnut_id`)
- `NoteLeadingFrontmatter` — existing frontmatter split helper used by zip builder
- CLI `/export` path: `exportSlashCommand` → `exportNotebook` → `writeNotebookExport` (unzip + `.doughnut-sync/baseline.json`)
- Green capability E2E: `cli_export.feature` (hierarchy, `index.md`, missing destination error, no `@wip`)

### Established Patterns
- Zip is the shared portable shape for CLI export and later sync pull consumers (Stories 2–3)
- Sync metadata lives under `.doughnut-sync/` beside portable Markdown (do not write credentials)
- Failures reject before partial success (`exportDestination`, unsafe zip entries, `AsyncAssistantFetchStage` abort)
- Participant Eric Yeh removed prior `doughnut_id` injection — triage marks restore as strengthen, not HYG-02 violation

### Integration Points
- Backend `GET` notebook export → zip bytes; CLI downloads and writes workspace
- Stories 2–3 consume the same zip via `exportNotebook` / `unzip` / `applyPull` — identity/link contract in the zip benefits them without implementing those stories here
- Push/lint later phases will round-trip on `doughnut_id` (D-01)

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- Granularity for this milestone bumped **fine → standard** before planning (fewer, slightly larger plans/commits).
- TRIAGE finish sketch: restore stable identity that round-trips with push/lint; cover wiki→ordinary links and attachment remote refs with E2E; keep hierarchy / `index.md` / sync-state separation.

</specifics>

<deferred>
## Deferred Ideas

- Story 2 preview reserved/duplicate/invalid-mapping reporting — Phase 9
- Story 3 incremental `applyPull` create/rename/move + metadata — Phase 10
- Stories 7–10 (create/rename/move/reconcile) — out of milestone
- SEED-001 spelling follow-ons — parked

</deferred>

---

*Phase: 8-Resolve pull/export (story 1)*
*Context gathered: 2026-08-03*
