# Guarded question schema retirement: local proof

Behavior 15, 2026-11-13, against published `c524fd16eb40da8b9e4cbc08344b5a287d7a9733`.
Local proof is accepted; actual checkpoint C remains G5. This is a prepared
progress increment, not completion of 15 or authorization to release.

Fresh independent refactoring returned none already clean with no edits/tests;
all accepted code hashes, proof and consumers remain unchanged. Whitespace
native `6342` passed. Final freshness `39077` returned unchanged at the pinned
main/base below. Reports: `/tmp/donut-contract-retirement-refactor.md` and
`/tmp/donut-question-contract-handoff.md`.

Fresh attempt `63372` completed selective formatting then its hook failed at
cached Spotless with zero reported lint errors. Forced check-only
`spotlessJavaCheck --rerun-tasks --info`, native `85215`, passed. Source hashes
remain unchanged; retry retains that one formatting pass and the normal hook.
No commit/publication occurred in the failed attempt.

## Implemented boundary and observed behavior

Collision-checked Java 360 independently counts questions referenced by no
recall_prompt. A positive count refuses before ALTER. Otherwise one ALTER
drops the existing secondary index and physical note column. It does not
depend on the temporary gate or guard 358, which 16 will retire after C.

The existing canonical graph case changes only its enabled target 359 to 360.
Real repair/retry now executes 358 → 359 → 360, preserving the same independently
captured ten-table asked graph and payload while removing exactly two owned
promptless candidates. The default-off case and original-catalog full-row
sentinel remain intact. No shared base or second canonical graph was added.

The separate refusal case migrates an EMPTY owned target through default-off
359, then commits one MakeMe question with explicit target-qualified verification.
Actual 360 refuses with its readiness cause; the same question, nullable column
and existing index are positively observed before ALTER. It does not pretend
that enabling a gate replays successful 359. The pending harness is temporary;
successful retirement is a one-time catalog reading, not an absence assertion.

The canonical production startup now supplies
`-Dspring.flyway.placeholders.questioncleanup=1=1` before `-jar`.
Profile defaults remain `1=0`. The existing startup-command test positively
observes this deliberate flag; source/hash/pinned-publication consumers were
checked locally. Actual running flags/template/publication remain G5.

## Exact local verification

```sh
CURSOR_DEV=true nix develop -c pnpm backend:verify > /tmp/donut-question-contract-verify.log 2>&1
CURSOR_DEV=true nix develop -c env DONUT_ERD_SCHEMA=doughnut_wt_6c52c877a9c84ab186ec2c44fc117692_test pnpm export:database-erd > /tmp/donut-question-contract-erd.log 2>&1
```

Backend native `89695` exited 0, 13:48:10–14:04:36 UTC: 645 XML classes,
3,202 tests, zero failures/errors, two skips. Actual default no-op passed
11.951s; complete populated chain/repair/preservation 12.239s; pre-ALTER refusal
8.692s; original-catalog isolation 10.436s. All XML is fresh at
14:04:29.955–14:04:29.981 UTC. The coordinator independently counted every XML,
read the observing setup/assertions and matched all five current source hashes.
Retained move/history/MCP/export/deletion and startup behavior ran in that suite.

The single run took 16m26 under changing host load. Actual binary growth,
41% reclaimable memory and 54.73% idle CPU supported root's bounded extension
at 14:00:20 after the original 8–12 minute estimate. It reached terminal before
a second extension was needed. No duplicate, retry or unowned intervention
occurred. Source editing was about 2–3 minutes; proof waiting is separate.
Internal Spotless only compacted one new test expression; that final source
was compiled and is retained with the passing result.

ERD native `36837` exited 0; generated output is byte-identical. Read-only
catalog native `26902` exited 0, UTC `14:06:01.210528`, explicitly assigned
`doughnut_wt_6c52c877a9c84ab186ec2c44fc117692_test`, transaction_read_only 1:
358/359/360 each success 1, note column 0, index 0, promptless selection 0.
The transaction was rolled back. This is the separate one-time physical reading.

## Consumers and remaining gates

The installed combined trace has 29 purpose dispositions: 13 run, two reused
unchanged defaults, 14 excluded. The startup-specific 15 readers have ten run,
two reused, three excluded. No local consumer remains pending. Literal commands,
actual startup/hash/pinning proof and disposition files are indexed in the
implementation handoff. Dated source/observations and immutable baseline/FK
migrations retain their provenance; runtime question attribution uses trackers.

Archive `/tmp/donut-question-contract-proof-20261113T134740Z` retains prelaunch
decision/sources, passing log/XML/manifest, consumer records and catalog/ERD
readings. Source freshness `82234` exited 0, unchanged at main/base
`5d2ae88a444206ccd518f2eb46da751ca09417cd`. No named active sibling exists.
The source's 131 original commitments remain intact. G5 still owns actual C;
G8/C93 explicitly carries transitive tooling retirement and observed neutral
startup to 16. The coordinator continues the existing selected story; no
resplit was adopted after the optional recommendation.

Before human review: publish after fresh refactor/format/hook, obtain applicable
branch CI, refresh production count/full FK/all-active B reading, and pin the
exact retained landing/tag/SHA and deliberate gate. After approval, observe
actual publication, every active binary/JVM, applied versions, public and fresh
login smokes. Keep the harness/config/resources through C. Only then may 16
retire spent tooling and prepare its separately reviewed neutral publication.
