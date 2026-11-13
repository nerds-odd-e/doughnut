# Release checkpoint evidence

## Checkpoint A preparation

Preparation required a go-ahead for a reviewed Dough Land `--keep-worktree`
increment and its exact immutable stable application tag/SHA, followed by
actual publication, all-instance and smoke/schema observations. Slice 8 stays
planned until those observations settle. At the 06:07 pre-release baseline
below, this execution had performed no release tag or production write.
The subsequent approved publication is recorded in the observation section.

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

## Checkpoint A publication and observation

The developer answered `yes` to the reviewed checkpoint handoff covering
Dough Land `--keep-worktree` and immutable `v1.3.74` at
`4059ac2353c590f33c971f5d531cf3853dacd4a8`, conditional on main CI/artifacts.
The retained-increment completion check correctly reports unfinished slices
8–10 and G2/G3. No assignment, plan, worktree or execution branch was retired.

### Landing and release identity

- Installed history-preserving publication accepted that exact SHA on
  `origin/main`; no candidate rewrite or rejected push occurred. The fetched
  suffix base was `9baf479ddac80f7e0b8bf27390f876109b0ff4d9`. The queued-closure
  guard passed; exact-identity guards prevented a different merge candidate.
- The first trunk publication used the supplied launch landing context.
  Capture is recorded: delivery `676fe139-0526-4b73-8316-86e725be0d5a`, receipt
  `0990e918-fe44-41bf-af5c-3249d5ed7c02`, at `2026-11-13T07:05:35.260Z`.
  Result: `/tmp/donut-tracker-delivery-60c866c4/land-checkpoint-a-result.json`.
- The supplied default checkout was clean on main and advanced safely to
  `4059ac2353c590f33c971f5d531cf3853dacd4a8`. The execution workspace remains
  on its original story branch. There was nothing new to commit for landing.
- Exact main-push `donut CI` run `43383257124`, attempt 1, succeeded. The
  main-target completion receipt has exact evidence and confirmed shutdown.
  Required unexpired artifacts: backend `14017787128` (210,179,950 bytes,
  digest `94f2c34df7e420275a7d0ed8389afaca00a05bad7c25b300a4ab799ef963424f`)
  and frontend-dist `14017672606` (4,453,735 bytes,
  digest `e79561f511803ab8c9bb97c8ab39c1e26c631e72ddfd65e46021d8468bf7ae01`).
- Rechecked unused/increasing version, fetched tags, then created and pushed
  annotated `v1.3.74`; raw OID `137cc2b8d0b31e47320c52ca8cd86fb4d27affe5`,
  peeled SHA equal to the approved commit. The tag was not moved or retried.
- [Application Release run 43384379117](https://github.com/nerds-odd-e/doughnut/actions/runs/43384379117),
  attempt 1, succeeded. Admission job `131749314613` reported `ready` for
  that tag/raw OID/release SHA, build SHA equal to release SHA and main CI
  `43383257124`/1. Actual deployment job `131749515563`, including artifact
  validation and `Publish application to GCS and MIG`, succeeded.
  The release record independently reports the same identities and
  `outcome: succeeded`. No workflow rerun or automatic patch bump occurred.

### Production observations and manual smoke

The reviewed disposable probe ran with
`./scripts/run.sh python3 /tmp/donut-tracker-delivery-60c866c4/checkpoint-a-postrelease.py`
and reached native terminal 0. Raw protocol, literal command metadata, hashes,
UTC times and schema output are in
`/tmp/donut-tracker-delivery-60c866c4/checkpoint-a-observed-0ciqvmq6/`.
Observation window: `2026-11-13T07:21:44.784348Z`–`07:22:26.888045Z`.

Fresh before/after MIG discovery found the same complete active membership:
one member at the current target size, all healthy/running, action NONE,
group stable and version target reached. Member `doughnut-app-group-qk27`,
ID `5334386849658087999`, reports localhost health profile `prod` and exact
build `4059ac2353c590f33c971f5d531cf3853dacd4a8`. Each discovered member was
checked; the previous one-instance count was not a fixed test assumption.
Authenticated GCP guest attributes supplied its rotated ED25519 fingerprint
`SHA256:PvwmjUaN8IDYlaPukZxR9Q4YkQV2Ys7Tyda9j5lLIXI`; a separate temporary
trust file and strict SSH checking reused the existing OS Login user/key.

At `2026-11-13 07:22:08.317726 UTC`, MySQL `8.4.11-google`, database
`doughnut`, read-only flag 1: V354 succeeded and its correction table exists;
latest applied versions 356/355/354 all succeeded. `mcq.note_id` remains
nullable and `fk_mcq_note` remains CASCADE to note. The copied bounded
read-only protocol preserves its earlier provenance; no private question
data or database write was used. The source's scalar write has unchanged
accepted local normal/regeneration proof in slices 6/7.

Public LB HTML and `/assets/main-DHEN45RN.js` match bytes from the exact
approved GCS frontend prefix. HTML SHA-256:
`63bc0cb74e2543836c29d45e3e5d06fe94301603993164ae3ea9abbfd82f8956`;
asset SHA-256:
`01cee794b8a3f304241c4eb835ddab7472548c89a2d888d94a73320ff0f62982`.
LB health reports the same production build. The public CLI URL matches the
frontend-bucket object generation `1794314872038666`, 2,037,020 bytes and
`text/javascript` media type.

The automated observation did not exercise interactive login: CUA reported
the browser access host unavailable before any UI action, and that CUA part
was closed with its failure summary. Its raw result remains unchanged.
The developer was asked to sign in and confirm reaching notebooks, then
replied `LGTM` to that explicit outstanding check. At the
`2026-11-13 07:33 UTC` resume this is accepted as the developer's manual
login smoke confirmation, not an automated authentication result. Together
with the accepted publication/all-active/schema/public observations above,
it settles G2 and checkpoint A. Slice 9 may now proceed; its own checkpoint B
still requires the plan's separate exact-identity go-ahead.
