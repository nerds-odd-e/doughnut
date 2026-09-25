# Teach the shared Spring test context in backend test guidance

**Identity:** quick/039-teach-shared-spring-test-context/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"400f64523b23560ca9a6deb23a3f4c9578298d2ff9dee102c14d7dcfd5550fec"}}
```

## Source

- Kind: bounded retrospective correction; no seed.
- Corrects the execution of the owner's 2026-09-25 request
  `/dough-test-optimization backend unit test` (no story or backlog identity),
  plan `.planning/quick/038-share-backend-test-context/PLAN.md` at `db643ef844`
  (deleted at wrap-up). Reviewed commits on `story/037-share-backend-test-context`
  (base `5c8bb75741`): `0f8709dfc1` (plan), `c7ea84e3a7`, `7cb7c300b1`,
  `6455811f4d`, `5bf185ba0d`, `db643ef844`.
- Findings re-verified at `db643ef844` (2026-09-26):
  - `.agents/skills/backend-testing/SKILL.md` line 51 (updated in `6455811f4d`)
    says every `test`-profile Spring test extends `testability/SpringTestBase`
    and that a different annotation mix caches another context. The same
    file's "Database Tests" example (lines 75-81) still shows
    `@SpringBootTest @ActiveProfiles("test") @Transactional class RestNoteControllerTests`:
    the restated-base pattern the execution removed from 77 classes, on a class
    that does not exist. Copying it boots a new context and a new Hikari pool
    (the skill's own `Too many connections` warning).
  - `docs/cloud_agent_backend_testing.md` line 89 tells readers to "Use
    `@SpringBootTest` and `@Transactional`" for database tests, naming the same
    missing `RestNoteControllerTests`.
  - `.planning/test-optimization-candidates.md` (added in `db643ef844`) says
    "plan 037 cut the suite from 21 context boots to 5". Plan number 037 was
    also allocated on main to `quick/037-fold-picture-attach-step-into-upload`,
    and both plans are removed at wrap-up, so the reference is ambiguous.

## Goal and scope

Beneficiary: developers and agents writing backend tests. The guidance they
read first shows the one shared context, so a new database test joins it
instead of booting another one and undoing the measured speed-up
(test phase 56.3s to 40.6s, 21 context boots to 5).

Included: replace the "Database Tests" example with a class extending
`SpringTestBase` (or `ControllerTestBase` for controllers) and no restated
annotations; make the cloud-agent doc say the same and name an existing
example (for example `TextContentValidatorTest`); reword the candidate record
to name the change by capability and date instead of "plan 037".

Excluded: any test or production code change; a mechanical guard that fails
when a new context appears (no evidence yet that guidance is insufficient);
the four contexts that differ on purpose.

Preserved promises: all backend tests unchanged; line 51's rule stays the
authority, the example only illustrates it.

## Slices

### 1. Backend test guidance shows only the shared Spring context
Type: Structure
Status: planned
Proof: `git grep -n "@SpringBootTest" -- .agents/skills docs` returns only
explanations of the base, not an example to copy for a `test`-profile test;
`git grep -n "RestNoteControllerTests\|plan 037"` returns nothing under
`.agents`, `docs` and `.planning/test-optimization-candidates.md`; each class
named as an example exists (`git ls-files | grep <Name>`). No test run is
needed because no code changes.

Change: removes the documentation weakness that contradicts the shared-context
rule (Structure owning a retrospective correction; no product behavior).

## Current decisions

- Documentation only. If the owner later sees new restated contexts appear
  despite the corrected guidance, a context-count guard can be proposed then.
