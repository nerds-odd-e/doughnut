# Verify testing guidance links

**Identity:** SEED-127#verify-test-guidance-links
**Source:** [bounded correction](../../seeds/SEED-127-verify-test-guidance-links.md#verify-test-guidance-links)
**Source review:** [source-only commitments](source-review.md)

## Execution context

Established claim: `669aa5f59cdec27de494ef5c23f04a6bd276ff7f`, publisher
`dashboard-territory.local-doughnut`, agent Mana-chan. Story Branch Mode uses
`/Users/terryyin/git/doughnut/.worktrees/verify-testing-guidance-links`, branch
`codex/verify-testing-guidance-links`, remote `origin`, integration target `main`.
Starting revision: `37bb542ff51c2f4f179b508448c4dcc11d79d536`.
Workspace reused from the established start; originating and execution checkout
are the same. Default checkout is `/Users/terryyin/git/doughnut`.
Checkout setup and `./scripts/run.sh pnpm exec biome --version` passed against
the current lockfile (Biome 2.6.6). Existing planning authority is retained for
overrun replanning; no new scope is authorized.
CI uses GitHub Actions `ci.yml` (verified active, `donut CI`) on the execution
branch. Claim CI on trunk is unobserved. Codex stream coordinator: `mana-seed127`.
Observer `/tmp/dough-ci-501/watch-AQFVKN`, PID `38325`, session `36029`,
yielded cell `g134:10`; runtime is this checkout's installed execution skill.
Accepted implementation `3cd9d8d52d3d646e7de66e1890a293fbd12f2f3a` on
`origin/codex/verify-testing-guidance-links`, suffix base the established claim.

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
Status: done
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

The current bounded route contains 22 local Markdown links across 10 sources.
Code spans/fences and external URLs are excluded from inputs; destination files
remain outside traversal unless explicitly listed as sources.

## Slice 1 accepted proof

Implementation/proof took about three active minutes. Source freshness returned
`unchanged` at trunk `669aa5f59cdec27de494ef5c23f04a6bd276ff7f`; predecessor
SEED-126 is already in the baseline, and no named sibling introduces a change.
All source-review commitments are compared individually in `source-review.md`.

- `./scripts/run.sh pnpm lint:test-guidance-links`: pass, 22 local links across
  10 sources. Actual guidance inputs supply the precondition; checker filesystem
  resolution and `anchors()` supply the result, rather than fixture outcomes.
- In isolated scratch copied from this candidate, public command
  `pnpm lint:test-guidance-links` at
  `/tmp/nix-shell.E9MFiu/seed127-link-proof-7f1s4kc9` produced baseline 0;
  replacing unit-testing's `../dough-makeme/SKILL.md` with
  `../dough-makeme/SKILL-missing.md` produced 1 and
  `.agents/skills/unit-testing/SKILL.md -> ../dough-makeme/SKILL-missing.md: ENOENT`;
  restoration produced 0. Separately replacing `#test-data-guidance` with
  `#test-data-guidance-missing` produced 1 and
  `.agents/skills/unit-testing/SKILL.md -> #test-data-guidance-missing: heading anchor #test-data-guidance-missing not found`;
  restoration produced 0. Final SHA-256 matched the original byte-for-byte:
  `6419f683f0952f8984113e2e98bdb23bf699f767be5544ef51f7288fc0ee39b2`.
  Raw stages retained at `/tmp/seed127-link-proof-evidence.json`; scratch retired.
- `./scripts/run.sh node .agents/skills/dough-execute-plan/scripts/test-entry-rules.mjs --project /Users/terryyin/git/doughnut/.worktrees/verify-testing-guidance-links --source frontend/tests/store/noteStore.trashNote.spec.ts`:
  pass, `makeMe:true,pageObjects:false`; independent membership boundary unchanged.
- `./scripts/run.sh node scripts/ci/node-test.mjs scripts/ci/ci-workflow.test.mjs`:
  seven tests pass, including lint command/report integration. Inspected root
  package scripts and `.github/lint_and_generated_types/action.yml`: normal CI
  lint reaches the new public command, with existing report groups preserved.
- `./scripts/run.sh node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/verify-testing-guidance-links --base 669aa5f59cdec27de494ef5c23f04a6bd276ff7f --term lint:test-guidance-links --term lint:all`:
  all 89 rows dispositioned at `/tmp/seed127-consumer-trace.md`. Affected root lint
  and CI consumers have the command and workflow proof above. Generic anchor,
  unchanged metadata, fixture, and planning-reader matches do not consume the
  added validation behavior. No affected consumer or promised observation remains
  unproved. No application suite adds proof at this boundary.
- `scripts/check_diff_whitespace.sh`: pass.

Fresh `/root/refactor_links` review returned `## REFACTOR COMPLETE`: none —
already clean, no edits or test reruns. Accepted proof remains applicable;
changed-source conventions are off and all edited files are below 250 lines.

## Execution complete

Product advice: no change. Retrospective of the accepted implementation against
the original source, full net change, focused proof and consumer observations
found no correction or product follow-up. The ten-source command owns the local
guidance route coherently; package/CI lint consumes it without changing existing
report groups. Application tests and E2E journeys are unchanged and add no proof
to this documentation boundary. North Star product topics do not govern this
local validation; no architectural choice or ADR change is introduced.

Process review enabled by `skipProcessRetrospective:false` and completed from
the available conversation/tool history; no persistent process finding needed.
This bounded correction is outside automatic feature-story Intent Coverage,
so no process review is deferred. CI for the accepted implementation remains
pending on the retained story-branch observer; completion owns that result.
This completion increment is `records-only`; accepted semantic proof is unchanged.
