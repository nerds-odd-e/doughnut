# Populated cleanup readiness verification

Behavior 14, 2026-11-13, accepted against `34bbaff8a1b9f475a760350b244b4eec48440f88`.
This settles G6's real populated cleanup proof. Physical retirement and actual
checkpoint C remain G5/15; production has not received this intermediate code.

Fresh independent refactoring found none already clean, with no edits/tests.
Seven accepted source hashes and all proof boundaries remained unchanged;
documented whitespace native `82404` passed. Report:
`/tmp/donut-cleanup-readiness-refactor.md`. Final freshness `26704` again
returned unchanged at the main/base revision below before delivery.

First fresh delivery `93607` completed selective formatting, then its hook
failed at cached Spotless with zero reported lint errors. Forced check-only
`spotlessJavaCheck --rerun-tasks --info`, native `69359`, exited 0. Source
hashes remain identical. Retry reuses that accepted formatting; the normal
hook remains enabled and no older HEAD substitutes for the candidate.

## Promise, setup and observed result

The story's "Then clean unreferenced questions and drop the column" is split
at the safe local cleanup boundary. Java 358 refuses a positive promptless
count with the default-off gate before SQL 359 can record a successful no-op.
SQL 359 selects only questions referenced by no recall_prompt of any kind.
All four application profiles retain explicit `questioncleanup: "1=0"`.

Both cases create their own catalog through canonical 357. Committed MakeMe
construction switches the retained JDBC connection, then explicitly verifies
target-qualified rows and the joined graph before commit. A wrong route fails
and rolls back. Independent target JDBC reads observe the persisted outcome.
Only that catalog/grant is revoked/dropped; shared rows/history stay intact.

The ten-table graph includes two referenced questions and three prompts:
answered, same-tracker sharing, and separate unanswered. Its wrong choice,
exclusion reason/times, AGAIN log, conversation/message/accepted correction
are actual stored payload. Two additional questions have no prompt. Literal
expected payload and captured IDs are independent of the construction helpers.

| Setup/action | Expected result | Actual observation |
| --- | --- | --- |
| Empty target passes 358 default-off; populate, then 359 default-off | All four question IDs, both candidate payloads and complete asked graph retained | Passed, 3.613s |
| Populate before 358; target 359 default-off | Deliberate-enablement refusal before cleanup, same IDs/asked payload | Passed as first phase of retry case |
| Real repair, override only gate to `1=1`, migrate 359 | Exactly two promptless IDs removed, both referenced IDs and complete asked graph retained | Passed, retry case 2.217s |
| Retained isolation case | Original full-row sentinel, original question IDs and Flyway history unchanged across target lifecycle | Passed, 1.707s |

The MCQ snapshot projects eight surviving entity columns, preparing the same
graph for immediate 15. Other table snapshots retain all columns. The original
catalog sentinel remains a full-row read because target DDL must not touch it.

## Exact proof and retained failures

```sh
CURSOR_DEV=true nix develop -c pnpm backend:verify > /tmp/donut-asked-cleanup-verify-fixed.log 2>&1
CURSOR_DEV=true nix develop -c env DONUT_ERD_SCHEMA=doughnut_wt_6c52c877a9c84ab186ec2c44fc117692_test pnpm export:database-erd > /tmp/donut-asked-cleanup-erd.log 2>&1
```

Native `6621` exited 0, 13:31:01–13:37:19 UTC: 644 XML classes,
3,201 tests, zero failures/errors, two skips. Both new cases and the retained
isolation case passed. The coordinator independently counted the XML, inspected
the actual setup/assertions and matched all seven source hashes to this run.
XML mtimes: 13:37:07.283–13:37:07.473 UTC. Startup/profile consumers also passed.
ERD native `91196` exited 0, 13:37:39–13:37:55 UTC. The generated diagram is
byte-identical; the existing MCQ CASCADE/prompt-to-conversation SET NULL
closure remains the deletion-risk basis. No diagram was hand-edited.

The first native `44825` exited 1 after 7m20: 3,201 tests, exactly one
failure, zero errors, two skips. Default-off and isolation passed; the retry
stopped before enablement because Flyway wrapped the guard's exception in
`Migration failed !`. Installed Flyway 12.4 source confirmed the cause wrapper.
The expected guard text now uses Spring's most-specific-cause utility.

Native `16319` exited 1 after 3m30 with that same failure. Its attempted edit
had not reached the file; the missing readback made this an invalid unchanged
duplicate. It is retained as failed evidence. Before `6621`, literal source,
the two-hunk diff and SHA were verified; compileTestJava executed. Root's
separate inspection instruction arrived after launch; root then inspected
and accepted the actual correction. Only the final run proves enabled cleanup.

Archive `/tmp/donut-asked-cleanup-proof-20261113T131929Z` preserves each run's
logs/XML/source snapshots and summaries, installed Flyway source, final hashes,
the oracle diff and ERD result. Earlier shared-catalog/bootstrap failures and
their costs remain in the final briefs; this success does not rewrite them.

## Consumers and delivery boundary

The installed trace searched the whole checkout with the changed paths plus
gate, deletion predicate, fixture, snapshot and migrations terms. All 93 readers
have purpose dispositions: 48 run, 45 excluded, none pending. Both actual
package-private helper callers compiled and ran. Startup/config/history and
retained move/history/MCP/export/deletion behavior ran in the full suite;
the exporter ran explicitly. Source/document lexical hits were inspected by
their actual purpose. Full trace/dispositions and literal command are retained
in `/tmp/donut-asked-cleanup-handoff.md` and
`/tmp/donut-asked-cleanup-consumer-dispositions.md`.

Freshness native `10394` exited 0: story/plan unchanged at pinned main/base
`5d2ae88a444206ccd518f2eb46da751ca09417cd`. No active named sibling exists.
The independent 131-row source review remains applicable; 14 owes G6 while
the original final-stage commitments remain with 15/16 and lifecycle owners.
No API generation is required. Seven handwritten sources remain below 250
lines. Active source editing was about five minutes; full-engine waits were
separate. Host memory was reclaimable despite high load; one owned backend
run at a time progressed to terminal without touching unowned processes.

This intermediate result is safe for branch publication with defaults off.
It intentionally refuses pending populated production cleanup. Actual C needs
the guarded DDL, startup override, fresh count/FK reading and exact reviewed
retained landing/tag/SHA authorization; no B approval supplies that authority.
