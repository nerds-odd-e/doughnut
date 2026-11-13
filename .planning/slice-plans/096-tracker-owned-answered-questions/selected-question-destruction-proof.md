# Selected-question destruction proof

Observed 2026-11-13 in this story worktree, starting from
`6863f1e6b3ae22941728c36cb9a45284a636641c`. This Structure slice prepares
destruction proof and committed-fixture isolation for Behavior 12. Production
mapping, writers, schema and controller behavior are unchanged.

## Selected IDs and observing boundaries

`NoteDependentRowsControllerTestBase.questionIdsForNote` selects distinct
question IDs through prompts and their current trackers before an action.
`dependentCounts(note, questionIds)` counts those same question rows directly;
the other five counters retain their expressions. No post-deletion selection
or hidden cache can lose the questions being observed.

Inspected pre-action capture and subsequent observations:

- `NoteControllerPermanentDeleteTests` and both folder-delete scenarios capture
  before controller deletion and retain their dependent-row observations.
- `NotebookGitDeletionPublicationControllerTest` captures both notes before the
  first publication and passes the lists to its direct question-count helper.
- `NotebookGitDeletionPublicationRetryControllerTest` and
  `NotebookGitIdempotentPublishControllerTest` use the same captured list in both
  publication snapshots.
- `NotebookGitDeletionPublicationAtomicControllerTest` and
  `NotebookGitComposedRangePublicationAtomicControllerTest` use the same lists
  for their initial counts and rollback observations.
- Composed deletion/addition, trash/move and both recreation helper cases
  capture before publication. Their existing identity/survival observations
  remain; tracker-only survivor/rejection checks use the existing tracker count.

Existing fixtures, test counts and assertions in the 26 edited existing sources
retain their purposes. The complete graph and moved-current-note deletion in
`NoteQuestionDeletionControllerTest` are untouched and pass in this run.

## Committed fixture graph

`CommittedUserCleanup` selects IDs and deletes prompts using one shared native
predicate: affected tracker users **or** trackers on affected users' owned notes.
Optional note/ownership joins cover nullable notes and circle ownership. After
prompt deletion it calls the existing repository's unreferenced-ID deletion;
the remaining cleanup order and caller transactions are preserved.

The sole new test, `CommittedUserCleanupTest`'s
`cleansLearningOwnedByTheTargetOrItsNotesAndKeepsUnrelatedLearning`, constructs:

- learner A's question on a surviving, B-created circle-owned note;
- learner B's question on A's owned note;
- an unrelated B prompt/question and surviving notes/user.

Both affected questions reference a surviving input note, so its physical FK
cannot hide missed cleanup. Scalar IDs are captured before flush/clear; fresh
post-cleanup reads observe the two affected questions, prompts and trackers
removed, A's note/user removed, and unrelated learning preserved. Behavior 12
retires this test's explicit compatibility setup while retaining its graph proof.

Eleven cleanup caller homes inherit one repository injection from
`SpringTestBase`. `QuestionGenerationBatchRowImportServiceAtomicTest` retains
its existing dedicated context and injects that existing bean directly. No
Spring annotation mix or inheritance changed; this corrects the initial
preflight's all-inherit assumption.

## Full backend observation

Literal commands, each owned to its native terminal result:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice11-destruction-red.log 2>&1
CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice11-destruction-green.log 2>&1
```

RED launched 09:04:54 UTC; native `88176` exited 1, observed 09:16:14.
641 fresh suites ran 3,196 tests: one failing test, zero errors, two skips.
Its two assertion failures were the intended leaked question rows; all other
graph observations passed. No FK or unrelated fixture failure supplied the red.

GREEN launched 09:18:57 UTC; native `94624` exited 0, observed 09:24:45.
641 fresh suites ran 3,196 tests: zero failures/errors, two skips. The new graph
case passed. XML modification times were 09:24:36.232549–09:24:36.359190 UTC;
the coordinator independently counted the complete result. Sources were stable
from 09:18:51 and their 27 hashes/sizes were retained with the handoff.

Editing/cleanup totaled about 3m05s. Required full-suite waits were separate:
11m20s RED and 5m48s GREEN including setup. On this 16-CPU/48-GiB host, RED
load rose from 25.58/28.03/25.16 to 56.63/45.70/36.07; fresh binary results
established progress. No duplicate owned proof ran; unowned work was preserved.
Before GREEN load was 36.85/43.63/37.32 with 43% free memory. The required
single full run was retained, not relaunched. Javac's native-query unchecked
note was visible; compilation and all tests passed.

## Consumer dispositions and acceptance reading

The installed consumer trace searched the checkout at this base with terms
`dependentCounts`, `questionIdsForNote`, `countQuestionsByIds`,
`deleteByUserExternalIdentifierLike`, `countRowsByNoteId` and
`countRecallPromptsByNoteId`. All 34 discovered test readers ran in GREEN;
the two guidance readers retain their link targets/headings. No production or
journey reader was reached and no consumer remains pending. The complete
capture map and dispositions are in `/tmp/donut-slice11-handoff.md` and
`/tmp/donut-slice11-consumer-trace-dispositions.md`; these index the inspected
current sources and do not replace their observations.

Separate one-time reading at 09:19:38 UTC:
`rg -n 'countRowsByNoteId\("mcq"|dependentCounts\(Note note\)' backend/src/test/java`
found zero matches. No retained checker or absence test was added.

Source freshness after GREEN returned unchanged against pinned trunk
`c2305a93943ee83228ac919e2af4bec9056d4353`; its incoming changes affect only
backlog/retrospective records, with no product or migration change. The retained
source-only review remains applicable. These inspected counters and graph paths
settle G3; the move/delete retention and deployment promises remain with 12/13.

## Independent refactor

The fresh review corrected one atomic-publication comment to describe deletion
without attributing application-owned question cleanup to a cascade. The other
26 sources match their GREEN hashes; reversing that comment word also matches
the remaining hash. All executable proof is reused unchanged. The refactor's
sole guidance consumer retains its capture/rollback description; no consumer is
uncovered. Whitespace passed and all 32 files remain within 250 lines. No tests
were rerun for the comment. The review returned `REFACTOR COMPLETE`.

Guarded formatting passed, then the check-only hook reported Spotless's
up-to-date Java task with zero listed lint errors and stopped before commit.
Forced `spotlessJavaCheck --rerun-tasks --info` (native `2346`, exit 0)
recomputed and passed. No repair changed source bytes; the accepted single
formatter pass and semantic proof remain valid for the guarded retry.
