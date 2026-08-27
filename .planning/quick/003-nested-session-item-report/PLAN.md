# Nested session_item Learning Session Report

**Status:** planned (not started)

## Goal

Doughnut records Feedback from the Report shape in
`docs/commissioned-learning-session-protocol.md`: nested `<session_item>`
elements whose first line is `{title}: {1–4}` and whose following lines are
descriptive text. The Request’s `<how_to_report>` shows that shape, wrapped in
a fenced code block. A `<session_item_feedback>` block that is a sequence of
those `{title}: {1–4}` lines (inner tags gone on copy) still records Grade and
text.

Protocol doc is already the target. This plan is parser, Request markdown, and
tests.

## Decisions

| Decision | Choice |
|---|---|
| Item boundary | `<session_item>…</session_item>` |
| Title and Grade | First structured line `{title}: {1–4}` |
| Descriptive text | Lines after that until `</session_item>` |
| Copy without inner tags | Same `{title}: {1–4}` lines, following lines until the next matching title line |
| Fence | `<how_to_report>` example wraps `<session_item_feedback>` in a fenced code block |
| Grade-only Reports | `<session_item_grades>` / `<session_item_scores>` / bare `{title}: {1–4}` stay as they are |

## Slices

### 1. Nested session_item Report records Feedback — Behavior

Learner pastes a Report with `<session_item>` elements; Doughnut records Grade
and descriptive text on the commissioned tracker.

- E2E: `commissioned_learning_session.feature` “Recording a session item
  feedback report…” uses the nested-tag Report; tracker still shows the tutor
  text. Table-built reports in step defs use the same shape
  (`e2e_test/step_definitions/learning_session.ts`).
- Parser (`SessionItemFeedbackBlockParser` via `LearningSessionReportParser`):
  each `<session_item>` is one item; first `{title}: {1–4}` is Grade; rest is
  text. Unit tests at that boundary: two items; item with no `Title: N` line
  reported; Grade outside 1–4 reported; unknown title reported; duplicate title
  reported; unclosed item runs to end of block.
- Helpers that emit the Feedback block
  (`LearningSessionControllerTestBase`, request-builder tests that embed a
  sample Report) use nested tags so they stay green with the parser.

Request `<how_to_report>` still shows `###` / `Grade:` until slice 2. Run slice
2 next in the same sitting.

### 2. Request shows nested session_item and a fenced example — Behavior

Opening a potential session, the Request’s `<how_to_report>` contains a worked
Report with `<session_item>` and `{title}: {1–4}`, inside a fenced code block.

- `LearningSessionRequestMarkdownBuilder.appendHowToReport` / example items.
- E2E: existing “instruct the tutor…” assertion
  (`expectLearningSessionRequestInstructsDescriptiveFeedback`) looks for
  `<session_item>` and a fenced `<session_item_feedback>` example (the `Grade:
  4` line assertion becomes `Hola: 4` or the first session item’s title).
- Unit: `LearningSessionRequestTests` example block matches the protocol
  sample.

### 3. Title-grade sequence in the Feedback block records text — Behavior

Learner pastes `<session_item_feedback>` whose body is `{title}: {1–4}` lines
and following prose (the copy a chat GUI leaves when inner tags are gone).
Doughnut records Grade and that prose.

- E2E: one scenario on `commissioned_learning_session.feature` — Hola `4` plus
  a sentence of text, no inner `<session_item>` — then the tracker shows that
  sentence.
- Parser: when the Feedback block has no `<session_item>` elements, each
  `{title}: {1–4}` whose title is a Session Item in this notebook starts an
  item; following lines until the next such line are descriptive text. Unit:
  two items with prose; a non-title `Something: 1` line stays in the previous
  item’s text.

## Notes

- Grade-only `<session_item_grades>` scenarios stay as they are.
- Request Session Items still use `### {title}` under `<session_items>`; only
  the Report / `<how_to_report>` example changes.
- Capability names only in product files; slice numbers stay in this PLAN.
