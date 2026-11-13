# Current-tracker recent-question export proof

Slice 3 uses one `McqRepository.findRecentForNote` query through prompt/current
tracker membership. `EXISTS` preserves one result per question; a nullable key
selects whole-note history, while binary key/value matching retains property
focus. Creation time/ID ordering and the ten-question limit remain unchanged.
The controller's endpoint, request fields and response shape are unchanged.

`NoteExportControllerMovedQuestionsTest` invokes real property reification,
exports the resulting current note and observes the numbered question plus its
recorded selected answer. The original note stays stored in this compatible
stage; both move-plus-old-note-deletion journeys remain owned by slice 9.

Retained proof lives in cohesive homes under 250 lines:

- `NoteExportControllerRecentQuestionsTest`: canonical appendix, chronology/ID
  ties, note membership, contested inclusion and ordinary/default/HTTP flags.
  Every question-bearing case has a prompt and tracker, including false/default
  flag cases. This responsibility moved from the original context test home.
- `NoteExportControllerLearnerSelectionsTests`: own selection, latest answer
  time and ID tie. Same-tracker repeated questions remain valid.
- `NoteExportControllerQuestionPrivacyTest` and `PropertyPrivacyTests`:
  separate questions per learner tracker, own selection, other-answer privacy,
  guest behavior and active access refusal. Privacy/guest/refusal scenarios
  moved from the latest-selection home without weakening their observations.
- `PropertyHistoryTests` and existing `PropertyTests`: distinct newest ten,
  exact focus matching, raw values, duplicate occurrence and shared history
  through actual HTTP/controller exports.

The source-review table names the commitment-specific observations. Root
inspected the SQL, setup and assertions before the expensive command launched.
Literal proof command:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice3-backend-test.log 2>&1`.
Exit 0: migration/compile 50 seconds; tests `BUILD SUCCESSFUL in 6m 29s`.
All 639 current reports contain 3,202 tests, zero failures/errors, two skipped;
profile `test`, isolated database
`doughnut_wt_6c52c877a9c84ab186ec2c44fc117692_test`. Inputs stayed fixed through
the terminal result. Editing/inspection took about seven minutes; proof wait
was separate under the plan's indivisible full-suite exception.

Pre-launch host decision: this Mac has 16 logical CPUs/48 GiB RAM, load
40.70/28.01/20.39 and memory pressure reporting 38% free. No active Java/Cypress
worker or competing owned heavy proof was found. One warm full suite was
selected, with reconsideration on failure/contention. When it exceeded the
estimated window, the owner performed a bounded liveness check; it completed
normally without a rerun or cancellation.

Consumer trace: `/tmp/donut-slice3-consumer-trace.md`, all 125 consumers
dispositioned. The reached highlighted-property browser journey's selection,
key and own-choice purposes are observed by the real HTTP/controller cases
above. Cypress was not run. Root also inspected the export form: it forwards
the unchanged request fields and treats returned Markdown as preview/copy/
download text; it does not derive ownership. No generator is triggered.

Separate retirement reading: the consumer trace and a one-time `rg` reading
for `findTop10ByNote_IdOrderByCreatedAtDescIdDesc|findRecentForProperty` under
`backend/src` found no remaining caller or declaration. Bank-only promptless
inclusion and the unlinked fixture/assertion were deleted outright; no absence
assertion replaced them. Dated source/evidence remains as recorded.

Source freshness was unchanged at `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`;
the story and plan name no sibling coordination dependency. Independent
refactoring returned `none — already clean` and `## REFACTOR COMPLETE` with no
edits: query, controller forwarding and test homes are cohesive. Accepted code,
setup and assertions remained unchanged, so the full-suite proof was reused.
All changed files/records are under 250 lines and the whitespace check passed.
