# Stored question ownership observation

**Execution observation (2026-11-13 01:39:59.235160 UTC).** Read from this
checkout at `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`, using the owner's
existing signed-in credential and existing OS Login key. Current MIG discovery
returned one healthy member, `doughnut-app-group-qk27`, instance ID
`5334386849658087999`; this is a snapshot, not a fixed instance-count claim.
The same instance's healthcheck reported profile `prod`, revision
`12288107bd2a2dbbfc2eabc2d00f44e0d4ed4e74`. MySQL reported
`8.4.11-google`, database `doughnut`, session `transaction_read_only = 1`.

| Aggregate | Observed count |
| --- | ---: |
| Questions shared by more than one tracker | 0 |
| Questions unreferenced by any prompt | 14,119 |
| Prompts | 142,867 |
| Distinct referenced questions | 132,878 |

Zero cross-tracker sharing settles this slice's premise and permits the
planned ownership transition. No production data, schema, application,
credentials, IAM or SSH metadata changed. No private question text was read.
These counts establish this snapshot only; they do not explain historical
losses or constitute deployment/schema confirmation for later checkpoints.

The observed delete rules were `mcq.note_id → note` CASCADE,
`recall_prompt.mcq_id → mcq` CASCADE,
`recall_prompt.memory_tracker_id → memory_tracker` CASCADE,
`memory_tracker.note_id → note` CASCADE,
`memory_tracker.user_id → user` CASCADE,
`recall_log.memory_tracker_id → memory_tracker` CASCADE,
`question_generation_batch_request.memory_tracker_id → memory_tracker` CASCADE,
`conversation.recall_prompt_id → recall_prompt` SET NULL and
`recall_prompt.answer_id → answer` NO ACTION. All observed update rules were
NO ACTION.

Literal discovery and successful read commands, run in this checkout:

```sh
./scripts/run.sh gcloud compute instance-groups managed list-instances doughnut-app-group --project=carbon-syntax-298809 --zone=us-east1-b --format=json
set -o pipefail
./scripts/run.sh gcloud compute ssh terry_odd_e_com@doughnut-app-group-qk27 --project=carbon-syntax-298809 --zone=us-east1-b --plain --ssh-flag=-i --ssh-flag=/Users/terryyin/.ssh/google_compute_engine --ssh-flag=-oBatchMode=yes --ssh-flag=-oConnectTimeout=15 --ssh-flag=-oStrictHostKeyChecking=yes --ssh-flag=-oHostKeyAlias=compute.5334386849658087999 --ssh-flag=-oUserKnownHostsFile=/tmp/donut-tracker-ownership-3glcav7e/known_hosts --command='bash -s' < /tmp/donut-tracker-ownership-3glcav7e/probe.sh | tee /tmp/donut-tracker-ownership-3glcav7e/result.txt
```

The script reads `/api/healthcheck`, obtains the app's existing database
credential in process memory through its documented metadata-token/Secret
Manager route, and invokes `mysql --host=db-server --port=3306
--user=doughnut --database=doughnut --connect-timeout=15 --batch --raw` with
process-scoped `MYSQL_PWD`, without tracing or printing credentials. It executes:

```sql
SET SESSION time_zone = '+00:00';
SET SESSION MAX_EXECUTION_TIME = 30000;
SET SESSION TRANSACTION READ ONLY;
START TRANSACTION WITH CONSISTENT SNAPSHOT;
SELECT UTC_TIMESTAMP(6) AS observed_at_utc, VERSION() AS mysql_version,
       DATABASE() AS database_name, @@transaction_read_only AS transaction_read_only;
SELECT COUNT(*) AS cross_tracker_shared_questions
FROM (SELECT mcq_id FROM recall_prompt WHERE mcq_id IS NOT NULL
      GROUP BY mcq_id HAVING COUNT(DISTINCT memory_tracker_id) > 1) AS shared;
SELECT COUNT(*) AS unreferenced_questions FROM mcq AS q
WHERE NOT EXISTS (SELECT 1 FROM recall_prompt AS p WHERE p.mcq_id = q.id);
SELECT COUNT(*) AS prompts, COUNT(DISTINCT mcq_id) AS referenced_questions
FROM recall_prompt;
SELECT k.TABLE_NAME, k.COLUMN_NAME, k.CONSTRAINT_NAME,
       k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME,
       r.DELETE_RULE, r.UPDATE_RULE
FROM information_schema.KEY_COLUMN_USAGE AS k
JOIN information_schema.REFERENTIAL_CONSTRAINTS AS r
  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
 AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME AND r.TABLE_NAME = k.TABLE_NAME
WHERE k.CONSTRAINT_SCHEMA = DATABASE() AND k.REFERENCED_TABLE_NAME IS NOT NULL
  AND (k.TABLE_NAME IN ('mcq', 'recall_prompt', 'memory_tracker')
       OR k.REFERENCED_TABLE_NAME IN ('mcq', 'recall_prompt', 'memory_tracker'))
ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION;
ROLLBACK;
```

Terminal result: exit 0. Raw aggregate output and credential-free script are at
`/tmp/donut-tracker-ownership-3glcav7e/{result.txt,probe.sh}`. The first strict
SSH attempt stopped on a stale locally stored host key; authenticated GCP guest
attributes supplied the matching offered ED25519 fingerprint
`SHA256:8/LGOh27dMKJtnUDFxpKFe8bqGGubOaqkQ3QbSfe5ro`, used in a separate
temporary known-hosts file while preserving the user's stored file. A second
attempt using gcloud's default local username was rejected before script
execution. Read-only instance/project metadata established OS Login was enabled;
the existing OS Login profile matched the exact local public key and supplied
the successful username above. No key was enrolled or refreshed. Query execution
took under 6 seconds after SSH launch; preparation/access diagnosis was separate
from that bounded production read. No automated test or consumer-contract change
was needed for this observation-only slice.

Coordinator acceptance: inspected the read-only SQL/script and raw aggregate
result above; the distinct-tracker grouping returned zero, with the engine,
schema rules and deployed revision observed at the stated boundary. Source
review allocates no product commitment to this premise-only slice. Delivery
classification: `records-only`; no contract consumer changed and no independent
refactor is required. Source freshness was unchanged at `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`;
the selected story/plan names no sibling coordination dependency.
