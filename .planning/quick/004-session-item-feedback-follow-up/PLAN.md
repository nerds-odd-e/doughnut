# Session item Feedback report follow-up

**Status:** in progress (slice 1 done)

## Goal

Fix the tagless Feedback path so an out-of-range Grade is reported to the
learner, and drop test assertions that re-pin a shape already covered at
another stable boundary.

Inspected product commits: nested `<session_item>` parse, fenced
`<how_to_report>` example, tagless title-grade sequence.

## Decisions

| Decision | Choice |
|---|---|
| Out-of-range tagless Grade | Start an item on `{title}: {digits}` when `title` is a Session Item; `acceptItem` already rejects range. Do not treat `Hola: 5` as prose or as a silent no-op. |
| Error-path test | Parser unit at `LearningSessionReportParser` (title-grade sequence file). No new E2E — invalid input. Nested `Hola: 5` already covers the reject reason. |
| Redundant tests | Keep E2E recording scenarios (tracker text) and the nested two-item parser test (canonical Feedback shape). Slim only re-assertions of that same shape. |
| Bare report with no `<session_item_feedback>` | Unchanged. Grade-only still rejects following prose. See Discoveries. |

## Discoveries (not in this plan)

- **Request example is pinned twice.**
  `LearningSessionRequestTests.requestMarkdownInstructsDescriptiveFeedback`
  has the full fenced sample. E2E
  `expectLearningSessionRequestInstructsDescriptiveFeedback` repeats
  `<session_item>`, fence, and `Hola: 4`.
- **`prefersFeedbackBlockOverLegacyGradesBlock`** re-asserts the two-item
  Hola/Gracias grades; unique claim is “ignore the grades block” (Hola is 4,
  not 1).
- **Title-grade happy path** re-asserts `Grade.EASY` / `Grade.AGAIN` already
  canonical in nested parse + grade-only tests. Unique claim: two items and
  prose assignment without inner tags.
- **Not dead / not redundant:** nested vs tagless E2E (different paste shape,
  tracker is the user path); `recordingWritesTutorFeedbackOnRecallLog` vs E2E
  (HTTP/RecallLog vs tracker page); `gradeOnlyFeedbackItem…` vs
  `blankProse…` (null from missing lines vs stripped whitespace);
  `nonTitleGradeLineStaysInPreviousItemText`.
- **Out of scope (value fork):** if a chat GUI strips *all* tags, including
  `<session_item_feedback>`, following prose is rejected as unparseable on the
  grade-only path. Original plan kept that path as-is. Do not change it here.
- **Out of scope:** locale flake on “hours between last and next recall”
  (`03/06` vs `11/06`); Spanish example prose on non-Spanish notebooks
  (pre-existing); stale `.planning/STATE.md` Operator Next Step pointing at
  deleted plan 003 (GSD state — not this product follow-up).

## Slices

### 1. Tagless out-of-range Grade is reported — Behavior

Status: done

Tagless `{title}: {digits}` starts an item when the title is a Session Item.
Out-of-range Grade is rejected in `acceptItem`; a following valid item still
records.

### 2. Drop overlapping Feedback and Request test assertions — Structure

Same observable product behavior; tests assert only their unique claim.

- E2E `expectLearningSessionRequestInstructsDescriptiveFeedback`: keep the
  fenced `<session_item_feedback>` example (Request UI copy). Drop
  `<session_item>` and `Hola: 4` — the controller test owns that sample.
- `prefersFeedbackBlockOverLegacyGradesBlock`: assert Hola is 4 (not the
  grades-block 1), not the full two-item grade mapping.
- Title-grade two-item test: assert titles and descriptive text; do not
  re-assert EASY/AGAIN.

Unlocks nothing further; stop-safe if skipped after slice 1.

## Notes

- Capability names only in product files; slice numbers stay in this PLAN.
