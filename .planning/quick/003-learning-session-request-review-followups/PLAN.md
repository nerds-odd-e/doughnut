# Learning session request — post-implementation review follow-ups

**Status:** in progress (slice 1 done)

## Origin

Retrospective inspection of the full "graph context merged across session
items" implementation, commits `fd7098034e..845e580ffe` (the original feature
plus its formatting-review fixups). Findings below are confirmed by reading
the diffs and, for the doc mismatch, by running the actual test output —
not raised speculatively.

## Findings

1. **[Docs, confirmed] ADR 0005 and the focus-context design doc example are
   missing a blank line before `<focus_note>`.** Ran
   `LearningSessionRequestTests.returnsRequestMarkdownFromDueTrackersWithoutWritingRecallLogs`
   with a temporary debug print: real output is
   `- Tutoring status: not yet tutored\n\n<focus_note>\n...` (blank line),
   because `FocusContextMarkdownRenderer.renderFocusNote()` always leads with
   `FocusContextConstants.FOCUS_NOTE_SECTION_START` (`"\n<focus_note>\n"`),
   and `LearningSessionRequestMarkdownBuilder.appendSessionItem()` now calls
   `renderFocusNote()` directly (no longer wrapped in `<focus_context>` +
   "Max depth" line, which used to absorb that leading newline). The doc's
   hand-written example (`docs/adrs/0005-commissioned-learning-session-protocol.md`,
   both "Hola" and "Gracias" sections) shows no blank line there — stale
   relative to the code the same commit (`0134688da8`) was aligning it with.
   Code behavior matches the rest of the document's convention (every
   top-level-ish tag gets a leading blank line), so the doc is what is wrong,
   not the code.

2. **[Structure, confirmed] `MergedRelatedNotes` has no direct unit test.**
   It is a pure, dependency-free algorithm (first-seen-wins dedup by
   `(notebook, title)`, plus `exclude()` pre-claiming) — exactly the
   "independent, intentional domain-stable contract" `unit-testing.mdc` calls
   out for direct coverage. Today it is only exercised indirectly through two
   DB-backed tests in `LearningSessionRequestRelatedNotesTests`. The sibling
   `renderRelatedNotes()` got this same direct-coverage treatment in the
   formatting-review plan (slice 3); `MergedRelatedNotes` was missed.

## Open question (Jidoka before any slice) — not a slice by itself

3. **Per-session-item token budget scaling.** `appendSessionItems()` calls
   `focusContextRetrievalService.retrieve()` once per tracker, each with its
   own full `FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET` (2000 tokens). A
   Request with N session items can therefore carry up to N× that budget in
   merged focus + related content — ADR 0005 already documents this
   ("retrieved... with the normal per-note Focus Context budget"), so it is a
   known, stated design choice, not a silent bug. Flagging only because it
   does not obviously bound total Request size as commissioned notebooks
   grow. No slice proposed unless the developer wants a combined-budget cap
   across session items — that would need its own discussion of what the cap
   should be, not a mechanical fix.

## Slices

### 1. ADR and design doc examples match actual Request output — Docs

Status: done

- ADR 0005 Hola/Gracias examples now have the blank line before `<focus_note>`
  (matches `FOCUS_NOTE_SECTION_START`).
- Design doc already had the blank line inside `<focus_context>`; no edit.
- Learning: none for remaining slices.

### 2. `MergedRelatedNotes` dedup and exclude contract has direct coverage — Structure

- New `backend/src/test/java/com/odde/doughnut/services/focusContext/MergedRelatedNotesTest.java`.
- Cases: `addAll` dedups by `(notebook, title)` first-seen-wins across
  multiple calls; `exclude()` prevents a matching note from ever being added
  by a later `addAll`; `asList()` returns a snapshot independent of further
  mutation (call `addAll` again after taking the list, confirm the earlier
  list is unchanged).
- No production code change expected — this is pinning existing behavior.
  If a case reveals an actual defect, fix it in this same slice (small,
  contained) rather than opening a new one.
- Verify: new unit test class passes; no other tests affected.

## Explicitly not planned

- The `render()` vs `renderRelatedNotes()` near-duplicate loop
  (`"\n---\n"` + `appendRetrievedNote`) in `FocusContextMarkdownRenderer` is
  pre-existing (relocated, not introduced, by the `fd7098034e` split) and is
  three lines — not worth a slice.
- No dead code found in the reviewed range; the `RetrievalConfig.focusNoteOnly()`
  removal (slice 2 of the original feature plan) was clean — method and its
  two tests removed together, no leftover callers.
