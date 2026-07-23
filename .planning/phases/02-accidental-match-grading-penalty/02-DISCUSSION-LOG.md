# Phase 2: Accidental-match grading & penalty - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 2-Accidental-match grading & penalty
**Mode:** `--auto` (no interactive prompts; Claude auto-selected all gray areas and chose the recommended option for each)
**Areas discussed:** Accidental-match lookup mechanism & home; SRS penalty magnitude & `correct`/threshold semantics; Matched-note selection (single vs all); Skip-when-matches-reviewed-note & shared-title/alias edge case

---

## Accidental-match lookup mechanism & home

| Option | Description | Selected |
|--------|-------------|----------|
| (A) Extend `WikiLinkResolver` with a wider readable-notebook lookup; inject into `MemoryTrackerService` | New `findAccidentalMatch(answer, reviewedNote, viewer)` reusing `noteCandidates`/`aliasTargetCandidates` + `userMayReadNotebook`, with notebook-scoping removed. | ✓ |
| (B) New `NoteRepository`/`NoteAliasIndexRepository` cross-notebook query + Java-side readability filter in `MemoryTrackerService` | Push the wider search into new repo query methods; filter readability in the service. | |
| (C) Inline the search in `MemoryTrackerService.answerSpelling` using existing repos | No new collaborator; search logic lives inline in the grading method. | |

**User's choice:** [auto] (A) — recommended default.
**Notes:** Matches the locked PROJECT.md constraint "Reuse `WikiLinkResolver`" while satisfying "needs a wider lookup than the existing unqualified resolver." The existing `resolveWikiLinkToken` is notebook-scoped and parses `[[wiki-link]]` token syntax, so it is not used as-is — the answer is a bare title/alias and the search must cross notebook boundaries. Reusing the resolver keeps all title/alias match logic cohesive.

---

## SRS penalty magnitude & `correct`/threshold semantics

| Option | Description | Selected |
|--------|-------------|----------|
| (A) `correct=false`, `outcome=ACCIDENTAL_MATCH`, `updateForgettingCurve(-10)` (half of wrong), no 12h override; threshold counts (unchanged) | Wrong = `ForgettingCurve.failed()` = -20 + 12h override; accidental = -10 + no 12h. Threshold behavior unchanged (out-of-scope). | ✓ |
| (B) `correct=false`, `outcome=ACCIDENTAL_MATCH`, `updateForgettingCurve(-5)` (quarter of wrong), no 12h override; threshold unchanged | Even lighter penalty. | |
| (C) `correct=false`, full wrong penalty (-20 + 12h) | Not lighter — rejected by AM-02. | |

**User's choice:** [auto] (A) — recommended default.
**Notes:** -10 = `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` (10) = exactly half of the -20 wrong penalty, reusing the existing SRS constant. "Lighter than wrong" = half the index penalty AND dropping the 12h `nextRecallAt` override. `correct` stays the sole SRS-credit signal (Phase 1 lock), so `correct=false`. The lighter penalty must NOT go through `MemoryTracker.recallFailed` (which applies -20 + 12h); the exact seam (increment `recallCount`, apply -10, still run `hasExceededWrongAnswerThreshold`) is a planning detail. The accidental match still counts toward the 5-wrong-in-14-days re-assimilation threshold because `correct=false` and the out-of-scope item forbids changing the threshold.

---

## Matched-note selection (single vs all)

| Option | Description | Selected |
|--------|-------------|----------|
| (A) First readable match (lowest id, excluding reviewed note) → `matchedNoteId`; leave `matchedNotes` list empty for Phase 3 | Phase 2 detects one accidental match; Phase 3 reveals all. | ✓ |
| (B) Find ALL readable matches; set `matchedNoteId` to first AND populate `matchedNotes: List<NoteTopology>` now | Front-runs Phase 3's plural reveal. | |
| (C) Find all but only set `matchedNoteId` (discard the rest until Phase 3) | Wastes the extra search work Phase 3 will redo. | |

**User's choice:** [auto] (A) — recommended default.
**Notes:** Stop-safe / one-observable-behavior-per-phase. Phase 2's success criteria only require *detecting* an accidental match (singular `matchedNoteId`); Phase 3's explicit scope is "all matched notes are surfaced." `AnsweredQuestion.from` embeds the full `Answer`, so `matchedNoteId`/`outcome` surface with no DTO edit; `AnsweredQuestion.matchedNotes`/`overlap` stay null for Phases 3/6.

---

## Skip-when-matches-reviewed-note & shared-title/alias edge case

| Option | Description | Selected |
|--------|-------------|----------|
| (A) `correct=true` → skip accidental-match search entirely, even if another readable note shares the title/alias | Shared-title situation is the overlap case (declared in Phase 5, "try again" in Phase 6), not auto-detected. | ✓ |
| (B) `correct=true` but another note also matches → still flag as accidental match | Conflates accidental match with overlap; front-runs Phase 6. | |

**User's choice:** [auto] (A) — recommended default.
**Notes:** Directly satisfies Phase 2 success criterion #3 and the locked decision "Overlap is declared, not auto-detected."

---

## Claude's Discretion

- Exact name/signature of the new `WikiLinkResolver` wider-lookup method.
- The precise code seam for the lighter penalty while preserving the threshold check (research/planner to confirm against `markAsRecalled` / `recallFailed`).
- Whether the wider lookup is one combined query or a title-then-alias fallback across readable notebooks (follow existing `noteCandidates` ordering).
- Test placement and naming (backend unit/integration only; no E2E in Phase 2 — no UI).

## Deferred Ideas

- Reveal both notes in the UI → Phase 3 (AM-03).
- Surface ALL matched notes (plural) → Phase 3.
- Offer add-link UI with matched note pre-selected → Phase 4 (AM-04).
- Alias-as-wiki-link overlap declaration → Phase 5 (OVL-02, OVL-03); alias-blast-radius spike lives there.
- Overlap "try again, no credit" → Phase 6 (OVL-01).
- MCQ / fuzzy / cross-notebook qualified typing → v2 (out of scope).
