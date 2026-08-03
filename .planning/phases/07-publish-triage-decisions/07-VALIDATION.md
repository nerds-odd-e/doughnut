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

Wave plans also assert story-local citation counts when publishing each dossier (same oracle row). Shared-path (`shared`) presence is verified by `rg` after D-03 tagging tasks (stories 3 and 6).

## Out of scope for this phase

- Product unit/E2E tests (no `cli/` or `e2e_test/` mutation)
- RESEARCH.md package legitimacy (no new packages)
