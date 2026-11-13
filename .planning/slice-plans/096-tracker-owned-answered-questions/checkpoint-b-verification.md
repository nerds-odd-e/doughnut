# Checkpoint B publication and observation

Observed 2026-11-13 after the developer's `go ahead` for the reviewed retained
landing and exact tag/SHA. The subsequent `LGTM`, answering the fresh sign-in
and notebook-list smoke request, settles that final manual observation.

## Accepted identities and publication

Dough Land retained the worktree, branch, Taken assignment and plan while
publishing `34cf85c27b97876419039d6dbb5dc1cb922997d8` onto origin/main.
Its one push was accepted without candidate rewriting; the clean default
checkout advanced to the same SHA. Checkpoint A already consumed the launch's
landing context, so this later publication supplied none.

Exact main CI [43413409754/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43413409754)
succeeded, with both required artifacts available and unexpired. Its observer
completion confirmed shutdown and zero unread notifications.

Immutable annotated tag `v1.3.75` has raw ref
`130fad39781394fd9823229986889ed9577debec`, peeling to the accepted SHA above.
[Application Release 43415091457/1](https://github.com/nerds-odd-e/doughnut/actions/runs/43415091457)
admission job `131850857533` selected that tag/ref/release SHA, the identical
build SHA and CI `43413409754/1`. Actual GCP publication job `131851972234`
and its publication step succeeded, 11:14:12–11:19:56 UTC; watch `74397`
exited 0. The independent GCS application-release record agrees and records
`outcome: succeeded`. Post-checkout submodule cleanup warnings did not fail
publication or its job; the actual observations below independently cover B.

## Every active backend and applied migration

The explicit-identity read-only helper ran as native `4187`, exit 0:

```sh
./scripts/run.sh python3 /tmp/donut-tracker-delivery-60c866c4/checkpoint-b-postrelease.py --expected-tag v1.3.75 --expected-release-sha 34cf85c27b97876419039d6dbb5dc1cb922997d8 --expected-build-sha 34cf85c27b97876419039d6dbb5dc1cb922997d8 --expected-flyway-version 300000357 --expected-ci-run-id 43413409754 --expected-ci-run-attempt 1
```

Observation window: 11:20:49.314769–11:21:48.101947 UTC; all 15 commands
exited 0. Dynamic before/after enumeration showed a stable, reached MIG
target with complete unchanged membership, targetSize 1. Its sole active
member `doughnut-app-group-qk27`, ID `5334386849658087999`, was
RUNNING/HEALTHY/NONE and reported the exact build SHA through localhost health.
This covers every actual active backend rather than assuming the historic
two-instance configuration.

Authenticated guest ED25519 host keys, the existing OS Login identity/key and
strict host verification were used. SQL used a read-only consistent snapshot
in UTC and rollback; no learner content, data writes or access changes occurred.
At 11:21:21.413257 UTC, MySQL `8.4.11-google`, database `doughnut`, showed
Flyway `300000357` success 1 at rank 964. Nullable `mcq.note_id` remains;
question-note FK count is 0 and `fk_mcq_note(note_id)` remains indexed.
Prerequisite 354/table and latest 355/356 were also successful. Health alone
was not treated as evidence of Flyway completion.

## Public and manual smokes

Public HTML and actual `/assets/main-DHEN45RN.js` bytes matched the tagged
GCS frontend prefix. LB health reported the exact prod build. CLI
identity-encoding HEAD matched its independent frontend-bucket object:
generation `1794314872038666`, length `2037020`, type `text/javascript`.

Browser host access was unavailable and native Chrome inspection timed out;
no automated UI login observation was claimed. After the deployed/schema/public
checks, the developer was asked once to sign in and confirm notebook-list
access. Their subsequent `LGTM` is the manual B confirmation, distinct from A.
G4's reviewed landing/tag, publication, all-active, applied FK migration and
smokes are therefore proved. Slice 12 pairs this observation with its post-B
fixture retirement and full-backend proof before its done transition;
[the maintained proof](tracker-retention-verification.md#post-b-fixture-retirement-and-maintained-proof)
retains both journeys. C remains a later gate.

Raw timestamps, output hashes, schema TSV and complete command arguments:
`/tmp/donut-tracker-delivery-60c866c4/checkpoint-b-observed-71uhj8jx/observation.json`.
Admission/publication logs, accepted landing and CI receipts, tag identity and
human handoff are retained in that directory's parent and
`checkpoint-b-review.json`. No accepted publication needs repeating.
