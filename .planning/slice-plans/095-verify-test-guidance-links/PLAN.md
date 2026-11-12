# Verify testing guidance links

**Identity:** SEED-127#verify-test-guidance-links
**Source:** [bounded correction](../../seeds/SEED-127-verify-test-guidance-links.md#verify-test-guidance-links)

## Goal and boundaries

Give test authors/refactorers repeatable positive link/anchor reachability
proof for the local testing guidance route. Preserve entry selection and all
application/test behavior. This plan authorizes no implementation by itself.

The check reads Markdown links in unit-testing, frontend-testing,
backend-testing, CLI and E2E-authoring skills; E2E's running and mocking
references; shared MakeMe/page-object entrypoints; and shared test-entry rules.
It resolves local destinations relative to each source, including fragment-only
heading references. External URLs and backticked code-path mentions are not
local Markdown link inputs. Do not recursively expand into all project docs.

## Provenance and current finding

Intent Coverage checked `924d9293c767d15f7247402d387ebb7fc8654f85` on 2026-11-12.
In an isolated worktree, only unit-testing's `Dough MakeMe` link changed from
`../dough-makeme/SKILL.md` to `../dough-makeme/SKILL-missing.md`. The maintained
selector stayed successful (`makeMe:true,pageObjects:false`); an independent
link witness observed existing/missing/existing over baseline/fault/restoration.
The scratch file was restored byte-for-byte, the restored selector passed and
the scratch worktree was retired. This is a confirmed path omitted by that
selector command, not proof of a defect in the correctly delivered guidance.
The accepted one-time link walkthrough covers the delivered links separately.

Predecessor source/plan remain recoverable at that revision under
`.planning/seeds/SEED-126-use-shared-test-guidance.md#use-shared-test-guidance`
and `.planning/slice-plans/094-use-shared-test-guidance/PLAN.md`.

## Current decisions and existing solutions

- PFE: no maintained local Markdown link/anchor validator exists in project
  scripts, package commands or CI. Installed backlog parsers own canonical story
  identity; frontend Markdown rendering owns product content. Neither should
  acquire this guidance-validation responsibility.
- Use one focused Node check command over the actual bounded guidance inputs,
  integrated as a lint operation through root package scripts and `lint:all`.
  Prefer that sufficient command and scratch discrimination over a separate
  validator framework or implementation-mirroring test harness. Existing Node
  standard-library filesystem/path tools and root command conventions suffice.
- Prove the check at its public command boundary. No new CI Node test-report
  group or changes to CI fragment counts are required for this lint check.
  Membership selection stays in the installed selector; do not duplicate it.
- Current North Star topics govern product trees, formats, synchronization and
  audio; none governs this documentation validation. No domain model or
  Accepted ADR changes are required.

## Outside-in proof ownership

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Valid local authoring links and anchors reach installed/local guidance | 1 | Run the new focused command on the actual bounded guidance set; success with every link resolved |
| Broken file or heading link gives a useful failure | 1 | In isolated scratch, change a real entrypoint link, run the same command and observe nonzero plus referring source and destination; restore and observe success; separately exercise a heading fragment |
| Link proof remains distinct from membership and the one-time removal sweep | 1 | Read the check's positive resource-resolution boundary; replay the representative selector to confirm its existing independent result |
| Normal lint reaches the check | 1 | Verify root `lint:all` invokes the focused command; existing lint/CI workflow contract proof stays green |

## Decisive premises and observations

Observed in the execution checkout serving SEED-126 on 2026-11-12 at
`924d9293c767d15f7247402d387ebb7fc8654f85`; later edits are closure records.

| Premise | Consuming operation and observed result | Unverified remainder |
| --- | --- | --- |
| Membership selection omits the guidance-link transition | The retained scratch report's `./scripts/run.sh node .agents/skills/dough-execute-plan/scripts/test-entry-rules.mjs --project /tmp/dough-seed126-intent-coverage --source frontend/tests/store/noteStore.trashNote.spec.ts` exits 0 baseline/fault/restored while the independent link witness sees missing under fault | None for the gap; this does not claim other checks omit it |
| Current guidance supplies valid local inputs | SEED-126 slice-2's 20-link/anchor walkthrough and its explicit authoring/shared-entrypoint destination readings passed; Intent Coverage's independent MakeMe link witness passed baseline and restored | The new maintained check and its deliberate-fault proof are slice 1's deliverable |
| Root Node tooling and ordinary lint wiring can host this bounded check | `./scripts/run.sh node scripts/ci/node-test.mjs scripts/generate-api-summary.test.mjs` exited 0 with four tests; root `lint:all` already invokes focused validation commands, and CI's lint action invokes `pnpm lint:all` | No claim that full lint was run during planning; focused integration proof remains slice 1's deliverable |

## Ordered slices

### 1. Validate the local testing guidance route
Type: Behavior
Status: planned
Proof: all rows in the proof table, at the command boundary.

Behavior: A test author/refactorer runs the focused guidance lint command with
the current local guidance route → it resolves each local Markdown destination
and heading anchor → success confirms reachability, or a broken link produces
nonzero status and the referring source/destination needed to repair it.

Add the minimal scoped check (for example `scripts/check-test-guidance-links.mjs`)
and root `lint:test-guidance-links` command, and invoke it from `lint:all`.
Use actual guidance files as proof inputs. Observe file and heading failures in
isolated scratch, preserve the exact fault/reach evidence and restore to a
passing check. Keep command proof separate from membership selection. Run
`./scripts/run.sh pnpm lint:test-guidance-links` and
`./scripts/run.sh node scripts/ci/node-test.mjs scripts/ci/ci-workflow.test.mjs`.
Use the installed consumer trace for the new lint operation and affected callers;
application suites add no proof for this documentation-check boundary.

Sizing: about five active minutes including focused proof and scratch restore;
one source-resolution rule and one command loop. Above five minutes, scrutinize;
above ten minutes, follow the project's finer-decomposition/overrun rule. No
production subsystem, dependency installation or expensive suite is introduced.

## Delivery and concerns

Use the repository's Jidoka, fresh post-change refactor, coordinator
`./scripts/run.sh pnpm format:changed`, plan update, agent commit/check-only hook
and push gates. No API generation applies. Use `scripts/check_diff_whitespace.sh`.
Keep every edited file within the existing 250-line check; expected new checker
and package wiring fit that limit without changing the oversized CI composite.
Refinement was not needed: one cohesive reachability outcome and proof loop.
No remaining slice-specific concern was identified after the bounded PFE and
premise review. No Take or execution is authorized by this preparation.

## Learnings

None beyond the recorded proof gap and existing-solution decision.
