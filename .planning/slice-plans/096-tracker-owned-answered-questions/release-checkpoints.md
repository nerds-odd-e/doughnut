# Release checkpoint evidence

## Checkpoint A preparation

G2 retains the required go-ahead for a reviewed Dough Land `--keep-worktree`
increment and its exact immutable stable application tag/SHA, followed by
actual publication, all-instance and smoke/schema observations. Slice 8 stays
planned until those observations settle. No release tag or production write
has been performed by this execution.

Read [release-application](../../../.agents/skills/release-application/SKILL.md),
its [canonical runbook](../../../docs/gcp/conditional-backend-deploy.md) and
the [smoke contract](../../../docs/gcp/prod-frontend-static-lb.md#smoke-checks-after-a-change).
Admission requires an unused increasing `vMAJOR.MINOR.PATCH`, an exact main
commit and successful applicable main CI/artifacts. Ordinary main publication
alone starts no application release. ApplicationReadyEvent health alone does
not establish migration application.

### Observed pre-release baseline

Read-only observation on 2026-11-13, execution branch based at
`c4ff03f0e9bbf4ac2727d6970121565506f07e82`:

- Remote stable-tag inventory has 75 tags; highest numeric tag is `v1.3.73`.
  Its raw annotated ref is `b3a9d01f68af44711d9b209ea93d17a3a2010344`.
  `v1.3.74` is the next patch candidate at this reading and must be rechecked
  before approval/publication. No final release SHA is selected yet.
- Connected GitHub read found Application Release run `43355503494`, attempt 1,
  for `v1.3.73`, head `a13ab0cb63edddf63e11e8e5735d4e313579714e`, started
  `2026-11-13T02:55:52Z`. Admission job `131656466172` and actual publication
  job `131656654662` both succeeded, including the GCS/MIG publication step.
  This existing release is separate from the execution's future checkpoint A.
- Current MIG discovery returned one healthy running instance with action NONE:
  `doughnut-app-group-qk27`, ID `5334386849658087999`. This is a snapshot,
  not a fixed instance-count assertion. Group `isStable` and
  `versionTarget.isReached` both returned true; target size is 1 at this read.
- The instance health endpoint reported profile `prod` and commit
  `a13ab0cb63edddf63e11e8e5735d4e313579714e`.
- At `2026-11-13 06:07:14.555640 UTC`, MySQL `8.4.11-google`, database
  `doughnut`, `transaction_read_only = 1`: Flyway `300000354` succeeded
  (installed rank 961), and `conversation_note_correction` exists. The
  runbook's prerequisite is settled. Latest applied versions are 356/355/354,
  all successful; later story migrations must allocate above the actual tip.
- Physical `mcq.note_id` is nullable, and `fk_mcq_note` retains CASCADE to note.
  These are the compatible pre-release schema conditions; G2 remains open.

Raw read-only protocol and results:
`/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/donut-checkpoint-a-34mdkp9x/`.
It contains `schema-read.sh`, `result.txt`, `hostkeys.json`, `group-status.json`
and the separately verified temporary `known_hosts` file. MIG discovery:
`/tmp/donut-checkpoint-a-mig-before.json`.

The first strict SSH attempt stopped before query execution after host-key
rotation. Authenticated GCP guest attributes supplied the offered ED25519
fingerprint `SHA256:ww1bowSsZP51Tng0R5r7INXgcCXTVpXPe3CSjC+ZkxE`; a separate
temporary trust file preserved both the user's file and the earlier dated
probe. The subsequent read exited 0 using the existing OS Login key/user.
No key enrollment, SSH/IAM metadata change, private question read or database
write occurred. The script obtains the existing DB credential only in process
memory via the previously verified metadata/Secret Manager route.

SQL actions used session UTC, a 30-second SELECT bound, READ ONLY and a
consistent snapshot. They read the engine/database/read-only flag, version
354 and latest three Flyway records, table existence, nullable column and
existing FK rule, then rolled back. No operational health result is being
substituted for checkpoint A's future all-active-binary observation.
