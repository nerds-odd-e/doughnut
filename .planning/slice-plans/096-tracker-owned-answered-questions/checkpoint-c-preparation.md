# Checkpoint C preparation baseline

Read-only production observation, 2026-11-13. This records the B baseline for
later C review; no C enablement, landing, tag or contract migration occurred.
The exact C candidate/tag and completed local/CI proof remain prerequisites.

## Observed current release and production selection

```sh
./scripts/run.sh python3 /tmp/donut-tracker-delivery-60c866c4/checkpoint-c-preflight.py --expected-tag v1.3.75 --expected-release-sha 34cf85c27b97876419039d6dbb5dc1cb922997d8 --expected-build-sha 34cf85c27b97876419039d6dbb5dc1cb922997d8 --expected-ci-run-id 43413409754 --expected-ci-run-attempt 1 --expected-flyway-version 300000357
```

Native `67246` exited 0; all eight commands exited 0. Window:
12:39:12.502702–12:39:44.301432 UTC. The immutable tag and succeeded application
record match all explicit inputs. Dynamic before/after enumeration reached a
stable targetSize 1 with unchanged complete membership. Its sole active
`doughnut-app-group-qk27`, ID `5334386849658087999`, was healthy and reported
prod build `34cf85c27b97876419039d6dbb5dc1cb922997d8`.

At 12:39:37.554272 UTC, MySQL `8.4.11-google`, database `doughnut`, read-only
transaction 1, reported:

| Aggregate | Count |
| --- | ---: |
| All MCQ rows | 146,997 |
| Distinct questions referenced by any recall prompt | 132,878 |
| Questions referenced by no prompt, selected by cleanup | 14,119 |

The selection uses exactly `NOT EXISTS (SELECT 1 FROM recall_prompt rp WHERE
rp.mcq_id = m.id)`, independent of answer presence. Total equals referenced
plus selected. No learner IDs, question text, answers or note content were read.
This count is an observed baseline rather than a write freeze; live traffic
and separately owned operations can change later database state.

## Applied shape, full FK closure and process-read capability

Latest/applied migration 357 succeeded. The nullable physical note_id column
and its nonunique fk_mcq_note secondary index remain; its note FK count is 0.
The complete current qualified FK catalog was captured, not inferred solely
from committed ERD. The MCQ delete walk has exactly:

- `mcq` → `recall_prompt.mcq_id`, constraint fk_recall_prompt_mcq_cascade,
  CASCADE. The cleanup predicate selects rows with no such prompt.
- `recall_prompt` → `conversation.recall_prompt_id`, constraint
  fk_conversation_recall_prompt, SET NULL; the delete walk stops there.

No restricting or additional cascade edge was observed in this closure. The
local full asked-graph proof must still include conversation/message/correction
and exact prompt identities because answer/log counts alone can miss prompt loss.

Existing authenticated OS Login and guest ED25519 verification were reused.
The same safe sudo-n /proc reader intended for C found exactly one canonical
application JVM, PID 1033; allowlisted gate arguments were empty at B. This
observes access to that runtime seam before any C gate change. It does not
establish the future enabled value. Full process arguments and secrets stayed
in remote process memory and were not captured. SQL explicitly used UTC,
READ ONLY/consistent snapshot, a SELECT time bound and ROLLBACK confirmation.

## Review and evidence limits

C's human go-ahead must cover the exact reviewed retained landing/tag/SHA and
deliberate production enablement. Local populated cleanup/contract proof,
applicable CI/artifacts and actual all-active/schema/public/manual observations
remain required. B authorization is not C authorization. Retain the pending
migration harness/configuration through actual C, then complete the separate
normal-startup lifecycle before final acceptance.

The baseline can be reused while the relevant release/schema/selection
conditions still match; reassess after a relevant change or a release delay.
Membership and metadata were checked across this bounded reading, not frozen
against future changes. Information-schema metadata is not an InnoDB MVCC freeze.

Raw inputs, timestamps, output hashes, complete FK metadata, counts, host-key
fingerprint and rollback marker:
`/tmp/donut-tracker-delivery-60c866c4/checkpoint-c-preflight-al2yzfoz/observation.json`.
Prepared C postrelease scripts are syntax-checked only and have not run;
actual C admission identities must be supplied at use.
