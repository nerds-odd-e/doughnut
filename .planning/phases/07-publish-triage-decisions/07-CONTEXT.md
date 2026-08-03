# Phase 7: Publish triage decisions - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Publish a keep / strengthen / remove decision for each portable-workspace story 1–6, based only on non–Terry / non–Yeong Sheng participant work, citing acceptance examples from the portable-workspace note. Phases 8–13 must be able to act from this published triage alone (no re-audit). This phase does **not** apply keep/remove in the tree — that is Phases 8–13.

</domain>

<decisions>
## Implementation Decisions

### Evidence standard
- **D-01:** Each story’s published decision is an **actionable dossier**: verdict (keep / strengthen / remove) + acceptance-example citations + key participant paths/commands/tests + WIP/gap signals an executor needs. — **Reversibility:** costly — Phases 8–13 treat the dossier as the sole action source; thinning it later forces re-audit.
- **D-02:** Key paths include **capability entrypoints** (CLI command(s), main modules, matching E2E features) **plus** the concrete **delete/keep file set**, **and** a **whole participant-touched inventory** under the story surface so nothing is missed.
- **D-03:** When work overlaps stories, **duplicate shared paths under every related story’s dossier**, tagged as shared — no phase may skip a shared file because it was listed only elsewhere.
- **D-04:** WIP/gap signals use **labels + one concrete proof each** (e.g. `@wip` / half-wired / wrong acceptance / no external value, plus a pointer: scenario name, missing acceptance bullet, or broken command path). Not labels-only; not full repro bug reports.

### Claude's Discretion
- **Triage artifact location:** Publish as capability-named `TRIAGE.md` under `.planning/phases/07-publish-triage-decisions/` (not phase-numbered product code). CONTEXT/SUMMARY may point at it; Phases 8–13 read `TRIAGE.md` as the sole action source. (Area not selected for discussion.)
- **Strengthen bar:** Phase 7 locks verdict **strengthen** plus the gap list with proofs (per D-04). Intended finish criteria for the later action phase may be sketched briefly when obvious from the gaps; detailed strengthen implementation stays in Phases 8–13. (Area not selected.)
- **Author attribution:** Exclude Terry Yin, Tan Yeong Sheng, and `terryyin` variants from triage scope (HYG-02). Treat named LIA participants (Eric Yeh, Ben Huang, etta.huang, Joy-kgo, and peers such as Logan / XinxinKao on the CLI surface) as in-scope. Cursor Agent / mixed-author files: attribute by participant-authored hunks or commits where practical; do not rewrite Terry/YS-owned work. (Area not selected.)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip, keep/strengthen/remove bar
- `.planning/REQUIREMENTS.md` — TRIAGE-01, TRIAGE-02 (and HYG-02 standing constraint)
- `.planning/ROADMAP.md` — Phase 7 goal, success criteria, Phases 8–13 consumers
- `.planning/STATE.md` — Current milestone position

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Stories 1–6 acceptance examples (keep/remove oracle); stories 7–10 out of scope

### Participant surface (audit targets)
- `cli/` — CLI commands and workspace sync/lint/push behavior
- `e2e_test/features/cli/` — Capability E2E (`cli_export`, `cli_sync_*`, `cli_lint_workspace`, `cli_push*`)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- CLI slash/commands under `cli/src/commands/` including `lintSlashCommand.ts` and notebook-related paths
- E2E features already present: `cli_export.feature`, `cli_sync_pull.feature`, `cli_sync_dry_run.feature`, `cli_lint_workspace.feature`, `cli_push.feature`, `cli_push_dry_run.feature`

### Established Patterns
- Planning artifacts under `.planning/phases/NN-slug/`; product code named by capability, not phase number
- Decision bar from PROJECT: keep = correct + no WIP + external user value; strengthen = minor gaps; remove = WIP/incorrect/non-valuable
- Git history on `cli/` + `e2e_test/features/cli/` is dominated by Terry Yin / Tan Yeong Sheng — triage must filter those authors out

### Integration Points
- Published triage feeds Phases 8–13 one story each; Phase 14 verifies hygiene (HYG-01/02/03)
- No product code changes expected in Phase 7 — documentation/decision artifact only

</code_context>

<specifics>
## Specific Ideas

- User explicitly wanted **both** the entrypoints + delete/keep set **and** the full participant-touched inventory (not one or the other).
- Shared/overlapping files must appear in every related story dossier, tagged shared.

</specifics>

<deferred>
## Deferred Ideas

- Applying keep / strengthen / remove in the tree — Phases 8–13
- Author-attribution edge cases if audit discovers ambiguous ownership — resolve during Phase 7 research/execute without expanding milestone scope
- Stories 7–10 and spelling SEED-001 — already out of milestone scope

None — discussion stayed within phase scope for selected gray areas

</deferred>

---

*Phase: 7-Publish triage decisions*
*Context gathered: 2026-08-03*
