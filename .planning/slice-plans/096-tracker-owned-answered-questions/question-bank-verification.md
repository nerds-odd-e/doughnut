# Ordinary recall and note actions

Slice 6 deletes the note question bank and exclusive controller, service,
AI-refinement, frontend, testability and browser-test dependencies. Shared
recall generation, contest/regeneration, conversations, assimilation and recent
question exports remain. `Mcq` writes a hidden scalar `compatibilityNoteId`
from its generation input; the physical nullable column and FK remain for
the staged rollout. Current attribution already comes from the tracker.

Before delegation, deletion was compared with every later outcome: selected
question persistence (7), tracker lifecycle (8), move/delete retention and FK
release (9), and final schema cleanup (10). These shared paths and temporary
storage remain available, so there is no destructive dependency conflict.

Consumer closure required deleting test-only service request-export wrappers.
The existing request builder now owns the retained request-context proof.
Before editing the oversized 377-line test home, the coordinator recorded its
general context/model/scoped-instruction and property-type instruction seams.
Initially delivered homes were 219/194 lines before refactoring. Root compared all twenty
void methods (including setup) and all 51 assertions against accepted base
`dae402dfcf6a4c64865b5e14a6885dc5832842af`: names, inputs and assertions survive;
only the direct builder call and its explicit null argument differ.
The bank-export comparison itself was deleted as exclusive feature proof.

Bank-only Git MCQ fixtures, stored-note assertions and their support were
deleted while tracker, schedule and conversation observations remain.
The atomic-import test now identifies its generated question by the request's
unique context seed, preserving rollback/retry proof. Pure question fixtures
require no note; prompted fixtures supply compatibility input from the tracker.

The main edit phase took about 5m25s, with bounded subsequent exclusive-helper
cleanup; verification waits are separate. The complete consumer reading found
additional fixture/step dependencies and closed them before final proof.
No permanent absence assertion, checker or historical product note was added.

Native recovery inspected local/remote branch and the actual checkout before
replacement ownership. Both remained at the accepted base, with expected
uncommitted edits and no active proof worker. The interrupted first backend
run ended 130 during orphan closure. A later successful output lost its native
handle in the server restart; replacement ownership re-established current proof.
CI stop/rearm receipts and exact identities are in [execution context](execution-context.md).

## Accepted backend and generator proof

`CURSOR_DEV=true nix develop -c pnpm generateTypeScript` exited 0, log
`/tmp/donut-slice6-api-generation.log` (Gradle 1m51s). OpenAPI, client and
API summary were regenerated from source. The five bank operations and
testability injection/types are deleted; shared question wire shape is intact.
`CURSOR_DEV=true nix develop -c pnpm openapi:lint` exited 0 with no findings
(`/tmp/donut-slice6-openapi-lint.log`). Later test-helper cleanup changes no
generator input. Generated declarations were never hand edited.

Recovery command `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
exited 0 with the actual `:test` task executed, 3m12s. Log:
`/tmp/donut-slice6-backend-recovery.log`. Root parsed all 635 XML suites:
3,188 tests, zero failures/errors, two skips. Current XML timestamps span
04:33–04:36 UTC on 2026-11-13. General request tests pass 9/9 and property
instructions 11/11. Atomic import passes 1/1, moved-question export 1/1,
contest 6/6, regenerate 5/5 and the rename private-association home 2/2.
Their actual setup and retained observations were inspected with the diff.

Cheap runtime/registry proof was re-established with the literal command:

```sh
CURSOR_DEV=true nix develop -c node --test scripts/isolated-cypress-cli-spec.test.mjs scripts/isolated-cypress-openai-mock.test.mjs scripts/isolated-cypress.test.mjs > /tmp/donut-slice6-node-recovery.log 2>&1
```

Terminal 0: 25/25 passed, zero failures/skips/cancellations. Existing cases
exercise admitted specs, active registry/tag reconciliation, private mock
selection, lease ownership and refusal before reset. Source inputs stayed fixed.
Recovery headroom at 12:32 SGT was load 4.92/18.22/25.51, memory 52% free,
with no active owned heavy worker; serial backend → frontend → E2E was retained.

## Frontend proof

`CURSOR_DEV=true nix develop -c pnpm frontend:test` exited 0 headlessly:
409 files, 2,532 tests passed, 118.36s; log
`/tmp/donut-slice6-frontend-recovery.log`. Retained mounted toolbar/menu
observations cover export/trash shortcuts, voice/refine/assimilation controls
and overflow behavior. Existing recall, history, conversation and export
component suites exercise their unchanged generated contracts.

`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vite build --emptyOutDir`
exited 0 (4,117 modules, 27.28s). Root inspected its generated declaration diff:
six lines for the three deleted components. Then both commands exited 0
against those final declarations and the regenerated API types:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

The generated contract comparison found all 731 retained types and 44/45
retained SDK classes byte-identical; the other retained class drops only the
injection operation. This supports exclusions for unrelated shared-client
consumers, together with the mounted and type proof above.

## Browser and compatibility observation

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/ai_generated_recall_questions/question_contest.feature,e2e_test/features/messages/conversation_about_a_note.feature,e2e_test/features/recall/property_memory_tracker.feature,e2e_test/features/recall/recall_quiz_ai_question.feature' > /tmp/donut-slice6-e2e-recovery.log 2>&1
```

Terminal 0: 11/11 scenarios passed, zero failures/pending/skips, 3m20s.
Contest covers three original/replacement-question outcomes; conversation
covers two reply/export/correction journeys; five property cases observe
independent recall, navigation, tracked values and removal; the final ordinary
quiz observes its question and incorrect selected answer. Root inspected those
feature inputs/assertions and the actual four-spec summary. All twelve retained
method bodies in `noteActionsMethods` are byte-identical to the old mixed
helper, supporting reuse for mechanically relocated unchanged action callers.

The wrapper owned its isolated stack/private mock at `http://127.0.0.1:60576`,
profile `e2e`, database `doughnut_e2e_wt_6c52c877a9c84ab186ec2c44fc117692`.
Read-only SQL after the final quiz observed at
`2026-11-13 04:47:21.826947 UTC`, MySQL `8.4.11`: prompt/tracker/question/answer
IDs are all 1; tracker note, scalar write and joined original-reader note IDs
are all 1; asked stem matches, four choices remain and selected index is 1.
`fk_mcq_note` still has its existing CASCADE rule. Protocol/result/provenance:
`/tmp/donut-slice6-compatibility-read.sql`,
`/tmp/donut-slice6-compatibility-result.txt`,
`/tmp/donut-slice6-compatibility-provenance.json`. The latter records the base
revision, working-source/profile/database and four matching source hashes.
This observes current-binary fresh reads plus persisted compatibility input;
an original binary was not launched. Actual rollout compatibility remains
checkpoint A's observation, owned by slice 8.

Before E2E, load was 19.02/22.81/24.28 with memory 55% free and no owned heavy
worker. One serial batch was chosen; required observations passed without retry.

Literal SQL command (terminal 0):

```sh
CURSOR_DEV=true nix develop -c mysql --host=127.0.0.1 --port=3309 --user=root --batch doughnut_e2e_wt_6c52c877a9c84ab186ec2c44fc117692 < /tmp/donut-slice6-compatibility-read.sql > /tmp/donut-slice6-compatibility-result.txt 2>&1
```

## Removal reading

One-time acceptance reading after generation and component build:

```sh
rg --hidden -n 'McqController|McqRefinementControllerTests|NoteAddQuestion|QuestionExportDialog|mcqsTestSupport|isMcqValid|McqsTestData|injectMcq|injectMcqsToNotebook|injectYesNoQuestionsForNumberNotes|injectNumbersNotebookWithQuestions|injectNumberNotes|mcq_management|addQuestionPage|questionListPage|noteConversationAndQuestionMethods|refineAIQuestion|getAiGeneratedRefineQuestion|questionRefineAiTool|buildQuestionGenerationRequest|mcq-creation-journey|Questions for the note|Add Question|Note\.mcqs|Mcq\.note|getMcqs\(' --glob '!**/.git/**' --glob '!**/node_modules/**' . > /tmp/donut-slice6-sweep-reading.txt
```

Its 42 matches were confined to four live source/planning records: the selected
seed, source review, implementation brief and dated preparation evidence.
Product, tests, CLI, MCP, docs, guidance and generated output had no hit for
these retired names/actions/accessors. Inventory comparison classified omitted
tracked entries as deleted files, wrapper symlinks, the CLI submodule and a
binary ZIP fixture. Immutable migrations and dated done evidence retain their
provenance. The optimization candidate was already settled at the accepted
base; no product history note was added. This record retains the actual reading
inputs/results and is spent planning evidence for final Story Wrap Up.
Physical storage cleanup remains explicitly owned by slices 9 and 10.

All 1,662 aggregate consumer dispositions are settled in
`/tmp/donut-slice6-consumer-trace-dispositioned.md`; the orphan-closure supplement
has no external consumers, and the final generated/module supplement dispositions
all nine readers (`/tmp/donut-slice6-orphan-closure-trace.md`,
`/tmp/donut-slice6-final-generated-trace.md`). Root inspected their effect terms,
affected sources, setup/observations and provenance classifications. No missing
slice-6 observation or new behavior gap remains. Source freshness is unchanged
at `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`; no named sibling dependency applies.

## Independent refactoring and final backend proof

The fresh reviewer removed the test-only three-argument request-builder wrapper
and moved duplicated user-message parsing beside `inputText` in the existing
test base. The runtime four-argument method/body is byte-identical to the base;
all four compatibility provenance hashes still match. The resulting builder,
shared base and two test homes are 240/46/213/187 lines before formatting.
This is one backend request concept, with no wire/schema/generator trigger.

Only backend proof was invalidated. Renewed command
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` exited 0 after actual
test execution, 4m7s; log `/tmp/donut-slice6-refactor-backend.log`.
Root parsed all 635 reports: 3,188 tests, zero failures/errors, two skips;
the two request homes passed 9/9 and 11/11 at 05:00 UTC. There was no retry or
input change. Frontend, Node, browser, SQL and generator proof remain applicable.

Fresh trace `/tmp/donut-slice6-refactor-consumer-trace-dispositioned.txt`
settles all 1,751 consumers, including 95 newly inspected readers, with no
empty/pending cells. Whitespace and the 131-commitment obligation check passed.
The supplied test split remains cohesive and all handwritten changed files are
below 250 lines. Independent review returned `REFACTOR COMPLETE`, with no
remaining candidate, contradiction, uncovered consumer or outstanding command.
