# Show the smallest change in a note correction

**Identity:** SEED-073#source-and-smallest-change-in-corrections
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#source-and-smallest-change-in-corrections)

## Goal and scope

A note author deciding on a pending content correction sees what it would
change in the current note: removed and added text marked, properties
included, and inside a changed line only the changed characters. The
change follows the editor and **Load current content**.

Excluded, as the story says: title corrections; corrections already
decided and readers who cannot accept; accepting each change separately;
making the assistant propose smaller edits; the quoted source, which is
[its own story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#assistant-quoted-source-in-corrections).

## Existing solutions and design

- The review, `frontend/src/components/conversations/ConversationNoteCorrectionReview.vue`
  (201 lines; the file-size check is 250), already holds both texts: the
  loaded current content and the editor's wording. The change is worked
  out in the browser. No API, stored data or backend change. Acceptance is
  untouched, so [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  holds as today.
- The product compares two texts line by line in
  `frontend/src/components/commons/diffViewPairedRows.ts` (199 lines),
  shown side by side by `DiffView.vue` and `DiffPane.vue` in the refine-note
  preview and the undo confirmation. Reuse that line comparison. Do not
  reuse the side-by-side view for corrections: its cells do not wrap
  (`white-space: pre` in `DiffPane.vue`), and a note paragraph is often one
  long line (about 600 characters in the prisoner's dilemma note), so the
  change would sit off screen.
- The correction shows the change as one column of wrapped text: unchanged
  text plain, removed text in `<del>`, added text in `<ins>`. Put it in its
  own small component so the review stays under the file-size check, and
  keep the comparison rule in one place. If the comparison module would
  pass 250 lines, give the within-line comparison its own module beside it.
- `buildPairedDiffRows` and what `DiffView` renders stay as they are, so the
  refine-note preview and the undo confirmation see no change. If
  execution changes either, their consumers' tests join that slice's proof.
- The within-line comparison works on characters, not words, so text
  without spaces is handled by the same rule. It runs only on lines the
  line comparison found changed.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| A note's content, as the review loads it, carries its properties as text lines before the body | slices 1 and 2: a property change is a changed line | 2026-11-18, read-only `GET /api/notes/7323` on production (Solid OIDC): `content` starts `---\nuses: '[[WebID]]'\ntype: Note\n…---\n\nSolid OIDC is…` | settled |
| The body change of a real correction sits inside one line | slice 2 is needed | 2026-11-18, `git show 5ed4e72e` and `git show fa5efa4d` in read-only clones of the two production notebooks: each body is one line; the property changes are separate lines | settled |
| The review can be driven through `ConversationInner` with a pending content correction, edited wording and **Load current content** | proof of slices 1 and 2 | 2026-11-18 at cda3cf0370 in this worktree: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation/ConversationInner.corrections.spec.ts tests/components/conversation/ConversationInner.staleCorrections.spec.ts` → 2 files, 8 tests passed | settled |
| The E2E scenario "Reviewing and accepting a note content correction" runs in this worktree | slice 1's E2E step | 2026-11-18 at cda3cf0370 in this worktree: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_content/note_content_completion.feature` → 2 passing, 0 failing | settled |

## Outside-in proof ownership

Component proof enters through `ConversationInner` with the helpers in
`frontend/tests/components/conversation/conversationCorrectionTestSupport.ts`
and observes the rendered review: the text inside `<del>` and `<ins>`
elements and the unmarked text. Each new test must fail before its slice's
change. Frontend proof also needs both typechecks from the `frontend`
skill (`vue-tsc --noEmit` and `tsc --noEmit`).

| Story example | Slice | Observation |
| --- | --- | --- |
| 1. Two property lines and a body sentence change; other properties unmarked | 1 | component test: the old property lines are in `<del>`, the new ones in `<ins>`; `type`, `url`, `a part of` are in neither |
| 3. Typing a changed line back removes its mark | 1 | component test: after setting the editor value, that line is in neither `<del>` nor `<ins>` |
| 5. After **Load current content** the change is against the note as loaded | 1 | component test in the stale-review flow: marks are computed from the refreshed content |
| 6. Wording equal to the current note says there is no change | 1 | component test: the review shows the no-change message and no `<del>` or `<ins>` |
| A pending content correction in the running product marks its change | 1 | E2E, existing scenario in `e2e_test/features/ai_generated_content/note_content_completion.feature`: for "It is a" → "It is a vigorous city." the review marks the removed and added text |
| 2. One sentence replaced inside a one-line paragraph | 2 | component test: `<del>` and `<ins>` hold only the changed words; the paragraph's other sentences are in neither |
| 4. Japanese characters added in the middle of a line | 2 | component test: `<ins>` holds exactly 「最後に「か」がつくと」 |

The reader's guide `docs/recall-help.md#reviewed-note-corrections` gains
the change display in slice 1.

## Ordered slices

### 1. A pending content correction marks its changed lines
Type: Behavior
Status: planned
Proof: the component tests for examples 1, 3, 5 and 6 and the E2E step
above; both typechecks; the existing correction specs in
`frontend/tests/components/conversation/` stay green.

Behavior: a pending content correction the author may accept, with the
current note loaded → the review shows the change between the current
content and the editor's wording as one column: unchanged lines plain,
removed lines in `<del>`, added lines in `<ins>` → editing the wording or
choosing **Load current content** updates the marks; equal texts show a
no-change message. The change comes before the editor. The full current
content, the original proposal and the editor stay. Title corrections and
decided corrections render as today.

Interim behavior: a changed line is marked whole, so a one-line paragraph
is marked whole. Slice 2 replaces this.

### 2. Only the changed characters inside a changed line are marked
Type: Behavior
Status: planned
Proof: the component tests for examples 2 and 4; slice 1's tests stay
green, with example 1's body sentence now marked only where it differs;
both typechecks.

Behavior: a removed line and the added line that replaces it → the line is
shown once, with only the removed characters in `<del>` and the added
characters in `<ins>`. A line that was only added or only removed stays
marked whole.

## Current decisions

- Split by the owner on 2026-11-18: this plan shows the change only.
- The comparison is by line, then by character inside changed lines; one
  rule for every language.
- `<del>` and `<ins>` carry the marks, so tests and assistive technology
  read them without relying on color.
- The side-by-side `DiffView` and its two consumers are left as they are.

## Concerns

Slice-plan refinement was not needed: two slices, each one outcome with
its own proof. No slice-specific concern remains.
