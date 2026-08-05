# Phase 11: Add as overlapped note - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-05
**Phase:** 11-add-as-overlapped-note
**Mode:** `--auto`
**Areas discussed:** Per-row CTA & persist path, Post-success dialog/result behavior, AMR-09 no-regrade fence, AMR-07 gate reuse, Test coverage strategy

---

## Per-row CTA & persist path

| Option | Description | Selected |
|--------|-------------|----------|
| Per-row CTA + `appendOverlapWikiLinkToNoteContent` + `updateTextField` | Wire Phase 10 util from resolve row; content mutation on reviewed note | ✓ |
| Plain `appendAliasToNoteContent` with title string | Faster but Pitfall 5 (plain alias, no future OVERLAP) | |
| Nested confirm Modal before save | Extra friction; nested-dialog risk | |

**User's choice:** [auto] Per-row CTA + Phase 10 util + `updateTextField` (recommended default)
**Notes:** Host mutate in dialog host; keep AnsweredSpellingQuestion outcome chrome untouched.

---

## Post-success dialog/result behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Return to match list in same Modal; stay on ACCIDENTAL_MATCH chrome | Multi-match friendly; mirrors Phase 9 | ✓ |
| Always dismiss resolve Modal after declare | Simpler; worse for second match | |
| Flip to OVERLAP try-again chrome | Violates AMR-09 / Pitfall 4 | |

**User's choice:** [auto] Return to list; stay on ACCIDENTAL_MATCH (recommended default)
**Notes:** Null append → no save, stay on list (AMR-10 quiet state deferred).

---

## AMR-09 no-regrade fence

| Option | Description | Selected |
|--------|-------------|----------|
| Content mutation only; leave outcome/schedule; assert no try-again | ADR 0003 / Pitfall 4 compliant | ✓ |
| Emit `retry` after declare so user can reclaim | Explicitly out of scope / locked anti-pattern | |
| Re-grade answer as OVERLAP | Corrupts ACCIDENTAL_MATCH penalty already applied | |

**User's choice:** [auto] Content mutation only; assert no try-again / reclaim (recommended default)
**Notes:** Highest-risk behavior pitfall for the milestone.

---

## AMR-07 gate reuse

| Option | Description | Selected |
|--------|-------------|----------|
| Same hide-when-readonly-or-unloaded gate as Build a link | AMR-07 already decided in Phase 9 | ✓ |
| Always show Add CTA; fail on write | Bad UX / Pitfall 7 | |
| Separate weaker gate for overlap only | Drift between mutating actions | |

**User's choice:** [auto] Shared mutating-action hide gate (recommended default)
**Notes:** Prefer generalizing `canOfferBuildLink` rather than duplicating.

---

## Test coverage strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Vitest boundary Wave 1, then targeted E2E Wave 2; keep overlap_try_again green | Matches Phases 7–9 | ✓ |
| E2E-only for declare | Weaker Pitfall 4 regression signal in CI unit gate | |
| Skip E2E until Phase 12 | Leaves AMR-08/09 without browser proof | |

**User's choice:** [auto] Vitest then E2E; uncouple overlap_try_again (recommended default)
**Notes:** Full AMR-05 reopen polish remains Phase 12.

---

## Claude's Discretion

- Button density/classes next to Build a link
- Silent vs toast on null append
- Exact Vitest StoredApi stubbing pattern
- E2E feature file placement

## Deferred Ideas

- Phase 12 AMR-05 reopen; AMR-10..13 / SEED-001 as already parked
