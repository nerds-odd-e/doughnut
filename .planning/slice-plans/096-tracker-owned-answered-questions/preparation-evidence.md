# Preparation evidence

The following retained observation is verbatim. Its slice numbers and forward
references resolve through the [ordered plan](PLAN.md#ordered-slices) and
[detailed implementation briefs](implementation-briefs.md).

## Decisive premises and observation

Retained production evidence and source readings remain at `1e331f5c5c` as
recorded in the seed. New disposable observations were made on 2026-11-13 in
this owned workspace at `0ec6e1dafa97ef0946fbc2afd0039649318d1c72`, profile
`test`, MySQL **8.4.11**, database
`doughnut_wt_3ede975a67f44093a759a2739bd92420_test`. The local engine version,
FK metadata and actual controller consumers were observed, not inferred from
nullable-column definitions.

| Premise and consuming operation | Result-bearing observation | Remainder/owner |
| --- | --- | --- |
| Reduction can erase a moved tracker's prompt while leaving its answer/log | The seed's two production binlog traces remain applicable. In the scalar compatibility prototype with the FK retained, the real reduction controller followed by a fresh history read returned zero prompts for the surviving graded log. The tracker ID and due time survived. | Slice 9 retains a maintained regression; this is no claim about every historical loss. |
| The inverse move has the same retention requirement | The real `NoteController.reifyProperty`, source trash and permanent-delete controllers, with fresh persistence contexts between requests, also returned zero history prompts while the FK remained. With the tracker-owned prototype and FK dropped, both journeys passed history, authenticated MCP and export reads. | Slice 9 owns lasting canonical payload proof once, with the inverse case asserting its delta. |
| Scalar compatibility writes can be read after fresh ORM hydration | With `Mcq.note` replaced by an Integer `note_id` column and a transient generation input, a saved prompt was flushed/cleared and freshly loaded with its question. SQL read the original input note ID from `mcq.note_id`. The initial original-mapping run also freshly loaded that physical shape successfully. | Slice 6 implements the compatibility mapping. Every original binary must be replaced at checkpoint A before retiring that write. |
| New writes and fresh prompt loads work without a question-note mapping | The final prototype removed the scalar mapping/write. The actual recall-generation controller (external OpenAI mocked) persisted a question and a fresh ORM prompt read returned its stem and current tracker note. The old-note deletion journeys also freshly loaded the retained question after the FK drop. | Slice 9 implements this after checkpoint A; these local observations do not establish production deployment. |
| Existing data never shares one question across different trackers | Current callers allocate fresh questions, but the schema and legacy fixtures permit sharing. No production ownership-count observation is retained. The grouping query and applicable FK shape are available locally. | Authorized early credentialed probe in slice 1; nonzero sharing stops dependent work and preserves every attempt. |
| Application-owned cleanup covers direct tracker, unanswered-prompt and permanent-note deletion | The tracker-owned prototype passed all three real controller paths with populated FK-closure fixtures and fresh reads. Other-tracker answered questions, the answered prompt on the same tracker, and a question still used by another prompt on that tracker survived. Existing conversation/deletion tests ran in the same full suite. | Slice 8 implements the shared lifecycle rule. Bank-only consumers and fixture cleanup are assigned below. |
| Release checkpoints replace every earlier binary and apply the intended schema | Source still runs migrations at ApplicationReadyEvent and rolls two instances. Local DDL is not production deployment evidence. | Owner-authorized checkpoint A/B/C observations must reach actual publication jobs, all active binaries and applied schema before dependent slices proceed. |

### Disposable probe protocol and limits

All three runs used the literal command:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The wrapper selected the isolated database above, migrated it and ran the whole
backend suite. No filtered backend run, production access or paid model call was
made. `TrackerQuestionPreparationProbeTest` was temporary and is deleted.

- Initial original-mapping run: **3,200 tests, 5 failures, 2 skipped**. The fresh
  prompt read passed. Two held-question fixtures reached Hibernate's
  transient-note reference error; three deletion fixtures lacked the required
  batch-owner builder input. These five are not retention/deletion proof.
  The request boundaries and batch fixtures were corrected for the later runs.
- Scalar compatibility run: **3,201 tests, 19 failures, 2 skipped**. The fresh
  scalar write/reload and same-tracker shared-question case passed. Both
  move/delete/history cases failed with zero prompts, and direct tracker and
  unanswered deletion left the question row. Permanent deletion still held its
  fixture tracker in the request context; the final run corrects that boundary.
  The other 14 failures identify seven Git stored-note assertions, six pure
  `QuestionEvaluationTest` fixtures using `forNote(null)`, and the atomic-import
  query through `Mcq.note`; slice 6 owns their consumer conversion/removal.
- Tracker-owned run: **3,201 tests, 21 failures, 2 skipped**. All **seven** temporary probes passed,
  including actual generation and fresh reload, both move/delete/history/MCP/
  export journeys, the three deletion paths, and shared-question retention.
  The 21 failures are seven export fixtures (slice 3), nine Git stored-note
  observations and three bank controller tests (slice 6), the atomic-import
  query (slice 6), and one intermediate-generation row lookup (slice 7).
  `CommittedUserCleanup` is an additional read-through consumer for slice 9.
  These are the observed consumers of the pending feature deletion and query
  conversion; they are not a claim that
  the story implementation or its required full-suite acceptance is complete.

The move fixtures create a wrong MCQ answer at index `1` from choices
`key/picked/third`, record Grade AGAIN, and exclude it with reason
`two interpretations`. After the real move and original-note deletion, the
fresh controller history, authenticated `/api/mcp` `list_recall_answers` call
and current-note Markdown export retain the stem, choices, index, recorded
correctness, grade, exclusion/reason, due time, current note and own selected
answer. Deletion fixtures additionally create a batch request and a prompt
conversation with message and accepted correction, following every incoming FK
in the observed learning subtree. The inverse journey separately invokes trash
and permanent deletion; setup does not simulate its resulting tracker move.

The prototype uses scalar question IDs before tracker deletion and an
application-issued selected-ID DELETE with `NOT EXISTS` on remaining prompts.
Unanswered cleanup uses its existing conversation-detachment boundary. Permanent
note deletion calls that same tracker deletion owner. Querying recent questions
through prompts/current trackers reaches the moved question and own choice.
Surviving authorization/context consumers derive through `RecallPrompt`.
The prototype retained transient bank scaffolding only to compile the untouched
feature; it is not a proposed final representation.

DDL was limited to this disposable database, through
`./scripts/run.sh mysql -h127.0.0.1 -P3309 -uroot --batch --raw
<isolated-database> -e ...`: `ALTER TABLE mcq DROP FOREIGN KEY fk_mcq_note` before
the final run, then `ALTER TABLE mcq ADD CONSTRAINT fk_mcq_note FOREIGN KEY
(note_id) REFERENCES note(id) ON DELETE CASCADE` after it. The restoration query observed zero invalid note
references and the restored `fk_mcq_note` rule as CASCADE.
All scratch Java/test edits were reverted before this record write; the product
checkout matched the starting revision. Raw inputs/logs and summaries were kept
outside the checkout under `/tmp/donut-tracker-preparation-324fe884/`.

Fresh host evidence was taken before the serial runs (load 23.95/37.21/48.32,
about 116 MiB free pages). Each invocation owned only its isolated test database;
no peer process was stopped. The mandatory test tasks took 6m01s and 6m10s in
the first two runs, and 4m01s in the final run, plus migration/startup. Execution
must recheck current headroom rather than reuse that host snapshot.
