# Accept a wrong spelling answer as an alias

**Identity:** SEED-073#accept-spelling-answer-as-alias
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#accept-spelling-answer-as-alias)
**Source review:** [source-review.md](source-review.md)

## Outcome and boundaries

A learner whose spelling answer was marked wrong, but names no other note,
adds the typed answer to the reviewed note's `aliases` with one click on the
answer result; later spelling recalls of that note accept it.

Included: the action on the spelling answer result, its done state (also
when the result is shown again), and the story's restrictions: no action for
an answer that names another note, a correct or blank answer, a read-only
notebook, or an answer that cannot be a valid alias.

Excluded, as the story defers them: changing or excluding the grade of the
answer just given, multiple-choice answers, merging notes, a several-answer
spelling format, a poor-fit warning, removing an alias from the result.

## Architecture and reuse

PFE: every part exists; the story adds one action to the answer result.

- `appendAliasToNoteContent` (`frontend/src/utils/appendAliasToNoteContent.ts`,
  used by the Wikidata flows) adds an item to frontmatter `aliases`, creating
  the frontmatter when absent, and returns `null` when the content would not
  change or `aliases` is not a list.
- `noteStore.updateTextField(noteId, "edit content", content)` is the write
  the answer result already uses for "Add as overlapped note"
  (`AccidentalMatchResolveDialog.vue`); the reviewed note's realm, including
  `notebookRealm.readonly`, comes from `noteStore.getNoteRealmRefAndLoadWhenNeeded`
  there too.
- Valid alias text is already one rule in
  `frontend/src/utils/authoredAliasesValidation.ts` (`isValidPlainAliasText`,
  module-private), mirroring `FrontmatterAliases` on the backend. Slice 2
  exports it rather than adding a second recognizer.
- `Note.matchAnswer` (backend) accepts the title or any valid frontmatter
  alias; no backend change is expected.
- `AnsweredSpellingQuestion.vue` is shown for every graded spelling answer;
  an `OVERLAP` outcome never reaches it (`useRecallAnswerHandling` turns it
  into retry feedback), and `ACCIDENTAL_MATCH` already shows its own resolve
  action. The new action belongs to the plain wrong outcome only.

Accepted decisions: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
keeps note Markdown authoritative and aliases in frontmatter; writing the
alias into content follows it. [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
is untouched: the grade stays (owner decision 2026-11-17), so no RecallLog or
schedule change. No conflict found. No North Star topic governs this work.

No expected edit touches a file over the 250-line check: largest are
`e2e_test/start/pageObjects/recallPage.ts` (239, not expected to change),
`AnsweredSpellingQuestionAccidentalMatch.spec.ts` (216, not expected to
change), `e2e_test/step_definitions/recall.ts` (213),
`answeredSpellingQuestionTestSupport.ts` (117), `AnsweredQuestionPage.ts`
(101), `AnsweredSpellingQuestion.vue` (98). Put the action in its own small
component beside `AccidentalMatchResolveRow` if the result component would
otherwise grow past one concept.

## Current decisions

- The grade of the answer just given stays; nothing calls the backend
  besides the note content update.
- Offer rule (one rule, no special cases): the answer result offers the
  action when the outcome is a plain wrong answer, the learner may edit the
  reviewed note's notebook, the trimmed answer is valid alias text, and
  `appendAliasToNoteContent` would change the content. When the note's
  aliases already hold the answer, the result shows the done state instead.
- The done text names the answer, for example "`つてを頼る` is now an alias".
  The exact wording is the implementer's within that meaning.

## Decisive premises and observations

Preparation checkout: this workspace, revision
`e3127961afaca4666faed1b7b1e70a8761dd2024`, 2026-11-17.

| Premise / consuming operation | Observation, result and limits | Owner |
| --- | --- | --- |
| `appendAliasToNoteContent` writes the typed answer as a plain `aliases` item | Scratch vitest (reverted) via `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm vitest run` in `frontend/`: content without frontmatter + `つてを頼る` → `---\naliases:\n  - つてを頼る\n---\n…`; an existing list + `draw a line` → appended item; `aliases: x` (scalar) → `null`. | Slice 1 |
| The backend accepts an answer that equals a frontmatter alias | `RecallPromptAnswerSpellingControllerTest.answerOneOfTheFrontmatterAliases` asserts it; the E2E `overlap_try_again.feature` example `hue`/`color` answers an alias as correct. Read at this revision, not re-run; slice 1's E2E observes the round trip from the web write. | Slice 1 |
| A wrong answer is graded Again and the note is due again on a later day of the same E2E scenario | Not observed. If day 3 is not due, the scenario uses a later day; this changes only a step's day, not the approach or sizing. | Slice 1 |
| The answer result component tests run in this worktree | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm vitest run tests/components/recall/AnsweredSpellingQuestion` in `frontend/`: 5 files, 15 tests passed in 12 s. | Slices 1–2 |
| The spelling recall E2E journey runs in this worktree | `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/recall_quiz_spelling_question.feature` at this revision exited 0: 3 passing in 15 s, including "Spelling quiz reveals the reviewed note after a wrong answer". | Slice 1 |

## Outside-in proof ownership

| Promise / source example | Owning slice | Observable proof |
| --- | --- | --- |
| Wrong answer naming no note → one-click add → done state → next recall correct (example 1) | 1 | New scenario in `e2e_test/features/recall/recall_quiz_spelling_question.feature`: note `sedition`; answer `sedation` (wrong, names no note) → add as alias → result says it is now an alias → a later recall answered `sedation` is correct. |
| The note shown under the result lists the new alias; the done state holds when the result is shown again | 1 | Component test on `AnsweredSpellingQuestion`: the click sends content with the alias through `updateNoteContent`; a reviewed note whose aliases already hold the answer shows the done state and no action. |
| Answer naming another note keeps its flow, no alias action (example 2) | 2 | Component test: `ACCIDENTAL_MATCH` result shows the resolve action and no alias action. |
| Overlap retry (example 3) | — | Unchanged path: the result component is never shown; `overlap_try_again.feature` stays green in CI. |
| Correct or blank answer → no action (example 4) | 2 | Component tests. |
| Read-only notebook → no action (example 5) | 2 | Component test with `makeMe.aNoteRealm.readonly()`. |
| Invalid alias text → no action (example 6) | 2 | Component test with `draw/a line`; a unit test of the exported alias-text rule if it moves. |
| Grade stays | 1 | No backend change; the scenario's next recall happens on the schedule the wrong answer set. |

## Ordered slices

### 1. Add a wrong spelling answer as an alias from the answer result
Type: Behavior
Status: done
Proof: the new E2E scenario passes with
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/recall_quiz_spelling_question.feature`;
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm vitest run tests/components/recall/AnsweredSpellingQuestion`
in `frontend/` passes with the new tests.

Behavior: a wrong spelling answer that names no other note, on a note the
learner may edit → the learner clicks "Add `<answer>` as an alias" → the
note's `aliases` holds the answer, the result shows it is now an alias, and
a later spelling recall answered the same way is correct.

Write the E2E scenario first (step and page-object additions in
`recall.ts` / `AnsweredQuestionPage.ts`), then the action using
`appendAliasToNoteContent` and `noteStore.updateTextField`, with the done
state derived from the reviewed note's current content.

Interim behavior: after this slice the action may also appear on other
wrong results (accidental match, read-only, invalid text); slice 2 replaces
that with the full offer rule.

Accepted (2026-11-17): `AddSpellingAnswerAsAlias.vue` beside the result's
alert; E2E scenario "Accept a wrong spelling answer as an alias" and
`AnsweredSpellingQuestionAddAnswerAsAlias.spec.ts` passed (recall component
suite 189 tests; after refactor 315 tests incl. notes/composables, vue-tsc
clean). Refactor moved `appendAliasToNoteContent` into its own module.
Learning: the wrong answer on day 2 makes the note due on day 3. Learning
for slice 2: the done state today means `appendAliasToNoteContent` returned
`null`, which also happens for a scalar `aliases` or unparsable frontmatter
(G2); the offer rule must tell "already an alias" from "cannot append".

### 2. Offer the alias only where it can apply
Type: Behavior
Status: planned
Proof: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm vitest run tests/components/recall/AnsweredSpellingQuestion`
(and the alias-validation unit test if touched) in `frontend/` passes with
the new restriction tests; slice 1's scenario stays green.

Behavior: an accidental-match, correct, or blank answer, a read-only
notebook, or an answer with `/`, `\`, `:`, `#`, `^`, `[[`, `]]` or a line
break → the answer result shows no alias action (examples 2, 4, 5, 6); the
accidental-match resolve action is unchanged.

Export the existing valid-alias-text rule from
`authoredAliasesValidation.ts` and use it in the offer rule.

## Refinement and concerns

Refinement was not needed: two slices with separate proof loops, one offer
rule, and no Structure. This review identified no remaining slice-specific
concern.

## Story obligations

### G1. Alias action offered on every wrong result (first reported in slice 1)
Reported: slice 1 — "the action also shows on accidental-match, read-only, blank and invalid-text answers. Slice 2 owns those cases."
Story clause: "A wrong answer gets one offer, never both"
Disposition: receiving slice 2

### G2. Done state shown when the alias cannot be appended (first reported in slice 1)
Reported: slice 1 — "the done state means `appendAliasToNoteContent` returned null. That also happens when the note's `aliases` is a single value rather than a list, or the frontmatter cannot be parsed, so those notes wrongly show \"is now an alias\"."
Story clause: "Afterwards the result shows that the answer is now an alias"
Disposition: receiving slice 2
