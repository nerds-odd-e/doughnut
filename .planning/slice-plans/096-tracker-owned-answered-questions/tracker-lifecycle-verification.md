# Tracker-owned question lifecycle

Slice 8 retains the public `MemoryTrackerService` facade and extracts its
tracker/prompt destruction and conversation detachment into transactional
`MemoryTrackerLifecycle`. Grading, assimilation and history stay in their
existing homes. The collaborator selects scalar question IDs before removal;
the modifying unreferenced-question query flushes pending removals first and
deletes only selected questions with no remaining prompt. Permanent note
removal invokes the same owner inside its note transaction.

The incoming facade was 240 lines. The supplied cohesive seam delivers a
233-line facade and 71-line lifecycle before formatting; the note service
remains 164 lines. No database cascade, schema, API/DTO or generator input
changes. The compatibility scalar write, physical column and FK stay in place.
Later retention/schema outcomes (9/10) remain available; current-tracker
destruction is distinct from deleting a moved tracker's old input note.

## Inspected setup and observations

Four new controller cases use real database data and fresh flush/clear reads:

- Direct tracker deletion populates prompt/question, recorded answer/log,
  batch request, conversation/message and accepted correction. It observes
  tracker/prompt/question/log/request destruction, the retained note and
  answer, detached conversation subject, retained message/correction and
  another tracker's actual history/answer/schedule.
- Same-tracker sharing has answered and unanswered prompts for one question,
  plus an independently owned unanswered question. Unanswered cleanup removes
  its owned question, retains the shared question and answered prompt, and
  preserves conversation evidence. Deleting the final tracker then removes
  that shared question. This preserves Q05 rather than imposing a
  one-prompt-per-question constraint.
- Unmoved note deletion creates its real note through the notebook controller,
  including alias, level, property, authored reference and creator indices,
  skip membership, note conversation/message/correction and the complete
  learning graph. Setup checks each actual direct-note dependent family.
  Trash and permanent-delete controller operations remove the note, tracker,
  prompt and question.
- The current-note case asks before actual property reification, then deletes
  the current tracked note while the input note stays alive. Its question is
  removed and another tracker on the input note retains its real history.
  The existing physical note FK cannot supply that ownership outcome.

Root inspected both focused test classes and product/query diffs. Read-only
FK tracing found tracker children recall_prompt/log/batch request CASCADE and
prompt-to-conversation SET NULL. Production destruction callers converge on
the existing note boundary. The old six-counter helper's comments now describe
its selected scope; compatibility SQL and its 12 readers remain G3's slice-9
work, including pre-destruction question-ID capture to avoid vacuous counts.

## Proof ownership and repair

First required full command reached native terminal 1, 11m52s:
`/tmp/donut-slice8-backend.log`. There were 639 suites, 3,195 tests,
one failure, zero errors and two skips. The sole failure was the shared-question
case's second simulated request: intermediate history reads left a managed
answered prompt referring to its tracker, so Hibernate's existing pre-flush
dependency check ran before the database cascade.

The canonical delete fixtures already clear at request boundaries. A one-line
flush/clear before the second destructive controller request preserves the
same data and assertions while representing that separate HTTP request.
Manually deleting answered prompts would change retained answer semantics and
was not used. The repair took seconds; total editing stayed under nine minutes.
All inputs remain fixed during renewed verification.

Renewed native command 25646 reached terminal 0. The test stage completed in
5m55s after a 1m35s test-DB migration stage. Fresh XML at
`2026-11-13T06:36:31Z` contains 639 suites, 3,195 tests, zero failures/errors
and two skips; all four new cases ran. Root independently counted the XML.
Existing direct deletion (4), unanswered cleanup (7), permanent note deletion
(3), folder deletion (4), Git deletion publication (3) and relationship
reduction existing-key/source-property cases (5/7) passed. Literal command:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`; renewed output:
`/tmp/donut-slice8-backend-renewed.log`.

Launch load 20.77/12.31/11.43 and memory 41% free deteriorated to
73.74/58.96/41.34 and memory 36% free. Bounded worker/DB checks found ongoing
work, no lock wait and no other owned heavy run; host fseventsd was active.
After terminal, renewal used one serial full suite with bounded monitoring
(load 63.64/59.87/44.14, memory 38% free). No host process was changed.

The installed consumer trace reached terminal 0 against published base
`c4ff03f0e9bbf4ac2727d6970121565506f07e82`: 90 readers, 35 run, seven reused
and 48 excluded with explicit reasons, zero empty/pending dispositions.
The selected-counter supplement has 14 readers, 12 run/two reused, zero gaps.
Exact commands, inspected fixture/assertion locations and FK metadata protocol
are retained in `/tmp/donut-slice8-handoff.md`; dispositioned outputs are
`/tmp/donut-slice8-final-consumer-trace-dispositioned.md` and
`/tmp/donut-slice8-count-label-trace-dispositioned.md`. The unused repository
declaration `deleteByMemoryTracker_Id` has no product callers in the acceptance
reading. This is a one-time removal reading, not maintained absence proof.

Source freshness was settled again before independent refactoring: terminal 0,
selected story/plan unchanged at fetched authorized trunk
`9baf479ddac80f7e0b8bf27390f876109b0ff4d9`. No named sibling was supplied.

Fresh independent post-change refactor returned `none — already clean` and
`REFACTOR COMPLETE`. It inspected callers, transaction/query behavior, test
setup, actual XML, FK closure, size and Accepted ADRs. No edits were made;
all accepted proof boundaries and both dispositioned traces remain valid.
No redundant test run was required. No generation trigger applies because
controller signatures, DTOs, schema and frontend inputs are unchanged.

Coordinator selective formatting completed once. The independent commit hook
then stopped on Spotless's zero-lint-error result; no commit/publication was
made. A direct informational check reproduced that result with its formatter
task UP-TO-DATE. All six generated clean copies matched source bytes.
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend
spotlessJavaCheck --rerun-tasks --info` recomputed outputs and reached terminal
0 in 15s; `/tmp/donut-slice8-spotless-renewed.log`. Source/index bytes stayed
unchanged. The retry retains accepted formatting and semantic proof; the real
check-only hook remains enabled.

## Remaining checkpoint

G2/C84 remains open: actual compatible application rollout needs the reviewed
retained landing/tag go-ahead, applicable main CI/artifacts, publication,
all-active binaries and smoke/schema evidence. The read-only pre-release
baseline is retained in [release checkpoints](release-checkpoints.md).
Code/proof progress does not mark slice 8 done or authorize slice 9.
