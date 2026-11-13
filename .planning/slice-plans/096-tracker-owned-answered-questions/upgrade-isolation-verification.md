# Isolated upgrade fixture verification

Local Structure 13 proof, 2026-11-13. Hosted capability remains G7, owned by
13; destructive cleanup remains G6/14 and actual C remains G5/15.

## Implemented responsibility

The temporary fixture owns a UUID catalog and only its named-account grant.
It derives source URL/account from real datasource metadata/CURRENT_USER,
uses existing local root administration or the backend CI credential, and
runs canonical Flyway through 357 on the independent target. Existing shared
MakeMe persists its graph through the retained committed transaction connection.
Target-qualified joins verify actual question/prompt/tracker/note/user rows
BEFORE commit; a wrong route throws and rolls back. Finally clears/restores
the original catalog. Independent target JDBC reads/replay preserve payload.
Revoke/drop cleanup owns only its generated catalog/grant, including failures.

There is no graph copier, extra server, mock engine or new Spring context.
The selected Test.java uses MakeMe=true; helper=false. The local proof leaves
original MCQ rows and normal Flyway history intact. An OWNED promptless source
sentinel guarantees nonempty original data with positive stem/key and whole-row
preservation across target replay/teardown; finally deletes only that owned ID.

The existing MySQL CI wrapper gained an optional backend-only known-root path.
The pinned action chooses root OR named-user initialization, so an explicit
conditional step creates/grants the existing named account on its original
DB after root startup. Other callers retain their default path. The existing
password secret is passed only to the backend test step; values enter Docker
through environment/SQL stdin and are not printed or placed in command args.
No production migration/profile/startup change is part of this Structure.

## Accepted local proof

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-upgrade-isolation-sentinel-backend.log 2>&1
```

Native `63257` exited 0: 643 classes, 3,199 tests, zero failures/errors,
two skips. `canonicalSchemaReplayRetainsAskedEvidenceInItsOwnedCatalog` passed
in 0.985s, including provision/grant, actual precommit joins, target stored
payload/replay, positive original sentinel preservation and revoke/drop.
Test starts: 12:44:40.227–12:47:54.662 UTC. XML mtimes:
12:47:56.336279–12:47:56.468033 UTC. The coordinator independently counted
all XML and inspected the actual routing, teardown and assertion locations.
Archive: `/tmp/donut-upgrade-isolation-sentinel-proof`.

Prior native `39918` also passed the full 643/3,199 suite. Its original source
question list was uncontrolled, so root required the positive source sentinel
and justified one full rerun after that setup correction. The prior archive
`/tmp/donut-upgrade-isolation-proof` remains unchanged; it is not substituted
for final sentinel proof. Helper bytes remained identical; only the Test changed.
Active authoring/read/closure was estimated 7–9 minutes; full-suite waits are
separate. Each yielded command retained its sole owner to terminal, with fresh
binary progress during longer waits; no duplicate run or unowned intervention.

CI-source checks: `bash -n .github/mysql_db/create-test-user.sh`, actual YAML
parsing, whitespace and existing `scripts/ci/ci-workflow.test.mjs` all passed;
workflow native `64687` exited 0 with seven cases, no failures/skips. These
observe actual YAML policy/proof wiring, not hosted Docker/root operation.
Literal commands/setup/results: `/tmp/donut-upgrade-isolation-ci-handoff.md`.

## Consumers, freshness and current limits

All 20 combined readers and 23 CI-expanded readers have purpose dispositions:
`/tmp/donut-upgrade-isolation-consumer-dispositions.md` and
`/tmp/donut-upgrade-isolation-ci-consumer-dispositions.md`. The direct new Java
reader ran in the full suite; current source-sentinel setup and its expected
payload were inspected separately. Other workflow/release readers retain the
unchanged selector/path-ignore/proof contracts. The changed backend root path
and E2E default path still require actual CI; they are not excluded as proved.

The backlog adapter accepted merge `01afe6df760fb0a79d7d32ee535ccba0820aa10c`,
parents `76b27f...` and published main `5d2ae88...`, before the full local proof.
Incoming SEED-129 package-action/script/record changes have no overlap with
this Java fixture, mysql_db wrapper or CI workflow. Its already published
history and catalog maintenance were retained; unrelated package code was not
reimplemented. Source freshness independently returned unchanged at pinned
main `5d2ae88a444206ccd518f2eb46da751ca09417cd` before acceptance.

API/ERD generation is inapplicable: no application API/schema source changes.
Earlier six-file cleanup attempt and its two failure archives remain parked
and provenance-preserved; this fixture does not claim destructive selection
or C acceptance. The [actual read-only C baseline](checkpoint-c-preparation.md)
records the separate production count/FK/process capability observation.

Publish this reviewed local result as progress, keeping 13 planned. Actual
applicable branch CI must show backend named-user/bootstrap, root access,
owned target routing and revoke/drop before G7 and ordinary 13 completion.
Only then may cleanup 14 restore its compatible parked algorithm and full
asked graph; contract 15 must project retained MCQ payload fields rather than
compare a removed physical column. Source-sentinel payload remains an entire
ORIGINAL row because target DDL must never alter it.

Fresh independent refactoring returned `REFACTOR COMPLETE`, none already clean.
Current Java bytes match accepted 63257 sources; selected conventions, all
handwritten size limits and documented whitespace checks passed. No refactor
edits/tests were needed. Final source freshness remained unchanged at main
`5d2ae88...`. Report: `/tmp/donut-upgrade-isolation-refactor.md`.
