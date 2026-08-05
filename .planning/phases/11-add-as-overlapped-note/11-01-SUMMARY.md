---
phase: 11-add-as-overlapped-note
plan: 01
subsystem: frontend
tags: [vitest, accidental-match, overlap, wiki-link, resolve-dialog]

requires:
  - phase: 10-overlap-alias-append-util
    provides: appendOverlapWikiLinkToNoteContent util
  - phase: 09-build-a-link-from-resolve-dialog
    provides: Resolve dialog host + AMR-07 Build a link gate pattern
provides:
  - Shared canOfferMutatingAction gate for Build a link + Add as overlapped note
  - Per-row Add as overlapped note CTA (add-as-overlapped-note-{id})
  - Host declare path: appendOverlapWikiLinkToNoteContent → conditional updateTextField; stay on list
  - Vitest: declare + shared AMR-07 gates + no try-again / null-append
affects:
  - Phase 11 Plan 02 E2E Add as overlapped

actuals:
  tokens: 2800
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Shared mutating-action gate (canOfferMutatingAction) feeding both peer CTAs via canMutate"
    - "Declare stays on list — no step swap / closeDialogThen / retry emit"

key-files:
  created:
    - frontend/tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts
  modified:
    - frontend/src/components/recall/AccidentalMatchResolveDialog.vue
    - frontend/src/components/recall/AccidentalMatchResolveRow.vue
    - frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts

key-decisions:
  - "One canMutate prop from canOfferMutatingAction — both CTAs cannot drift (D-09)"
  - "Split Add as overlapped Vitest into capability-named sibling under 250-line rule"
  - "Null-append fixture builds token via buildWikiLinkText matching real notebook IDs"

patterns-established:
  - "Pattern: content-only declare — assert absent overlap-try-again* and undefined retry"
  - "Pattern: peer CTA row flex flex-wrap gap-2 — Build a link then Add as overlapped note"

requirements-completed: [AMR-08, AMR-09]

coverage:
  - id: D1
    description: "Writable+seeded Resolve shows Add as overlapped note; click updates reviewed content with [[ wiki-link; stays on list; ACCIDENTAL_MATCH chrome; no try-again/retry"
    requirement: AMR-08
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts#adds as overlapped note via wiki-link content update without try-again"
        status: pass
    human_judgment: false
  - id: D2
    description: "Shared AMR-07 gate omits both mutating CTAs when readonly or realms unloaded"
    requirement: AMR-08
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#omits mutating CTAs when reviewed notebook is readonly"
        status: pass
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#omits mutating CTAs when note realms are not loaded"
        status: pass
    human_judgment: false
  - id: D3
    description: "Null-append (duplicate wiki-link) skips updateTextField; list stays"
    requirement: AMR-08
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts#does not update content when overlap wiki-link is already present"
        status: pass
    human_judgment: false
  - id: D4
    description: "After declare: no overlap-try-again chrome; no retry emit (AMR-09)"
    requirement: AMR-09
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts#adds as overlapped note via wiki-link content update without try-again"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-08-05
status: complete
---

# Phase 11 Plan 01: Add as overlapped note Vitest Summary

**Per-row Add as overlapped note declares a wiki-link overlap on the reviewed note via `appendOverlapWikiLinkToNoteContent` → `updateTextField`, stays on the list, and never shows try-again or emits retry.**

## Performance

- **Duration:** ~8 min
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Shared `canOfferMutatingAction` gate feeds both **Build a link** and **Add as overlapped note** via one `canMutate` prop
- Host `addAsOverlappedNote` composes Phase 10 util then conditionally persists; stays on list step (no retry)
- Vitest at AnsweredSpellingQuestion boundary: writable declare + AMR-07 shared gates + null-append no-op + AMR-09 negatives
- Split declare/null-append into capability-named `AnsweredSpellingQuestionAddAsOverlapped.spec.ts` (250-line discipline)

## Task Commits

1. **Tasks 1–2 + wrap-up** — (this commit) feat(11-01)

## Files Created/Modified

| File | Change |
|------|--------|
| `AccidentalMatchResolveDialog.vue` | Shared gate; `addAsOverlappedNote` handler |
| `AccidentalMatchResolveRow.vue` | Peer CTA row; emit `addAsOverlapped` |
| `AnsweredSpellingQuestionAccidentalMatch.spec.ts` | Shared mutating-CTA gate asserts |
| `AnsweredSpellingQuestionAddAsOverlapped.spec.ts` | Declare + null-append Vitest |

## Deviations

- Extracted Add as overlapped tests to sibling capability-named spec (file-size >250 after tracer cases); gate deltas stay on AccidentalMatch under shared “omits mutating CTAs” names
- Cleared `updateNoteContent` spy before click asserts (spy history leaked across AccidentalMatch cases)

## Next

Wave 2 / 11-02: targeted E2E Resolve → Add as overlapped; stay without try-again; keep overlap_try_again uncoupled.
