# Phase 7 Validation (docs-only)

**Mode:** Nyquist Dimension 8 for a docs-only phase (`--skip-research`). No runtime test suite; automated checks are shell/`python3` asserts over `TRIAGE.md`.

**Artifact under test:** `.planning/phases/07-publish-triage-decisions/TRIAGE.md`

**Oracle:** `.planning/notes/2026-07-24-portable-notebook-workspace.md` acceptance bullets per story.

## Hardened completeness checks (plan 07-03 Task 3)

These asserts **must exit non-zero** when TRIAGE-01/02 is incomplete:

| Check | Pass condition | Fail condition |
|-------|----------------|----------------|
| Author filter | File contains `Author filter` | Missing author-filter text |
| Summary verdicts | Exactly six Summary rows (stories 1–6) each with Verdict `keep` \| `strengthen` \| `remove` | Any story missing from Summary; Verdict is `Pending`, `TBD`, or other token |
| Section schema | Each `## Story N:` has `### Verdict`, Acceptance citations, Capability entrypoints, Delete/keep, Participant-touched | Any required heading missing |
| Citation oracle | Annotated citation lines (`— match\|gap\|N/A`) per story equal oracle counts | Count mismatch |

### Citation oracle (acceptance bullets)

| Story | Expected annotated citations |
|-------|------------------------------|
| 1 | 7 |
| 2 | 3 |
| 3 | 5 |
| 4 | 4 |
| 5 | 4 |
| 6 | 5 |

Citation lines are counted as dossier bullets matching `— match`, `— gap`, or `— N/A` (case-insensitive) under each story section (PATTERNS skeleton shape).

## Per-story checks (plans 07-01 / 07-02 / 07-03)

Wave plans assert story-local annotated citation counts when publishing each dossier (same oracle row). Plan **07-01 Task 2** additionally requires Summary story-1 Verdict ∈ `{keep, strengthen, remove}` (not `TBD` / `Pending`) and story-1-scoped citation count `== 7` — same python section-scope shape as 07-02/07-03 story tasks. Shared-path (`shared`) presence is verified by `rg` after D-03 tagging tasks (stories 3 and 6).

## Pointer checks (plan 07-03 Task 4)

Split per file (do not require the literal `TRIAGE.md` string in every planning doc):

| File | Pass condition |
|------|----------------|
| `07-CONTEXT.md` | Contains path `07-publish-triage-decisions/TRIAGE.md` |
| `STATE.md` | Progress / next-action wording for Phase 7 published → Phase 8 (e.g. `Phase 8`, `triage published`, or `next`) |
| `ROADMAP.md` | Phase 7 Plans list includes `07-01`, `07-02`, `07-03` |

## Out of scope for this phase

- Product unit/E2E tests (no `cli/` or `e2e_test/` mutation)
- RESEARCH.md package legitimacy (no new packages)
