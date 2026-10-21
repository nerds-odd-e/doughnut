## Slice 1 accepted evidence

2026-10-21, claim revision 9de0b75a02cf7b8d5b92c0d80bfd2a753f41f095 plus
temporary probe. Independent source-only reviewer `/root/source_review_1` received
only the original Goal, Scope, examples, exclusions and UI clauses; its launch
and complete returned commitments are retained in this execution conversation.
Acceptance compared every commitment: accepted-answer identity, original content,
mark/outcome, creation time, timing, repeated/changed/overlapping submissions and
Grade/Confusion/log/schedule effects remain owned by backend slices 2 and 4;
retained prompt/input/session position, uncertainty feedback, deliberate repeatable
Retry, pending controls, navigation and stale-result protection by slices 3 and 5;
normal advance/history by 3 and 5; overlap's same-tracker new attempt and
accidental-match reveal/resolution under current visibility by 4 and 5;
ownership and ordinary grading by 2 and 4. No product recovery is accepted in this
structure slice. The listed story deferrals remain excluded.

Inspected scratch `RecallPromptSerializationProbeTest.preloadedAnswerIsFreshAfterRefreshWithLock`:
the reader binds a request-owned EntityManager, loads before acceptance
transaction entry, and asserts that the same prompt remains managed inside it.
The writer locks, answers through the real controller, flushes, and waits for an
explicit release. The reader starts the existing locking query and is observed
queued before release. On writer commit, lock-only still returns the same entity
with null answer; `refresh(prompt, PESSIMISTIC_WRITE)` reads the committed answer
ID. On writer rollback, refreshed answer is null and the real controller accepts
a subsequent answer, confirmed from another transaction.

Literal observations: `PROBE rollback=false lock-only=null refresh=1`;
`PROBE rollback=true lock-only=null refresh=null subsequent-accept=3`.
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Final result: 2,857 tests, zero failures/errors, two skipped; Gradle build 55s
plus 10s migration. The failed stronger snapshot variant and its interpretation
are recorded above. Acceptance inspected both XMLs and the actual source before
deletion. Local disposable evidence remains at
`/tmp/recall-serialization-before-transaction.xml`,
`/tmp/recall-serialization-inside-transaction.xml`, and
`/tmp/RecallPromptSerializationProbeTest.java`. Scratch source was removed from
the repository. Active work was approximately 5–6 minutes, with full-suite waits
excluded under the explicit exception.

Dependent acceptance must lock/refresh before nonlocking transactional reads.
Authorization currently compares loaded owner IDs; AnsweredQuestion projects
tracker identity/property focus, not scheduling state. Fresh-context controller
tests in slices 2 and 4 still own complete response/effect proof, including
retained concurrent and rollback observations. No schema/API wire change.
