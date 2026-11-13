# Selected questions and prompt persistence

Slice 7 makes feasibility evaluation an in-memory operation. The existing
`RecallPromptPersister` rechecks ordinary generation, then writes its selected
question and prompt in one transaction. Its explicit new-prompt entry also
owns regeneration of an already asked question, preserving that operation's
new-payload behavior when an original unanswered prompt exists. Compatibility
input remains populated from the tracker's current note.

Retiring intermediate future writes was compared with all later outcomes:
tracker cleanup (8), retention of asked history (9) and legacy-orphan cleanup
(10). Asked history and temporary scalar/FK/column remain available, with no
destructive dependency conflict. The old intermediate-row lookup, assertions
and exclusive repository dependency were deleted. The retained feasibility
scenario observes its selected result.

## Inspected controller observations

`MemoryTrackerQuestionPersistenceControllerTest` invokes actual property
reification before generation. It flushes/clears, reloads the prompt/question,
and observes the selected stem, ordered choices, correct index, focus,
rationale, context seed, tracker identity and compatibility input. Captured
external AI input names the exact current focus title. The question-count
delta proves one selected ownership event, without imposing a global
one-prompt-per-question constraint.

The sibling case creates a real competing prompt in the one-shot external AI
callback, between the outer lookup and the transactional recheck. A fresh load
returns that prompt and its question identities with the same one-question
delta. This reaches the inner recheck rather than short-circuiting generation
through its initial lookup.

`RecallPromptRegenerateControllerTest` freshly reloads the replacement question,
tracker and compatibility input. Its existing actual-prompt regeneration,
current-note AI context, access and incomplete-content cases remain.
Existing `OpenAiCallDoesNotHoldTransaction` cases retain normal-generation
observations outside a test transaction.

## Application-owned atomicity

Acceptance identified G1: the new payload/count tests inherit a test transaction
and cannot alone establish application-owned rollback. A separate nested case
uses `NOT_SUPPORTED`, committed setup/cleanup and a one-shot AI callback that
commits only tracker deletion while leaving the note valid. The subsequent
prompt write reaches a real SQL integrity fault. A separate committed read
compares durable question cardinality with its pre-call value. This observes
atomic ownership, not a removed intermediate interface or error wording.
The current observation passed at 05:29:58 UTC on 2026-11-13: the real SQL
integrity fault was reached and durable question cardinality retained its
baseline. Root inspected the test's setup, application call and separate read,
plus its actual 1/1 XML result. This supplies G1's missing observation.

The initial full suite exited 0 before G1 was added: 636 XML suites,
3,190 tests, zero failures/errors and two skips, 6m4s;
`/tmp/donut-slice7-backend.log`. Its source inputs stayed fixed. The G1
addition took about two minutes, bringing cumulative editing to about five
or six minutes; required full-suite waits are separate.

Renewed literal command after the G1 addition:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice7-backend-atomic.log 2>&1
```

Native terminal exit 0, 7m3s, actual tests executed. Root parsed all 637 XML
suites: 3,191 tests, zero failures/errors and two skips. The new ordinary/race
cases pass 2/2, outside-harness atomicity 1/1, actual regeneration 5/5,
recovery 3/3, no-transaction AI cases 2/2, batch atomicity 1/1 and batch import
2/2. Inputs stayed fixed, with no retry. Host load was 51.51/37.38/28.97,
memory 48% free and no competing owned worker before the serial run; a bounded
progress check found the exact worker active on the heavily loaded host.
The longer indivisible wait is recorded separately from bounded editing.

## Existing batch producer

Root and the implementer inspected `QuestionGenerationBatchRowImportService`:
its selected question/prompt and imported status already share its transaction.
The retained atomic-import test observes rollback and retry through that path.
Fresh independent refactoring routed that selected pair through the same explicit
new-prompt writer and deleted its unused persister dependency. Root verified the
factory sets exactly the same three prompt fields. Batch admission, unconditional
creation, enclosing transaction, status updates and metrics remain unchanged.

Renewed command `CURSOR_DEV=true nix develop -c pnpm backend:test_only >
/tmp/donut-slice7-refactor-backend.log 2>&1` exited 0 with actual tests
executed, 2m2s: 637 suites, 3,191 tests, zero failures/errors and two skips.
Batch atomicity 1/1/import 2/2, G1 1/1, ordinary/race 2/2 and actual
regeneration 5/5 all passed with their setup/assertions unchanged.
Normal-generation/current-input/compatibility proof retains applicability.

The independent pass returned `REFACTOR COMPLETE`; its separate read-only
review found no test or record gap. Consumer trace
`/tmp/donut-slice7-refactor-consumer-trace-dispositioned.md` settles all 47
consumers with zero empty/pending cells. The implementation's 47-reader trace
is `/tmp/donut-slice7-consumer-trace-dispositioned.md`. Whitespace and the
131-commitment obligation check pass. All handwritten changed files remain
under 250 lines; the importer is 69. No API, component or schema generation
is triggered. Prelaunch load 3.14/13.11/23.97, memory 47% free and no competing
owned worker supported one serial passing run without retry.
