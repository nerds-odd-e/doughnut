# Tracker-move retention verification

Local Behavior 12 proof, observed 2026-11-13 in the established worktree from
`b7213b7465b3ee84d26fd7d679a7a03735ac9776`. Checkpoint B remains open: local
proof does not establish production publication or application of the migration.

## Source result and implementation

Both story journeys now retain their original tracker, prompt, question,
answer and log identities after real movement and old-note deletion. Fresh
history retains question/choices, index 2/incorrect, grade, outcome, ambiguity
exclusion, timers and schedule. Real MCP, export, contest and conversation
requests observe the destination note. Ordinary owner access and application
deletion proof remain in the full suite.

`Mcq`'s hidden scalar mapping and its sole production writer are deleted.
`GeneratedQuestionPostProcessor.assembleMcq` retains two meaningful arguments;
generation/evaluation/regeneration still receive their actual Note input.
Batch import still uses atomic selected-question/prompt persistence.
The prompt fixture builder uses ordinary `please(needPersist)` again. Its small
opt-in generated-time operation supplies historical fixture time; other callers
retain their default. Exclusive physical-note test setup/getter observations
are retired while payload, current-note permission and cleanup survivors remain.

`V300000357__drop_mcq_note_foreign_key.sql` contains only:

```sql
ALTER TABLE mcq DROP FOREIGN KEY fk_mcq_note;
```

The nullable physical column and index remain. No new cascade, baseline rewrite
or data cleanup is part of this stage. Current/fetched SQL tip 356, Java tip
342, local applied maximum 356 and reachable collision history were checked
before allocating 357. No production history was newly queried for allocation.

## Inspected journey boundaries

`ListRecallAnswersMovedTrackerMcpEndpointTest` owns two cases; its dedicated
`MovedTrackerRecallAnswerTestBase` owns only their fixture and fresh-read support.
Both reuse the existing MCP base/context without new Spring annotations or
controller visibility. All handwritten sources are within 250 lines.

- Canonical: answer index 2 incorrectly on relationship R's note tracker,
  exclude ambiguity, then POST the real reduction. Its existing implementation
  permanently deletes R. The same tracker is read on S/property `related`.
- Inverse: author real `related: "[[Target]]"` frontmatter before asking through
  S's property tracker; POST reification, then real trash and permanent-delete
  requests for S. The same tracker is read on R with note-level focus.
- Generation is now minus 25 hours, answer minus 24, exclusion minus 12.
  Baselines follow real exclusion. Only scalar IDs and fresh immutable HTTP
  JSON cross destructive requests; the helper flushes/clears between actions
  and reads, including trash-to-permanent-delete.
- Before each move, temporary native UPDATE **and positive SELECT equality**
  establish a populated legacy original-note value. This setup remains until B;
  fresh NULL rows cannot stand in for legacy retention.

Canonical shape is asserted once: stem, three choices, key 0, recorded index 2,
incorrect mark, JSON-null Answer outcome, AGAIN log grade, answer/log IDs,
timestamps, exclusion reason, all timers and cached scheduling evidence. Complete
fresh history equality preserves the entire payload. The inverse compares its
own complete baseline and destination/focus delta. Tracker reads independently
preserve stability, difficulty, recall/assimilation times, activity and type.

Real bearer MCP observes current title, Portable path and graph focus; canonical
choice text, key, rationale, answered time and exclusion are explicit. The S
export includes the moved question/choices and learner choice independently of
the key. Note-level R export omits the property selector. After R's deletion,
contest captures the precise S focus in the external-AI request and keeps the
question; conversation subject/export retain the same prompt/tracker and S focus.
Conversation export observes its actual `/input/0/content` structured field.

## Full red/green evidence

Literal required commands:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice12-retention-red.log 2>&1
CURSOR_DEV=true nix develop -c pnpm backend:verify > /tmp/donut-slice12-retention-green.log 2>&1
```

Accepted chronology-correct RED, native `17378`, exited 1 with production
mapping/writes/FK unchanged. All 642 classes/3,198 tests ran: exactly the two
new cases failed, zero errors, two skips. Both real journeys retained their log
but returned `recallPrompt: null`. Canonical positive payload preconditions
passed. Test-start timestamps span 10:02:57.262–10:04:41.850 UTC. Exact sources,
XML, log, summary and 14 HTTP observations are in `/tmp/donut-slice12-red`.

Final GREEN/schema gate, native `57947`, exited 0: 642 classes, 3,198 tests,
zero failures/errors and two skips; both new cases pass. Test-start timestamps
span 10:13:08.843–10:15:33.875 UTC. The coordinator independently counted all
fresh XML; modification times are 10:15:34.877656–10:15:34.942790 UTC. Sources,
XML, log and summary are archived in `/tmp/donut-slice12-green`.
`backend:verify` internally formats, migrates and runs the full suite once;
that same command supplies GREEN and the schema gate.

Earlier attempts remain as dated evidence, never substituted for final proof:

| Native | Result and diagnosed correction | Archive |
| --- | --- | --- |
| 22676 | Wrong-reason 403 before answering; align controller TestBean user with token learner | `/tmp/donut-slice12-red-auth` |
| 39597 | Inverse reached loss; canonical JSON-null assertion stopped early | `/tmp/donut-slice12-red-mixed` |
| 74344 | Both reached loss; then strengthen generation-time chronology | `/tmp/donut-slice12-red-before-chronology` |
| 52460 | Retention passed; empty selector correctly rejected and conversation input was an array | `/tmp/donut-slice12-green-projection` |

Each has two failures, zero errors and the complete fresh suite. The final two
projection corrections leave the accepted RED's earlier history-loss point,
fixture and move actions unchanged; no redundant RED was needed. Each renewal
addressed a concrete diagnosed error, and every native command reached terminal.

The 52460 archive's 24 HTTP observations show inverse MT4/P1/Q1/A1/log1,
S14-to-R16; canonical MT5/P2/Q2/A2/log2, R19-to-S17. These numeric observations
belong to that failed-tail run. Final GREEN freshly captures and compares its
own IDs; successful XML does not print their numeric payloads.

## Catalog and generated artifact

Local catalog observation `97081`, exit 0, shows successful version 357,
`note_id YES int unsigned` and retained nonunique `fk_mcq_note(note_id)`.
This is a one-time catalog reading, not a maintained absence guard.

```sh
CURSOR_DEV=true nix develop -c env DONUT_ERD_SCHEMA=doughnut_wt_6c52c877a9c84ab186ec2c44fc117692_test pnpm export:database-erd
```

ERD export `7290` exited 0 against that exact isolated migrated catalog. The
generated diagram changes only the note-to-question FK edge and its FK-column
marker; no generated output was hand-edited. `ERD EXPORTED` was returned.
API generation is inapplicable: the retired scalar was JsonIgnore; controller
signatures and DTO/wire fields are unchanged.

## Consumers, removal reading and limits

The installed trace and purpose dispositions are retained in
`/tmp/donut-slice12-consumer-observations.md`: 89 full-backend readers ran GREEN,
99 independent DTO/fixture, unrelated-entity and dated-record readers are
excluded with reasons. Generation/batch atomicity, current-note access/context,
ordinary destruction/retry/rollback, privacy, exports and MCP retain their
observations. The appended final receipt supersedes its pending-run state while
preserving that report's time/provenance.

An explicit `Mcq` entity expansion closed the suppressed generic term: 213
readers, 115 additional paths beyond the earlier report. All additional paths
are disposed: 38 reuse actual GREEN boundaries and 77 are excluded with concrete
unchanged-wire/provider/fixture reasons. All 26 additional concrete backend test
classes resolve to their real FQCN/nested XML with passing cases. Actual committed
cleanup and the testability controller were inspected; no runtime consumer or
missing observation remains. Record: `/tmp/donut-slice12-entity-consumer-closure.md`.

Separate one-time whole-product reading found zero retired scalar APIs and old
Note-argument call forms outside planning. The retained atomic question save,
current postprocessor calls, independent TS story, immutable baseline, new
migration and pending legacy fixture were individually classified. No absence
test, guard, historical product note or renamed observation was added.

Editing/cleanup was conservatively estimated at 8m50s, including fixture repairs;
required full-suite/migration/ERD waits are separate. The 16-CPU/48-GiB host had
38% free memory before GREEN; unowned filesystem/Node work later raised load to
30.62/20.89/15.94. The progressing owned run reached terminal without duplication
or intervention in unowned processes.

Acceptance freshness pinned trunk `50fc79bdc3c6b2ed7d3e61ef4abfa74bc2382b50`
with unchanged selected source/plan. Incoming changes only refine/plan SEED-129;
they contain no product or migration change. Reconcile that history before the
reviewed checkpoint candidate. All 36 actual source commitments are carried
individually in the retained review; the earlier brief's count of 37 was a
clerical error and added no promise.

G4 owns human-reviewed B landing/tag, actual publication, every active unmapped
binary, production FK migration and smokes. Retire the temporary native legacy
write/read/helper after B and before 13, preserving both permanent journeys.

## Independent refactor

The fresh review and its independent helper/cohesion reading found no candidate
or edit. Archived GREEN sources match the new test, helper and prompt builder
byte-for-byte; all executable proof and consumer dispositions remain applicable.
Changed-source selection, actual handwritten size limits and whitespace checks
passed. The 272-line generated ERD stays generator-owned. No tests were rerun,
and the review returned `REFACTOR COMPLETE` with G4 still open.
